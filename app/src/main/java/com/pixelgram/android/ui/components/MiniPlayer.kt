package com.pixelgram.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.feature.player.FullPlayer
import com.pixelgram.android.feature.player.PlayPauseMorph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun MiniPlayer(modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val music = graph.music
    val state by music.state.collectAsState()
    val track = state.track
    AnimatedVisibility(
        visible = track != null,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier,
    ) {
        if (track == null) return@AnimatedVisibility
        val covers by graph.covers.found.collectAsState()
        val coverPath = graph.covers.key(state.audio)?.let { covers[it] } ?: track.cover
        val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val seed by produceState<Color?>(null, coverPath) {
            value = coverPath?.let { path ->
                withContext(Dispatchers.IO) { runCatching { com.pixelgram.android.feature.player.coverPalette(path) }.getOrNull() }
                    ?.let { it.getOrNull(3) ?: it.first() }
            }
        }
        val themed = remember(seed, dark) { seed?.let { com.pixelgram.android.feature.player.schemeFromSeed(it.toArgb(), dark) } }
        val accent by animateColorAsState(themed?.primary ?: MaterialTheme.colorScheme.primary, label = "mini-accent")
        val secondary by animateColorAsState(themed?.onSurfaceVariant ?: MaterialTheme.colorScheme.onSurfaceVariant, label = "mini-secondary")
        val surfaceTint by animateColorAsState(if (themed != null) accent.copy(alpha = if (dark) SurfaceTintDark else SurfaceTintLight) else Color.Transparent, label = "mini-surface")
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val full = with(density) { BarHeight.toPx() }
        val swipe = remember { Animatable(0f) }
        val shrink = full * ShrinkShare * tanh(swipe.value / (3f * full * ShrinkShare))
        Box(
            Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(BarHeight - with(density) { shrink.toDp() })
                .clip(RoundedCornerShape(CapsuleRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .graphicsLayer {
                    alpha = 1f - (shrink / (full * ShrinkShare)).coerceIn(0f, 1f) * 0.35f
                    clip = true
                }
                .background(surfaceTint)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (swipe.value > CloseDistance.toPx()) music.stop()
                                swipe.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                            }
                        },
                        onDragCancel = { scope.launch { swipe.animateTo(0f) } },
                    ) { change, amount ->
                        change.consume()
                        scope.launch { swipe.snapTo((swipe.value - amount).coerceAtLeast(0f)) }
                    }
                }
                .clickable(interactionSource = null, indication = ripple(color = accent)) { FullPlayer.open = true },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(start = 6.dp, end = 3.dp),
            ) {
                AnimatedContent(
                    targetState = coverPath to track.fileId,
                    contentKey = { it.second },
                    transitionSpec = {
                        (fadeIn(spring(dampingRatio = 0.8f, stiffness = 380f)) + androidx.compose.animation.scaleIn(spring(dampingRatio = 0.8f, stiffness = 380f), initialScale = 0.7f)) togetherWith
                            (fadeOut(spring(dampingRatio = 0.8f, stiffness = 380f)) + androidx.compose.animation.scaleOut(spring(dampingRatio = 0.8f, stiffness = 380f), targetScale = 0.7f))
                    },
                    label = "mini-cover",
                ) { (path, _) ->
                    val image: Any? = path?.let { java.io.File(it) } ?: track.minithumb
                    Box(
                        Modifier.size(CoverSize).clip(RoundedCornerShape(CoverRadius)).background(accent.copy(alpha = if (dark) 0.2f else 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(Icons.Rounded.MusicNote, null, tint = accent, modifier = Modifier.size(20.dp))
                        if (image != null) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                AnimatedContent(
                    targetState = track.title to track.performer,
                    transitionSpec = {
                        val up = state.direction
                        (slideInVertically { it * up } + fadeIn()) togetherWith (slideOutVertically { -it * up } + fadeOut())
                    },
                    modifier = Modifier.weight(1f),
                    label = "mini-title",
                ) { (title, performer) ->
                    Column {
                        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (performer.isNotBlank()) Text(performer, style = MaterialTheme.typography.bodySmall, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                val short = state.duration in 1 until LongTrackMs
                if (short) {
                    Slot(accent, onClick = music::previous) {
                        androidx.compose.material3.Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous), tint = accent, modifier = Modifier.size(20.dp))
                    }
                }
                Slot(accent, onClick = music::toggle) {
                    ProgressRing(
                        progress = if (state.duration > 0) state.position.toFloat() / state.duration else 0f,
                        playing = state.playing,
                        color = accent,
                    )
                    PlayPauseMorph(state.playing, 18.dp, accent)
                }
                val close by animateFloatAsState(if (state.playing) 0f else 1f, spring(dampingRatio = 0.8f, stiffness = 380f), label = "mini-close")
                val closeColor = lerp(accent, secondary, close)
                Slot(closeColor, onClick = { if (state.playing) music.next() else music.stop() }) {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.SkipNext,
                        stringResource(R.string.player_next),
                        tint = accent,
                        modifier = Modifier.size(20.dp).graphicsLayer {
                            alpha = (1f - close).coerceIn(0f, 1f)
                            rotationZ = close * 90f
                            scaleX = 1f - 0.4f * close
                            scaleY = 1f - 0.4f * close
                        },
                    )
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Close,
                        stringResource(R.string.action_close),
                        tint = secondary,
                        modifier = Modifier.size(20.dp).graphicsLayer {
                            alpha = close.coerceIn(0f, 1f)
                            rotationZ = (1f - close) * 90f
                            scaleX = 0.6f + 0.4f * close
                            scaleY = 0.6f + 0.4f * close
                        },
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun Slot(color: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val radius by androidx.compose.animation.core.animateDpAsState(if (pressed) 10.dp else SlotRadius, spring(dampingRatio = 0.6f, stiffness = 600f), label = "slot")
    Box(
        Modifier
            .size(SlotSize)
            .padding(SlotInset)
            .clip(RoundedCornerShape(radius))
            .background(color.copy(alpha = if (pressed) 0.18f else 0f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ProgressRing(progress: Float, playing: Boolean, color: Color) {
    val wave = remember { Animatable(if (playing) 1f else 0f) }
    LaunchedEffect(playing) { wave.animateTo(if (playing) 1f else 0f, spring(dampingRatio = 1f, stiffness = 200f)) }
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        var last = 0L
        while (playing) {
            withFrameNanos { now ->
                if (last != 0L) phase = (phase + (now - last) / 1_000_000_000f * WaveSpeed * 2f * PI.toFloat()) % (2000f * PI.toFloat())
                last = now
            }
        }
    }
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), androidx.compose.animation.core.tween(300), label = "ring")
    Canvas(Modifier.size(RingBox)) {
        val radius = RingRadius.toPx()
        val stroke = RingStroke.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val circumference = radius * 2f * PI.toFloat()
        if (shown < 1f) {
            val gap = if (shown > 0f) (2.dp.toPx() + stroke) / circumference * 360f else 0f
            val sweep = (1f - shown) * 360f - gap * 2f
            if (sweep > 0f) {
                drawArc(color.copy(alpha = TrackAlpha), shown * 360f - 90f + gap, sweep, false, Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2), style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        if (shown <= 0f) return@Canvas
        val edge = when {
            shown < 0.1f -> shown / 0.1f
            shown > 0.9f -> (1f - shown) / 0.1f
            else -> 1f
        }
        val amp = 1.dp.toPx() * wave.value * edge.coerceIn(0f, 1f)
        if (amp < 0.25f) {
            drawArc(color, -90f, shown * 360f, false, Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2), style = Stroke(stroke, cap = StrokeCap.Round))
        } else {
            val length = circumference * shown
            val wavelength = 11.dp.toPx()
            val steps = maxOf(2, (length / 1.dp.toPx()).toInt())
            val path = Path()
            for (i in 0..steps) {
                val along = i * length / steps
                val angle = along / radius - PI.toFloat() / 2f
                val r = radius + sin(along / wavelength * 2f * PI.toFloat() + phase) * amp
                val x = center.x + cos(angle) * r
                val y = center.y + sin(angle) * r
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

private val BarHeight = 48.dp
private val CoverSize = 36.dp
private val CoverRadius = 18.dp
private val CapsuleRadius = 24.dp
private val SlotSize = 36.dp
private val SlotInset = 0.dp
private val SlotRadius = 15.dp
private val RingBox = 34.dp
private val RingRadius = 15.dp
private val RingStroke = 2.5.dp
private val CloseDistance = 40.dp
private const val ShrinkShare = 0.3f
private const val TrackAlpha = 0.3f
private const val WaveSpeed = 7f / 11f
private const val LongTrackMs = 600_000L
private const val SurfaceTintDark = 0.12f
private const val SurfaceTintLight = 0.08f
