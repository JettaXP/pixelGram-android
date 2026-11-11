package com.pixelgram.android.feature.chat

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import org.drinkless.tdlib.TdApi

enum class Format { Bold, Italic, Underline, Strike, Mono, Spoiler, Quote, Link }

@Immutable
data class RichDraft(val value: TextFieldValue = TextFieldValue(), val entities: List<TdApi.TextEntity> = emptyList()) {
    val text: String get() = value.text

    fun edit(next: TextFieldValue): RichDraft {
        val old = value.text
        val new = next.text
        if (old == new) return copy(value = next)
        var prefix = 0
        val limit = minOf(old.length, new.length)
        while (prefix < limit && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (suffix < limit - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        val removedEnd = old.length - suffix
        val delta = new.length - old.length
        val moved = entities.mapNotNull { e ->
            val start = e.offset
            val end = e.offset + e.length
            val newStart = when {
                start < prefix -> start
                start >= removedEnd -> start + delta
                else -> prefix
            }
            val newEnd = when {
                end <= prefix -> end
                end >= removedEnd -> end + delta
                else -> prefix
            }
            if (newEnd <= newStart) null else TdApi.TextEntity(newStart, newEnd - newStart, e.type)
        }
        return RichDraft(next, moved)
    }

    fun has(format: Format, range: TextRange = value.selection): Boolean {
        if (range.collapsed) return false
        return entities.any { it.matches(format) && it.offset <= range.min && it.offset + it.length >= range.max }
    }

    fun toggle(format: Format, url: String? = null): RichDraft {
        val range = value.selection
        if (range.collapsed) return this
        val start = range.min
        val end = range.max
        val active = has(format)
        val rest = mutableListOf<TdApi.TextEntity>()
        for (e in entities) {
            if (!e.matches(format)) {
                rest += e
                continue
            }
            val s = e.offset
            val t = e.offset + e.length
            if (t <= start || s >= end) {
                rest += e
                continue
            }
            if (s < start) rest += TdApi.TextEntity(s, start - s, e.type)
            if (t > end) rest += TdApi.TextEntity(end, t - end, e.type)
        }
        if (!active) rest += TdApi.TextEntity(start, end - start, format.type(url))
        return copy(entities = rest.sortedBy { it.offset })
    }

    fun clearFormatting(): RichDraft {
        val range = value.selection
        if (range.collapsed) return this
        val draft = Format.entries.fold(this) { acc, f -> if (acc.has(f)) acc.toggle(f) else acc }
        return draft.copy(entities = draft.entities.filter { e -> e.type is TdApi.TextEntityTypeCustomEmoji || e.offset + e.length <= range.min || e.offset >= range.max })
    }

    fun insert(piece: String, entity: TdApi.TextEntityType? = null): RichDraft {
        val sel = value.selection
        val newText = value.text.replaceRange(sel.min, sel.max, piece)
        val next = edit(TextFieldValue(newText, TextRange(sel.min + piece.length)))
        return if (entity == null) next else next.copy(entities = (next.entities + TdApi.TextEntity(sel.min, piece.length, entity)).sortedBy { it.offset })
    }

    fun formatted(): TdApi.FormattedText {
        val raw = value.text
        val lead = raw.length - raw.trimStart().length
        val body = raw.trim()
        val list = entities.mapNotNull { e ->
            val s = (e.offset - lead).coerceIn(0, body.length)
            val t = (e.offset + e.length - lead).coerceIn(0, body.length)
            if (t <= s) null else TdApi.TextEntity(s, t - s, e.type)
        }
        return TdApi.FormattedText(body, list.toTypedArray())
    }

    companion object {
        fun of(text: String, entities: List<TdApi.TextEntity>): RichDraft = RichDraft(TextFieldValue(text, TextRange(text.length)), entities)
    }
}

private fun TdApi.TextEntity.matches(format: Format): Boolean = when (format) {
    Format.Bold -> type is TdApi.TextEntityTypeBold
    Format.Italic -> type is TdApi.TextEntityTypeItalic
    Format.Underline -> type is TdApi.TextEntityTypeUnderline
    Format.Strike -> type is TdApi.TextEntityTypeStrikethrough
    Format.Mono -> type is TdApi.TextEntityTypeCode
    Format.Spoiler -> type is TdApi.TextEntityTypeSpoiler
    Format.Quote -> type is TdApi.TextEntityTypeBlockQuote
    Format.Link -> type is TdApi.TextEntityTypeTextUrl
}

private fun Format.type(url: String?): TdApi.TextEntityType = when (this) {
    Format.Bold -> TdApi.TextEntityTypeBold()
    Format.Italic -> TdApi.TextEntityTypeItalic()
    Format.Underline -> TdApi.TextEntityTypeUnderline()
    Format.Strike -> TdApi.TextEntityTypeStrikethrough()
    Format.Mono -> TdApi.TextEntityTypeCode()
    Format.Spoiler -> TdApi.TextEntityTypeSpoiler()
    Format.Quote -> TdApi.TextEntityTypeBlockQuote()
    Format.Link -> TdApi.TextEntityTypeTextUrl(url.orEmpty())
}

class DraftStyles(private val entities: List<TdApi.TextEntity>, private val accent: Color, private val content: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText = TransformedText(
        buildAnnotatedString {
            append(text)
            for (e in entities) {
                val start = e.offset.coerceIn(0, text.length)
                val end = (e.offset + e.length).coerceIn(start, text.length)
                if (start == end) continue
                val style = when (e.type) {
                    is TdApi.TextEntityTypeBold -> SpanStyle(fontWeight = FontWeight.Bold)
                    is TdApi.TextEntityTypeItalic -> SpanStyle(fontStyle = FontStyle.Italic)
                    is TdApi.TextEntityTypeUnderline -> SpanStyle(textDecoration = TextDecoration.Underline)
                    is TdApi.TextEntityTypeStrikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    is TdApi.TextEntityTypeCode -> SpanStyle(fontFamily = FontFamily.Monospace, background = content.copy(alpha = 0.1f))
                    is TdApi.TextEntityTypeSpoiler -> SpanStyle(background = content.copy(alpha = 0.22f))
                    is TdApi.TextEntityTypeBlockQuote -> SpanStyle(background = accent.copy(alpha = 0.14f), color = accent)
                    is TdApi.TextEntityTypeTextUrl -> SpanStyle(color = accent, textDecoration = TextDecoration.Underline)
                    else -> null
                }
                if (style != null) addStyle(style, start, end)
            }
        },
        OffsetMapping.Identity,
    )

    override fun equals(other: Any?): Boolean = other is DraftStyles && other.entities == entities && other.accent == accent && other.content == content
    override fun hashCode(): Int = entities.hashCode() * 31 + accent.hashCode()
}

class SelectionToolbar(private val onShow: (SelectionMenu?) -> Unit) : TextToolbar {
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        status = TextToolbarStatus.Shown
        onShow(SelectionMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested))
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        onShow(null)
    }
}

class SelectionMenu(
    val rect: Rect,
    val copy: (() -> Unit)?,
    val paste: (() -> Unit)?,
    val cut: (() -> Unit)?,
    val selectAll: (() -> Unit)?,
)
