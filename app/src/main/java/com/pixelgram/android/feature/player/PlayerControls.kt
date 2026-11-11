package com.pixelgram.android.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pixelgram.android.R
import kotlin.math.PI
import kotlin.math.sin

private enum class Control { Previous, Play, Next }

@Composable
internal fun MorphControls(
    playing: Boolean,
    onPrevious: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    scale: Float = 0.82f,
    gap: Dp = 8.dp,
    iconSize: Dp = 28.dp,
    playIcon: Dp = 34.dp,
    wide: Boolean = true,
    onHold: ((Int) -> Unit)? = null,
) {
    val sources = remember { Control.entries.associateWith { MutableInteractionSource() } }
    val pressed = Control.entries.firstOrNull { sources.getValue(it).collectIsPressedAsState().value }
    var held by remember { mutableStateOf(0) }
    val holding = when (pressed) {
        Control.Previous -> -1
        Control.Next -> 1
        else -> 0
    }
    LaunchedEffect(holding) {
        if (onHold == null) return@LaunchedEffect
        if (holding != 0) {
            kotlinx.coroutines.delay(HoldDelayMs)
            held = holding
            onHold(holding)
        } else if (held != 0) {
            onHold(0)
        }
    }
    val release: (() -> Unit) -> () -> Unit = { action ->
        {
            if (held != 0) {
                held = 0
                onHold?.invoke(0)
            } else {
                action()
            }
        }
    }
    val sideWidth = { me: Control ->
        when (pressed) {
            me -> 76f
            Control.Play -> 58f
            else -> 64f
        }
    }
    val playWidth = when (pressed) {
        Control.Play -> 100f
        Control.Previous, Control.Next -> 80f
        else -> 88f
    } + if (wide) 44f else 0f
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        MorphButton(sideWidth(Control.Previous), 64f, if (pressed == Control.Previous) 18f else 32f, scheme.secondaryContainer to scheme.onSecondaryContainer, sources.getValue(Control.Previous), release(onPrevious), scale) {
            Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous), Modifier.size(iconSize))
        }
        MorphButton(
            playWidth,
            88f,
            when {
                pressed == Control.Play -> 20f
                playing -> 28f
                else -> 44f
            },
            scheme.primary to scheme.onPrimary,
            sources.getValue(Control.Play),
            onPlay,
            scale,
        ) { PlayPauseMorph(playing, playIcon) }
        MorphButton(sideWidth(Control.Next), 64f, if (pressed == Control.Next) 18f else 32f, scheme.secondaryContainer to scheme.onSecondaryContainer, sources.getValue(Control.Next), release(onNext), scale) {
            Icon(Icons.Rounded.SkipNext, stringResource(R.string.player_next), Modifier.size(iconSize))
        }
    }
}

@Composable
private fun MorphButton(
    width: Float,
    height: Float,
    radius: Float,
    colors: Pair<Color, Color>,
    source: MutableInteractionSource,
    onClick: () -> Unit,
    scale: Float,
    content: @Composable () -> Unit,
) {
    val shownWidth by animateFloatAsState(width, spring(dampingRatio = 1f, stiffness = 700f))
    val shownRadius by animateFloatAsState(radius, spring(dampingRatio = 0.55f, stiffness = 520f))
    Box(
        Modifier
            .size((shownWidth * scale).dp, (height * scale).dp)
            .clip(RoundedCornerShape((shownRadius * scale).coerceAtLeast(0f).dp))
            .background(colors.first)
            .clickable(interactionSource = source, indication = ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.second) { content() }
    }
}

@Composable
internal fun PlayPauseMorph(playing: Boolean, size: Dp, tint: Color = LocalContentColor.current) {
    val alt by animateFloatAsState(if (playing) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 480f))
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = if (playing) null else stringResource(R.string.player_play),
            tint = tint,
            modifier = Modifier.size(size).graphicsLayer {
                alpha = (1f - alt * 1.6f).coerceIn(0f, 1f)
                rotationZ = MorphTurn * alt
                scaleX = 1f - 0.3f * alt
                scaleY = 1f - 0.3f * alt
            },
        )
        Icon(
            Icons.Rounded.Pause,
            contentDescription = if (playing) stringResource(R.string.player_pause) else null,
            tint = tint,
            modifier = Modifier.size(size).graphicsLayer {
                alpha = ((alt - 0.35f) * 1.6f).coerceIn(0f, 1f)
                rotationZ = -MorphTurn * (1f - alt)
                scaleX = 0.7f + 0.3f * alt
                scaleY = 0.7f + 0.3f * alt
            },
        )
    }
}

@Composable
internal fun WavySeekBar(fraction: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier, playing: Boolean = true, durationMs: Long = 0) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val seek by androidx.compose.runtime.rememberUpdatedState(onSeek)
    val amplitude = remember { Animatable(if (playing) 1f else 0f) }
    var phase by remember { mutableFloatStateOf(0f) }
    val active = playing && drag == null
    LaunchedEffect(active) {
        amplitude.animateTo(if (active) 1f else 0f, tween(WaveSettleMs, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(active) {
        var last = 0L
        while (active || amplitude.value > 0.001f) {
            withFrameNanos { now ->
                if (last != 0L) phase += (now - last) / 1_000_000_000f * WaveCyclesPerSecond * amplitude.value
                last = now
            }
        }
    }
    val colors = MaterialTheme.colorScheme
    val shown = (drag ?: fraction).coerceIn(0f, 1f)
    Box(modifier.height(32.dp)) {
        Canvas(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent()
                            val first = down.changes.firstOrNull { it.pressed } ?: continue
                            var at = (first.position.x / size.width).coerceIn(0f, 1f)
                            drag = at
                            first.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                at = (change.position.x / size.width).coerceIn(0f, 1f)
                                drag = at
                                change.consume()
                                if (!change.pressed) break
                            }
                            seek(at)
                            drag = null
                        }
                    }
                },
        ) {
            val mid = size.height / 2f
            val stroke = 4.dp.toPx()
            val gap = 5.dp.toPx()
            val x = size.width * shown
            val amp = 3.dp.toPx() * amplitude.value
            val wavelength = 30.dp.toPx()
            val end = (x - gap).coerceAtLeast(0f)
            if (end > stroke / 2) {
                val path = Path()
                var px = stroke / 2
                path.moveTo(px, mid + amp * sin((px / wavelength - phase) * 2f * PI.toFloat()))
                while (px < end) {
                    px = (px + 1f).coerceAtMost(end)
                    path.lineTo(px, mid + amp * sin((px / wavelength - phase) * 2f * PI.toFloat()))
                }
                drawPath(path, colors.primary, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val rest = x + gap
            if (rest < size.width - stroke / 2) {
                drawLine(colors.secondaryContainer, Offset(rest, mid), Offset(size.width - stroke / 2, mid), stroke, StrokeCap.Round)
            }
            drawCircle(colors.primary, stroke / 2, Offset(size.width - stroke / 2, mid))
            val thumb = if (drag != null) 6.dp.toPx() else 4.dp.toPx()
            drawRoundRect(
                colors.primary,
                topLeft = Offset(x - thumb / 2, mid - 11.dp.toPx()),
                size = Size(thumb, 22.dp.toPx()),
                cornerRadius = CornerRadius(thumb / 2),
            )
        }
        val label = drag
        if (label != null && durationMs > 0) {
            Layout(
                content = {
                    Surface(shape = CircleShape, color = colors.inverseSurface, contentColor = colors.inverseOnSurface, shadowElevation = 4.dp) {
                        Text(formatTime((label * durationMs).toLong()), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                },
                modifier = Modifier.matchParentSize().zIndex(1f),
            ) { measurables, constraints ->
                val bubble = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = (constraints.maxWidth * label - bubble.width / 2f).toInt().coerceIn(0, (constraints.maxWidth - bubble.width).coerceAtLeast(0))
                    bubble.place(x, -bubble.height - 6.dp.roundToPx())
                }
            }
        }
    }
}

internal fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = total % 3600 / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private const val MorphTurn = 45f
private const val HoldDelayMs = 400L
private const val WaveSettleMs = 420
private const val WaveCyclesPerSecond = 0.45f
