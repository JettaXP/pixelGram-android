package com.pixelgram.android.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.android.music.LyricLine
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LyricsState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
        Box(
            Modifier.size(88.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(40.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center), color = MaterialTheme.colorScheme.primary)
    }
}

private sealed interface LyricItem {
    val start: Float
    class Line(val index: Int, override val start: Float) : LyricItem
    class Gap(override val start: Float, val end: Float) : LyricItem
}

private fun lyricItems(lines: List<LyricLine>): List<LyricItem> {
    val out = mutableListOf<LyricItem>()
    val first = lines.firstOrNull { it.text.isNotBlank() }
    if (first != null && first.time > GapSeconds) out += LyricItem.Gap(0f, first.time)
    lines.forEachIndexed { i, line ->
        if (line.text.isNotBlank()) {
            out += LyricItem.Line(i, line.time)
        } else {
            val next = lines.drop(i + 1).firstOrNull { it.text.isNotBlank() }?.time
            if (next != null && next - line.time > GapSeconds && out.lastOrNull() !is LyricItem.Gap) out += LyricItem.Gap(line.time, next)
        }
    }
    return out
}

@Composable
internal fun SyncedLyrics(lines: List<LyricLine>, clock: PlayerClock, onSeek: (Long) -> Unit) {
    val now = rememberSmoothPosition(clock)
    val items = remember(lines) { lyricItems(lines) }
    val current = items.indexOfLast { it.start <= now + 0.15f }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (current - 1).coerceAtLeast(0))
    var placed by remember { mutableStateOf(false) }
    var pausedUntil by remember { mutableStateOf(0L) }
    val follow = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) pausedUntil = System.currentTimeMillis() + FollowPauseMs
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    val viewport = state.layoutInfo.viewportSize.height
    LaunchedEffect(current, pausedUntil) {
        val wait = pausedUntil - System.currentTimeMillis()
        if (wait > 0) kotlinx.coroutines.delay(wait)
        if (current < 0) return@LaunchedEffect
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == current }
        val previous = info.visibleItemsInfo.firstOrNull { it.index == current - 1 }?.size ?: 0
        val desired = (viewport * LyricsTopFade).toInt() + previous + info.mainAxisItemSpacing
        when {
            !placed -> {
                state.scrollToItem(current, -desired + info.beforeContentPadding)
                placed = true
            }
            item != null -> state.animateScrollBy((item.offset - info.viewportStartOffset - desired).toFloat(), tween(LineScrollMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            else -> state.animateScrollToItem(current, -desired + info.beforeContentPadding)
        }
    }
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(horizontal = 6.dp + LyricsBleed - LinePad, vertical = 120.dp),
        verticalArrangement = Arrangement.spacedBy(LineSpacing),
        modifier = Modifier
            .fillMaxSize()
            
            .layout { measurable, constraints ->
                val extra = LyricsBleed.roundToPx()
                val wide = constraints.copy(
                    minWidth = constraints.minWidth + extra * 2,
                    maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra * 2 else constraints.maxWidth,
                )
                val placeable = measurable.measure(wide)
                layout((placeable.width - extra * 2).coerceIn(constraints.minWidth, constraints.maxWidth), placeable.height) { placeable.place(-extra, 0) }
            }
            .nestedScroll(follow)
            .edgeFade(),
    ) {
        itemsIndexed(items) { position, entry ->
            val ease = androidx.compose.animation.core.FastOutSlowInEasing
            if (entry is LyricItem.Gap) {
                GapIndicator(entry, now, Modifier.padding(horizontal = LinePad))
                return@itemsIndexed
            }
            val line = lines[(entry as LyricItem.Line).index]
            val distance = if (current < 0) position + 1 else abs(position - current)
            val upcoming = position > current
            val lineEnd = items.getOrNull(position + 1)?.start ?: (line.time + 5f)
            val done = distance == 0 && now >= lineEnd - LineSettle
            val alpha by animateFloatAsState(if (distance == 0) 1f else if (upcoming) 0.5f else 0.4f, tween(LineFadeMs, easing = ease))
            val blur by animateFloatAsState(if (distance == 0) 0f else (distance * 1.3f).coerceAtMost(5f), tween(LineFadeMs, easing = ease))
            val scale by animateFloatAsState(
                when {
                    done -> 0.97f
                    distance == 0 -> 1.02f
                    else -> 0.94f
                },
                spring(dampingRatio = 0.9f, stiffness = 120f),
            )
            val style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            val color = MaterialTheme.colorScheme.onSurface
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    }
                    .blur(blur.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        pausedUntil = 0L
                        onSeek((line.time * 1000).toLong())
                    }
                    .padding(horizontal = LinePad, vertical = LinePad * 0.75f),
            ) {
                if (distance == 0) {
                    KaraokeLine(line.text, sungChars(line, now, lineEnd), style, color, MaterialTheme.colorScheme.primary)
                } else {
                    Text(line.text, style = style, color = color)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GapIndicator(gap: LyricItem.Gap, now: Float, modifier: Modifier) {
    val end = gap.end - GapLead
    val visible = now >= gap.start && now <= end
    val shown by animateFloatAsState(if (visible) 1f else 0f, tween(GapFadeMs))
    val progress = if (end > gap.start) ((now - gap.start) / (end - gap.start)).coerceIn(0f, 1f) else 0f
    val smooth by animateFloatAsState(progress, tween(100, easing = androidx.compose.animation.core.LinearEasing))
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .fillMaxWidth()
            .height(GapHeight * shown)
            .graphicsLayer { alpha = shown; clip = true },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularWavyProgressIndicator(
            progress = { smooth },
            modifier = Modifier.size(36.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

private fun sungChars(line: LyricLine, now: Float, end: Float): Float {
    val text = line.text
    return if (line.words.isNotEmpty()) {
        var chars = 0f
        var offset = 0
        line.words.forEachIndexed { i, word ->
            val wordEnd = line.words.getOrNull(i + 1)?.time ?: end
            val length = word.text.length
            val part = ((now - word.time) / (wordEnd - word.time).coerceAtLeast(0.05f)).coerceIn(0f, 1f)
            if (now >= word.time) chars = offset + part * length
            offset += length
        }
        chars / offset.coerceAtLeast(1) * text.length
    } else {
        val span = ((end - line.time) * SpanShare).coerceAtLeast(0.4f)
        ((now - line.time) / span).coerceIn(0f, 1f) * text.length
    }
}

class PlayerClock(val position: () -> Float, val playing: () -> Boolean, val speed: () -> Float)

@Composable
private fun rememberSmoothPosition(clock: PlayerClock): Float {
    var shown by remember { mutableFloatStateOf(clock.position()) }
    LaunchedEffect(clock) {
        var last = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { time ->
                val dt = if (last < 0) 0f else (time - last) / 1_000_000_000f
                last = time
                val target = clock.position()
                shown = if (!clock.playing()) {
                    target
                } else {
                    val predicted = shown + dt * clock.speed()
                    if (kotlin.math.abs(target - predicted) > ResyncSeconds) target else predicted + (target - predicted) * ClockPull
                }
            }
        }
    }
    return shown
}

private const val ResyncSeconds = 0.35f
private const val ClockPull = 0.06f

@Composable
private fun KaraokeLine(
    text: String,
    sung: Float,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    glow: androidx.compose.ui.graphics.Color,
) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val layout = remember(text, style, width) {
            measurer.measure(text, style, constraints = androidx.compose.ui.unit.Constraints(maxWidth = width))
        }
        val letters = remember(text, style) {
            var i = 0
            buildList {
                while (i < text.length) {
                    val size = Character.charCount(text.codePointAt(i))
                    val piece = text.substring(i, i + size)
                    if (!piece.isBlank()) add(i to measurer.measure(piece, style, softWrap = false))
                    i += size
                }
            }
        }
        val height = with(androidx.compose.ui.platform.LocalDensity.current) { layout.size.height.toDp() }
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(height)) {
            val glowRadius = GlowRadius.toPx()
            letters.forEach { (index, letter) ->
                val box = layout.getBoundingBox(index)
                val line = layout.getLineForOffset(index)
                val top = layout.getLineBaseline(line) - letter.firstBaseline
                val lit = (sung - index).coerceIn(0f, 1f)
                val nudge = if (lit > 0f && lit < 1f) NudgeStrength * kotlin.math.sin(lit * kotlin.math.PI.toFloat()) * kotlin.math.exp(-3f * lit) else 0f
                val w = letter.size.width.toFloat()
                val h = letter.size.height.toFloat()
                translate(box.left, top) {
                    withTransform({
                        scale(1f + nudge * 0.3f, 1f + nudge, pivot = androidx.compose.ui.geometry.Offset(w / 2f, h))
                    }) {
                        when {
                            lit >= 1f -> drawText(letter, color = color)
                            lit <= 0f -> drawText(letter, color = color.copy(alpha = DimAlpha))
                            else -> {
                                val filled = (w * lit).coerceIn(0f, w)
                                val edge = (w * EdgeShare).coerceAtLeast(1f)
                                val solid = (filled - edge).coerceAtLeast(0f)
                                val dim = color.copy(alpha = DimAlpha)
                                val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    0f to color,
                                    (solid / w).coerceIn(0f, 1f) to color,
                                    (filled / w).coerceIn(0f, 1f) to dim,
                                    1f to dim,
                                    startX = 0f,
                                    endX = w,
                                )
                                drawText(letter, brush = brush)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun Modifier.edgeFade(): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to androidx.compose.ui.graphics.Color.Transparent,
                0.12f to androidx.compose.ui.graphics.Color.Black,
                0.78f to androidx.compose.ui.graphics.Color.Black,
                1f to androidx.compose.ui.graphics.Color.Transparent,
            ),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
        )
    }

private val LyricsBleed = 20.dp
private val LinePad = 10.dp
private val LineSpacing = 10.dp
private val GapHeight = 56.dp
private val GlowRadius = 12.dp
private const val GapSeconds = 4f
private const val GapLead = 0.65f
private const val GapFadeMs = 200
private const val DimAlpha = 0.35f
private const val GlowAlpha = 0.35f
private const val NudgeStrength = 0.038f
private const val EdgeShare = 0.45f
private const val EdgeSlices = 12
private const val LyricsTopFade = 0.1f
private const val FollowPauseMs = 3_000L
private const val LineScrollMs = 650
private const val LineFadeMs = 700
private const val LineSettle = 0.35f
private const val SpanShare = 0.92f
