package com.pixelgram.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.td.TdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

object EmojiStickers {
    private val stickers = ConcurrentHashMap<Long, TdApi.Sticker>()
    private val json = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > JsonCacheSize
    }

    fun cached(id: Long): TdApi.Sticker? = stickers[id]

    suspend fun load(td: TdClient, id: Long): TdApi.Sticker? {
        stickers[id]?.let { return it }
        val found = runCatching { td.send(TdApi.GetCustomEmojiStickers(longArrayOf(id))).stickers.firstOrNull() }.getOrNull() ?: return null
        stickers[id] = found
        return found
    }

    suspend fun tgs(path: String): String? {
        synchronized(json) { json[path] }?.let { return it }
        val text = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                val bytes = file.readBytes()
                val gzip = bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
                if (gzip) GZIPInputStream(bytes.inputStream()).bufferedReader().use { it.readText() } else String(bytes)
            }.getOrNull()
        } ?: return null
        synchronized(json) { json[path] = text }
        return text
    }
}

const val CustomEmojiTag = "androidx.compose.foundation.text.inlineContent"

fun customEmojiKey(id: Long): String = "ce:$id"

fun customEmojiContent(entities: List<TdApi.TextEntity>, text: String, size: TextUnit): Map<String, InlineTextContent> =
    entities.mapNotNull { entity ->
        val type = entity.type as? TdApi.TextEntityTypeCustomEmoji ?: return@mapNotNull null
        val start = entity.offset.coerceIn(0, text.length)
        val end = (entity.offset + entity.length).coerceIn(start, text.length)
        val fallback = text.substring(start, end)
        customEmojiKey(type.customEmojiId) to InlineTextContent(Placeholder(size, size, PlaceholderVerticalAlign.TextCenter)) {
            CustomEmojiView(type.customEmojiId, fallback, Modifier.fillMaxSize())
        }
    }.toMap()

@Composable
fun CustomEmojiView(id: Long, fallback: String, modifier: Modifier = Modifier, size: Dp? = null, tint: Color = LocalContentColor.current, animate: Boolean = true) {
    val graph = LocalGraph.current
    val sticker by produceState(EmojiStickers.cached(id), id) { value = EmojiStickers.load(graph.td, id) }
    val sized = if (size != null) modifier.size(size) else modifier
    val current = sticker
    val snapshot by graph.store.snapshot.collectAsState()
    val fileId = when (current?.format) {
        null -> 0
        is TdApi.StickerFormatWebm -> current.thumbnail?.file?.id ?: 0
        else -> current.sticker.id
    }
    LaunchedEffect(fileId) { if (fileId != 0) graph.store.requestFileAsync(fileId) }
    val path = snapshot.files[fileId] ?: current?.let { s ->
        val file = if (s.format is TdApi.StickerFormatWebm) s.thumbnail?.file else s.sticker
        file?.local?.path?.takeIf { file.local.isDownloadingCompleted && it.isNotEmpty() }
    }
    val repaint = (current?.fullType as? TdApi.StickerFullTypeCustomEmoji)?.needsRepainting == true
    val painted = if (repaint) {
        sized.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
            drawContent()
            drawRect(tint, blendMode = BlendMode.SrcIn)
        }
    } else {
        sized
    }
    Box(sized, contentAlignment = Alignment.Center) {
        when {
            current == null || path == null -> Fallback(fallback, size)
            current.format is TdApi.StickerFormatTgs -> {
                val json by produceState<String?>(null, path) { value = EmojiStickers.tgs(path) }
                val text = json
                if (text == null) {
                    Fallback(fallback, size)
                } else {
                    val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(text), cacheKey = "tgs:$path")
                    if (composition == null) Fallback(fallback, size)
                    else LottieAnimation(composition, modifier = painted.matchParentSize(), iterations = LottieConstants.IterateForever, isPlaying = animate)
                }
            }
            else -> AsyncImage(model = File(path), contentDescription = fallback, modifier = painted)
        }
    }
}

@Composable
private fun Fallback(text: String, size: Dp?) {
    val density = LocalDensity.current
    if (size != null) Text(text, fontSize = with(density) { (size * FallbackScale).toSp() }, maxLines = 1) else Text(text, maxLines = 1)
}

private const val FallbackScale = 0.8f
private const val JsonCacheSize = 48
