package com.pixelgram.android.ui.components

import android.content.Context
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixelgram.android.settings.IconPack
import com.pixelgram.android.ui.theme.LocalAppearance
import java.util.concurrent.ConcurrentHashMap

@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val pack = LocalAppearance.current.iconPack
    val context = LocalContext.current
    val shown = if (pack == IconPack.Material) imageVector else remember(pack, imageVector) { IconPacks.icon(context, pack, imageVector) }
    androidx.compose.material3.Icon(shown, contentDescription, modifier, tint)
}

@Composable
fun Icon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    androidx.compose.material3.Icon(painter, contentDescription, modifier, tint)

@Composable
fun Icon(bitmap: ImageBitmap, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    androidx.compose.material3.Icon(bitmap, contentDescription, modifier, tint)

object IconPacks {
    private class Glyph(val viewport: Float, val top: Float, val paths: List<Pair<String, Boolean>>)

    private val tables = ConcurrentHashMap<IconPack, Map<String, Glyph>>()
    private val built = ConcurrentHashMap<String, ImageVector>()

    fun icon(context: Context, pack: IconPack, fallback: ImageVector): ImageVector {
        val base = fallback.name.substringAfterLast('.')
        val id = "${pack.name}:$base"
        built[id]?.let { return it }
        val glyphs = table(context, pack)
        val glyph = Variants.firstNotNullOfOrNull { glyphs["$it.$base"] }
            ?: Aliases[base]?.let { alias -> Variants.firstNotNullOfOrNull { glyphs["$it.$alias"] } }
            ?: return fallback
        val pad = if (pack == IconPack.Solar && base in MediaKeys) glyph.viewport * MediaPadding else 0f
        return build(fallback.name, glyph, fallback.autoMirror, pad).also { built[id] = it }
    }

    private fun table(context: Context, pack: IconPack): Map<String, Glyph> = tables.getOrPut(pack) {
        runCatching {
            context.assets.open("iconpacks/${pack.name.lowercase()}.tsv").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.associate { line ->
                    val parts = line.split('\t')
                    val box = parts[1].split(',')
                    parts[0] to Glyph(box[0].toFloat(), box.getOrNull(1)?.toFloat() ?: 0f, parts.drop(2).map { it.substring(2) to it.startsWith("e:") })
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun build(name: String, glyph: Glyph, mirror: Boolean, pad: Float): ImageVector {
        val size = glyph.viewport + pad * 2
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, size, size, autoMirror = mirror)
        val shift = pad > 0f || glyph.top != 0f
        if (shift) builder.addGroup(translationX = pad, translationY = pad - glyph.top)
        glyph.paths.forEach { (data, evenOdd) ->
            builder.addPath(addPathNodes(data), pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero, fill = SolidColor(Color.Black))
        }
        if (shift) builder.clearGroup()
        return builder.build()
    }

    private val Variants = listOf("Outlined", "Filled", "Rounded", "AutoMirrored.Outlined", "AutoMirrored.Filled", "AutoMirrored.Rounded")
    private val Aliases = mapOf(
        "Chat" to "ChatBubble",
        "ChatBubble" to "Chat",
        "People" to "Group",
        "AccountCircle" to "Person",
        "VolumeOff" to "NotificationsOff",
        "BookmarkBorder" to "Bookmark",
        "FavoriteBorder" to "Favorite",
    )
    private val MediaKeys = setOf("PlayArrow", "Pause", "SkipNext", "SkipPrevious", "Stop", "FastForward", "FastRewind")
    private const val MediaPadding = 0.14f
}
