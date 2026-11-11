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
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
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
    val current by remember(items) { derivedStateOf { items.indexOfLast { it.start <= now.value + ActiveLead } } }
    val active = current.coerceAtLeast(0)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val heights = remember(items) { mutableStateMapOf<Int, Int>() }
    var auto by remember { mutableStateOf(true) }
    var manual by remember { mutableFloatStateOf(0f) }
    var touched by remember { mutableLongStateOf(0L) }
    var initial by remember(items) { mutableStateOf(true) }
    var fling by remember { mutableStateOf<Job?>(null) }
    val gapPx = with(density) { LineGap.toPx() }
    val fallback = with(density) { FallbackHeight.toPx() }
    val tracker = remember { VelocityTracker() }
    val decay = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }
    val accent = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(items) {
        snapshotFlow { heights.size >= items.size }.first { it }
        initial = false
    }
    LaunchedEffect(auto, touched) {
        if (!auto) {
            delay(PreviewMs)
            auto = true
        }
    }
    LaunchedEffect(auto) {
        if (auto && abs(manual) >= 1f) {
            val anim = Animatable(manual)
            anim.animateTo(0f, tween((abs(manual) / 4f).toInt().coerceIn(200, 600), easing = FastOutSlowInEasing)) { manual = value }
        }
        if (auto) manual = 0f
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = constraints.maxHeight.toFloat()
        val anchor = viewport * AnchorRatio
        fun height(i: Int) = heights[i]?.toFloat() ?: fallback
        fun spacing(i: Int) = if (items[i] is LyricItem.Gap) 0f else gapPx
        val positions by remember(items, active) {
            derivedStateOf {
                val map = HashMap<Int, Float>()
                if (items.isEmpty()) return@derivedStateOf map
                map[active] = 0f
                var y = 0f
                for (i in active - 1 downTo 0) {
                    y -= height(i) + spacing(i)
                    map[i] = y
                }
                y = 0f
                for (i in active until items.size - 1) {
                    y += height(i) + spacing(i + 1)
                    map[i + 1] = y
                }
                map
            }
        }
        val bounds by remember(items, active, viewport) {
            derivedStateOf {
                if (items.isEmpty()) return@derivedStateOf 0f to 0f
                val below = (active until items.size - 1).sumOf { (height(it) + spacing(it + 1)).toDouble() }.toFloat()
                val above = (0 until active).sumOf { (height(it) + spacing(it)).toDouble() }.toFloat()
                val low = with(density) { 100.dp.toPx() } - anchor - below - height(items.size - 1)
                val high = viewport - with(density) { 150.dp.toPx() } - anchor + above
                minOf(low, high) to maxOf(low, high)
            }
        }
        LaunchedEffect(bounds) { manual = manual.coerceIn(bounds.first, bounds.second) }

        Box(
            Modifier
                .fillMaxSize()
                .edgeFade()
                .clipToBounds()
                .pointerInput(items) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fling?.cancel()
                        tracker.resetTracking()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                            change.consume()
                            auto = false
                            touched = System.currentTimeMillis()
                            manual = (manual + over).coerceIn(bounds.first, bounds.second)
                        } ?: return@awaitEachGesture
                        tracker.addPosition(start.uptimeMillis, start.position)
                        verticalDrag(start.id) { change ->
                            manual = (manual + change.positionChange().y).coerceIn(bounds.first, bounds.second)
                            touched = System.currentTimeMillis()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                        }
                        val velocity = tracker.calculateVelocity().y
                        fling = scope.launch {
                            AnimationState(initialValue = manual, initialVelocity = velocity).animateDecay(decay) {
                                val clamped = value.coerceIn(bounds.first, bounds.second)
                                manual = clamped
                                touched = System.currentTimeMillis()
                                if (value != clamped) cancelAnimation()
                            }
                        }
                    }
                },
        ) {
            items.forEachIndexed { index, item ->
                key(index) {
                    val distance = abs(index - active)
                    val target = anchor + (positions[index] ?: ((index - active) * fallback))
                    val frozen = remember { mutableFloatStateOf(target) }
                    LaunchedEffect(auto, target, initial) {
                        if (auto || initial) frozen.floatValue = target
                    }
                    val offset by animateFloatAsState(
                        if (auto) target else frozen.floatValue,
                        if (initial || !auto) snap() else tween(ShiftMs, (distance * StaggerMs).coerceAtMost(StaggerMaxMs), FastOutSlowInEasing),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .layout { measurable, c ->
                                val placeable = measurable.measure(c.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                                layout(placeable.width, 0) { placeable.place(0, 0) }
                            }
                            .offset { IntOffset(0, (offset + manual).roundToInt()) },
                    ) {
                        when (item) {
                            is LyricItem.Gap -> GapIndicator(
                                item,
                                now,
                                Modifier.onSizeChanged { heights[index] = it.height }.padding(horizontal = LinePad),
                            )
                            is LyricItem.Line -> {
                                val line = lines[item.index]
                                val end = items.getOrNull(index + 1)?.start ?: (line.time + 5f)
                                LyricRow(
                                    line = line,
                                    end = end,
                                    active = index == current,
                                    distance = distance,
                                    auto = auto,
                                    now = now,
                                    accent = accent,
                                    onSize = { heights[index] = it },
                                    onClick = {
                                        fling?.cancel()
                                        onSeek((line.time * 1000).toLong())
                                        auto = true
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GapIndicator(gap: LyricItem.Gap, position: State<Float>, modifier: Modifier) {
    val now = position.value
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

class PlayerClock(val position: () -> Float, val playing: () -> Boolean, val speed: () -> Float)

@Composable
private fun rememberSmoothPosition(clock: PlayerClock): State<Float> {
    val state = remember { mutableFloatStateOf(clock.position()) }
    var shown by state
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
    return state
}

private const val ResyncSeconds = 0.35f
private const val ClockPull = 0.06f

private class Word(val text: String, val start: Float, val end: Float)

private fun lineWords(line: LyricLine, end: Float): List<Word> =
    if (line.words.isNotEmpty()) {
        line.words.mapIndexedNotNull { i, word ->
            val text = word.text.trim()
            val stop = if (word.end.isNaN()) line.words.getOrNull(i + 1)?.time ?: end else word.end
            if (text.isEmpty()) null else Word(text, word.time, stop)
        }
    } else {
        val parts = line.text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val chars = parts.sumOf { it.length }.coerceAtLeast(1)
        val span = ((end - line.time) * SpanShare).coerceAtMost(chars * MaxCharSeconds).coerceAtLeast(0.3f)
        var at = line.time
        parts.map { text ->
            val length = span * text.length / chars
            Word(text, at, at + length).also { at += length }
        }
    }

private fun String.clusters(): List<String> {
    if (isEmpty()) return emptyList()
    val out = mutableListOf<String>()
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(this)
    var start = it.first()
    var end = it.next()
    while (end != java.text.BreakIterator.DONE) {
        out += substring(start, end)
        start = end
        end = it.next()
    }
    return out
}

private fun String.wholeLine(): Boolean = any {
    val d = Character.getDirectionality(it)
    d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || it in 'ऀ'..'෿'
}

private class ClusterMap(val word: IntArray, val inWord: IntArray, val length: IntArray)

private fun clusterMap(text: String, words: List<Word>, offsets: IntArray): ClusterMap {
    val count = offsets.size
    val word = IntArray(count) { -1 }
    val inWord = IntArray(count)
    val length = IntArray(count) { 1 }
    var pos = 0
    var cursor = 0
    words.forEachIndexed { w, item ->
        val at = text.indexOf(item.text, pos)
        if (at == -1) return@forEachIndexed
        val stop = at + item.text.length
        while (cursor < count && offsets[cursor] < at) cursor++
        val members = mutableListOf<Int>()
        while (cursor < count && offsets[cursor] < stop) members += cursor++
        members.forEachIndexed { k, c ->
            word[c] = w
            inWord[c] = k
            length[c] = members.size
        }
        if (cursor < count && offsets[cursor] == stop && stop < text.length && text[stop] == ' ') {
            word[cursor] = w
            inWord[cursor] = members.size
            length[cursor] = members.size + 1
            cursor++
        }
        pos = stop
    }
    return ClusterMap(word, inWord, length)
}

@Composable
private fun LyricRow(
    line: LyricLine,
    end: Float,
    active: Boolean,
    distance: Int,
    auto: Boolean,
    now: State<Float>,
    accent: Color,
    onSize: (Int) -> Unit,
    onClick: () -> Unit,
) {
    val target = when {
        active -> 1f
        !auto -> 0.3f
        else -> when (distance) {
            0 -> 0.45f
            1, 2 -> 0.35f
            3 -> 0.28f
            4 -> 0.2f
            else -> 0.15f
        }
    }
    val alpha by animateFloatAsState(target, tween(LineFadeMs))
    val blur by animateFloatAsState(if (active || !auto) 0f else (distance * BlurStep).coerceAtMost(BlurMax), tween(ShiftMs, easing = FastOutSlowInEasing))
    val style = TextStyle(
        fontSize = LyricSize,
        fontWeight = FontWeight.Bold,
        lineHeight = LyricSize * 1.3f,
        letterSpacing = (-0.5).sp,
        textAlign = TextAlign.Left,
        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    val text = line.text
    val words = remember(line, end) { lineWords(line, end) }
    val clusters = remember(text) { text.clusters() }
    val offsets = remember(text) {
        IntArray(clusters.size).also { out ->
            var at = 0
            clusters.forEachIndexed { i, c ->
                out[i] = at
                at += c.length
            }
        }
    }
    val map = remember(text, words) { clusterMap(text, words, offsets) }
    val whole = remember(text) { text.wholeLine() }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val glowPaint = remember { android.graphics.Paint().apply { isAntiAlias = true; typeface = android.graphics.Typeface.DEFAULT_BOLD } }
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { onSize(it.height) }
            .blur(blur.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = LinePad, vertical = 12.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = constraints.maxWidth
            val layout = remember(text, width, style) {
                measurer.measure(text, style, constraints = Constraints(minWidth = width, maxWidth = width))
            }
            val letters = remember(text, style) { clusters.map { measurer.measure(it, style, softWrap = false) } }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(with(density) { layout.size.height.toDp() })
                    .graphicsLayer(clip = false, compositingStrategy = CompositingStrategy.Offscreen),
            ) {
                if (text.isEmpty()) return@Canvas
                if (!active) {
                    drawText(layout, color = accent.copy(alpha = alpha))
                    return@Canvas
                }
                val position = now.value
                val progress = FloatArray(words.size)
                val sung = BooleanArray(words.size)
                words.forEachIndexed { w, word ->
                    sung[w] = position > word.end
                    progress[w] = when {
                        sung[w] -> 1f
                        position >= word.start -> ((position - word.start) / (word.end - word.start).coerceAtLeast(0.001f)).coerceIn(0f, 1f)
                        else -> 0f
                    }
                }
                if (whole) {
                    drawText(layout, color = accent.copy(alpha = FocusedAlpha))
                    words.indices.forEach { w ->
                        var left = Float.MAX_VALUE
                        var right = -Float.MAX_VALUE
                        var top = Float.MAX_VALUE
                        var bottom = -Float.MAX_VALUE
                        for (i in clusters.indices) if (map.word[i] == w) {
                            val b = layout.getBoundingBox(offsets[i])
                            left = minOf(left, b.left); right = maxOf(right, b.right)
                            top = minOf(top, b.top); bottom = maxOf(bottom, b.bottom)
                        }
                        if (left <= right && progress[w] > 0f) {
                            clipRect(left, top, right, bottom) {
                                drawText(layout, color = accent.copy(alpha = if (sung[w]) 1f else FocusedAlpha + (1f - FocusedAlpha) * progress[w]))
                            }
                        }
                    }
                    return@Canvas
                }
                val wobbles = FloatArray(words.size) { w ->
                    val since = (position - words[w].start) * 1000f
                    when {
                        since < 0f || since > 750f -> 0f
                        since < 125f -> since / 125f
                        else -> (1f - (since - 125f) / 625f).coerceAtLeast(0f)
                    }
                }
                fun letterProgress(i: Int): Float {
                    val w = map.word[i]
                    if (w == -1) return 0f
                    val word = words[w]
                    val duration = ((word.end - word.start) * 1000f).coerceAtLeast(100f)
                    val wordProgress = (position - word.start) * 1000f / duration
                    val length = map.length[i].toFloat()
                    return ((wordProgress - map.inWord[i] / length) * length).coerceIn(0f, 1f)
                }
                fun nudge(i: Int, lp: Float): Float {
                    val w = map.word[i]
                    return if (w != -1 && !sung[w] && progress[w] > 0f) NudgeStrength * sin(lp * PI.toFloat()) * exp(-3f * lp) else 0f
                }
                val pushes = FloatArray(layout.lineCount)
                for (i in clusters.indices) {
                    val w = map.word[i]
                    if (w == -1) continue
                    val scaleX = 1f + wobbles[w] * 0.025f + nudge(i, letterProgress(i)) * 0.3f
                    pushes[layout.getLineForOffset(offsets[i])] += layout.getBoundingBox(offsets[i]).width * (scaleX - 1f)
                }
                val used = FloatArray(layout.lineCount)
                val glowRadius = GlowRadius.toPx()
                for (i in clusters.indices) {
                    val letter = letters[i]
                    val box = layout.getBoundingBox(offsets[i])
                    val row = layout.getLineForOffset(offsets[i])
                    val top = layout.getLineBaseline(row) - letter.firstBaseline
                    val w = map.word[i]
                    val lp = letterProgress(i)
                    val n = nudge(i, lp)
                    val wobble = if (w != -1) wobbles[w] else 0f
                    val scaleX = 1f + wobble * 0.025f + n * 0.3f
                    val scaleY = 1f + wobble * 0.015f + n
                    val lw = letter.size.width.toFloat()
                    val lh = letter.size.height.toFloat()
                    withTransform({
                        translate(used[row] + box.left, top)
                        if (w != -1) scale(scaleX, scaleY, pivot = Offset(box.width / 2f, lh))
                    }) {
                        if (w != -1 && !sung[w] && progress[w] > 0.001f) {
                            val word = words[w]
                            val duration = (word.end - word.start) * 1000f
                            val perLetter = duration / word.text.length.coerceAtLeast(1)
                            val fade = (progress[w] * 5f).coerceIn(0f, 1f) * ((1f - progress[w]) * 8f).coerceIn(0f, 1f)
                            val impact = (((perLetter - 100f) / 250f).coerceIn(0f, 1f) * 0.6f + ((duration - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f).coerceIn(0f, 1f) * fade
                            if (impact > 0.01f) {
                                drawIntoCanvas { canvas ->
                                    glowPaint.maskFilter = BlurMaskFilter(glowRadius * impact, BlurMaskFilter.Blur.NORMAL)
                                    glowPaint.color = accent.copy(alpha = (0.35f * impact).coerceIn(0f, 0.4f)).toArgb()
                                    glowPaint.textSize = style.fontSize.toPx()
                                    canvas.nativeCanvas.drawText(clusters[i], 0f, letter.firstBaseline, glowPaint)
                                }
                            }
                        }
                        val base = when {
                            w == -1 -> FocusedAlpha
                            sung[w] || lp > 0.99f -> 1f
                            else -> FocusedAlpha + (1f - FocusedAlpha) * progress[w]
                        }
                        drawText(letter, color = accent.copy(alpha = base))
                        if (w != -1 && !sung[w] && lp > 0f && lp < 1f) {
                            val filled = lw * lp
                            val edge = (lw * 0.45f).coerceAtLeast(1f)
                            val solid = (filled - edge).coerceAtLeast(0f)
                            if (solid > 0f) clipRect(0f, 0f, solid, lh) { drawText(letter, color = accent) }
                            for (j in 0 until EdgeSlices) {
                                val from = solid + j * edge / EdgeSlices
                                val to = (solid + (j + 1) * edge / EdgeSlices + 0.5f).coerceAtMost(filled)
                                if (to > from) clipRect(from, 0f, to, lh) { drawText(letter, color = accent.copy(alpha = 1f - (j + 0.5f) / EdgeSlices)) }
                            }
                        }
                    }
                    if (w != -1) used[row] += box.width * (scaleX - 1f)
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

private val LinePad = 11.dp
private val LineGap = 4.dp
private val FallbackHeight = 68.dp
private val GapHeight = 56.dp
private val GlowRadius = 12.dp
private val LyricSize = 30.sp
private const val AnchorRatio = 0.3f
private const val ActiveLead = 0.1f
private const val GapSeconds = 4f
private const val GapLead = 0.65f
private const val GapFadeMs = 200
private const val FocusedAlpha = 0.3f
private const val NudgeStrength = 0.038f
private const val EdgeSlices = 12
private const val SpanShare = 0.9f
private const val MaxCharSeconds = 0.11f
private const val PreviewMs = 4_000L
private const val ShiftMs = 750
private const val StaggerMs = 20
private const val StaggerMaxMs = 200
private const val LineFadeMs = 250
private const val BlurStep = 1.5f
private const val BlurMax = 6f

