package com.pixelgram.android.pixel

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

data class CustomBadge(val emoji: String, val customEmojiId: Long, val text: String)

data class Supporters(val numbers: Map<Long, Int> = emptyMap(), val custom: Set<Long> = emptySet())

class PixelRemote(context: Context, private val scope: CoroutineScope) {
    private val dir = File(context.filesDir, "banners").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")
    private val badgesFile = File(dir, "badges.tsv")
    private val index = HashMap<Long, String>()
    private val loading = HashSet<Long>()
    private val bannerFlow = MutableStateFlow<Map<Long, File>>(emptyMap())
    private val badgeFlow = MutableStateFlow<Map<Long, CustomBadge>>(emptyMap())
    val banners: StateFlow<Map<Long, File>> = bannerFlow.asStateFlow()
    val badges: StateFlow<Map<Long, CustomBadge>> = badgeFlow.asStateFlow()
    private val supporterFlow = MutableStateFlow(Supporters())
    val supporters: StateFlow<Supporters> = supporterFlow.asStateFlow()
    private val supportersFile = File(dir, "supporters.json")
    private var lastRefresh = 0L

    init {
        scope.launch(Dispatchers.IO) {
            runCatching { if (indexFile.isFile) parseIndex(indexFile.readText()) }
            runCatching { if (badgesFile.isFile) badgeFlow.value = parseBadges(badgesFile.readText()) }
            runCatching { if (supportersFile.isFile) supporterFlow.value = parseSupporters(supportersFile.readText()) }
            refresh()
        }
    }

    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefresh < RefreshMs) return
        lastRefresh = now
        scope.launch(Dispatchers.IO) {
            runCatching { fetch("$Base/index.json") }.getOrNull()?.let { text ->
                indexFile.writeText(text)
                parseIndex(text)
            }
            for (url in SupporterUrls) {
                val text = runCatching { fetch(url) }.getOrNull()?.takeIf { it.contains("\"supporters\"") } ?: continue
                supportersFile.writeText(text)
                supporterFlow.value = parseSupporters(text)
                break
            }
            runCatching { fetch("$Base/badges.tsv") }.getOrNull()?.let { text ->
                badgesFile.writeText(text)
                badgeFlow.value = parseBadges(text)
            }
        }
    }

    fun want(userId: Long) {
        val hash = synchronized(index) { index[userId] } ?: return
        val file = File(dir, "$userId-$hash.jpg")
        if (bannerFlow.value[userId] == file) return
        if (file.isFile) {
            bannerFlow.update { it + (userId to file) }
            return
        }
        synchronized(loading) { if (!loading.add(userId)) return }
        scope.launch(Dispatchers.IO) {
            runCatching {
                download("$Base/$userId.jpg?h=$hash", file)
                dir.listFiles { f -> f.name.startsWith("$userId-") && f != file }?.forEach { it.delete() }
                bannerFlow.update { it + (userId to file) }
            }
            synchronized(loading) { loading.remove(userId) }
        }
    }

    private fun parseIndex(text: String) {
        val entries = Regex(""""(-?\d+)"\s*:\s*"([0-9a-f]+)"""").findAll(text)
            .associate { it.groupValues[1].toLong() to it.groupValues[2] }
        synchronized(index) {
            index.clear()
            index.putAll(entries)
        }
        bannerFlow.update { current -> current.filterKeys { it in entries } }
        bannerFlow.value.keys.forEach(::want)
    }

    private fun parseBadges(text: String): Map<Long, CustomBadge> = text.lines().mapNotNull { row ->
        val parts = row.split('\t')
        val userId = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val customId = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
        val emoji = parts.getOrNull(2)?.takeIf { it.isNotBlank() && it.length <= 32 } ?: return@mapNotNull null
        userId to CustomBadge(emoji, customId, clean(parts.getOrNull(3).orEmpty()))
    }.toMap()

    private fun parseSupporters(text: String): Supporters {
        val block = Regex("\"supporters\"\\s*:\\s*\\{([^}]*)}").find(text)?.groupValues?.get(1).orEmpty()
        val numbers = Regex("\"(\\d+)\"\\s*:\\s*(\\d+)").findAll(block).associate { it.groupValues[1].toLong() to it.groupValues[2].toInt() }
        val list = Regex("\"custom\"\\s*:\\s*\\[([^]]*)]").find(text)?.groupValues?.get(1).orEmpty()
        val custom = Regex("(\\d+)").findAll(list).map { it.groupValues[1].toLong() }.toSet()
        return Supporters(numbers, custom)
    }

    fun customAllowed(userId: Long): Boolean = userId in BadgeOwners || userId in supporterFlow.value.custom

    private fun clean(text: String) = text.replace(Regex("[\\p{Cc}\\p{Cf}&&[^\\u200D\\uFE0F]]"), " ").trim().take(64)

    private fun fetch(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TimeoutMs
        connection.readTimeout = TimeoutMs
        if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode}")
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun download(url: String, target: File) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TimeoutMs
        connection.readTimeout = TimeoutMs
        if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode}")
        val part = File(target.path + ".part")
        connection.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
        if (!part.renameTo(target)) error("rename failed")
    }

    companion object {
        private const val Base = "https://raw.githubusercontent.com/JettaXP/pixelGram/main/Banners"
        private const val RefreshMs = 60 * 60 * 1000L
        private const val TimeoutMs = 15_000
        val BadgeOwners = setOf(1780288121L, 1170147841L, 1061520526L)
        private val SupporterUrls = listOf(
            "https://pixelgram.jettax.xyz/api/badges.json",
            "https://raw.githubusercontent.com/JettaXP/pixelGram/main/docs/api/badges.json",
        )
    }
}
