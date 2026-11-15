package com.pixelgram.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import org.drinkless.tdlib.TdApi

class OverlaySlot(val start: Int, val end: Int, val content: @Composable (androidx.compose.ui.unit.Dp) -> Unit)

fun customEmojiSlots(entities: List<TdApi.TextEntity>, text: String, shift: Int = 0): List<OverlaySlot> = entities.mapNotNull { e ->
    val type = e.type as? TdApi.TextEntityTypeCustomEmoji ?: return@mapNotNull null
    val start = (e.offset + shift).coerceIn(0, text.length)
    val end = (e.offset + e.length + shift).coerceIn(start, text.length)
    if (end <= start) return@mapNotNull null
    val fallback = text.substring(start, end)
    OverlaySlot(start, end) { side -> CustomEmojiView(type.customEmojiId, fallback, androidx.compose.ui.Modifier.clickable(interactionSource = null, indication = null) { com.pixelgram.android.feature.chat.EmojiPreview.openPack(type.customEmojiId) }, size = side) }
}

@Composable
fun OverlayEmojiText(
    text: AnnotatedString,
    slots: List<OverlaySlot>,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    if (slots.isEmpty()) {
        Text(text, modifier = modifier, style = style, color = color, maxLines = maxLines, softWrap = softWrap, overflow = overflow, onTextLayout = onTextLayout)
        return
    }
    val hidden = remember(text, slots) {
        AnnotatedString.Builder(text).apply { slots.forEach { addStyle(SpanStyle(color = Color.Transparent), it.start, it.end) } }.toAnnotatedString()
    }
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    Box(modifier) {
        Text(hidden, style = style, color = color, maxLines = maxLines, softWrap = softWrap, overflow = overflow, onTextLayout = {
            layout = it
            onTextLayout(it)
        })
        val current = layout ?: return@Box
        val length = current.layoutInput.text.length
        Box(Modifier.matchParentSize()) {
        slots.forEachIndexed { index, slot ->
            if (slot.start >= length) return@forEachIndexed
            val line = current.getLineForOffset(slot.start)
            if (line >= current.lineCount) return@forEachIndexed
            if (slot.start >= current.getLineEnd(line, visibleEnd = true)) return@forEachIndexed
            val first = current.getBoundingBox(slot.start)
            val last = current.getBoundingBox((slot.end - 1).coerceIn(0, length - 1))
            val side = (current.getLineBottom(line) - current.getLineTop(line)).coerceAtLeast(first.height)
            val center = if (last.top == first.top) (first.left + last.right) / 2f else first.left + first.width / 2f
            val left = center - side / 2f
            val top = current.getLineTop(line)
            key(index) {
                Box(Modifier.offset { IntOffset(left.toInt(), top.toInt()) }) {
                    slot.content(with(density) { side.toDp() })
                }
            }
        }
        }
    }
}
