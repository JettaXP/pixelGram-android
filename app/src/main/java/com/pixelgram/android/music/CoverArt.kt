package com.pixelgram.android.music

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

class CoverArt(context: Context) {
    private val dir = File(context.cacheDir, "covers").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requested = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val foundFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    val found: StateFlow<Map<String, String>> = foundFlow.asStateFlow()

    fun key(audio: TdApi.Audio?, large: Boolean = false): String? {
        audio ?: return null
        if (audio.albumCoverThumbnail != null && !(large && audio.externalAlbumCovers.isNullOrEmpty())) return null
        val title = audio.title.trim()
        if (title.isEmpty()) return null
        val artist = audio.performer.trim()
        val key = "$artist|$title".lowercase()
        if (key !in foundFlow.value && requested.add(key)) {
            scope.launch { fetch(key, artist, title)?.let { path -> foundFlow.update { it + (key to path) } } }
        }
        return key
    }

    private fun fetch(key: String, artist: String, title: String): String? = runCatching {
        val file = File(dir, Integer.toHexString(key.hashCode()) + ".jpg")
        if (file.length() > 0) return file.absolutePath
        val term = URLEncoder.encode("$artist $title".trim(), "UTF-8")
        val body = get("https://itunes.apple.com/search?term=$term&entity=song&limit=5")?.let { String(it) } ?: return null
        val art = Regex(""""artworkUrl100"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1) ?: return null
        val image = get(art.replace("100x100bb", "600x600bb")) ?: return null
        file.writeBytes(image)
        file.absolutePath
    }.onFailure { requested.remove(key) }.getOrNull()

    private fun get(url: String): ByteArray? {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 12_000
        return try {
            val code = connection.responseCode
            if (code >= 500 || code == 429) throw java.io.IOException("HTTP $code")
            if (code != 200) null else connection.inputStream.use { it.readBytes() }.takeIf { it.isNotEmpty() }
        } finally {
            connection.disconnect()
        }
    }
}
