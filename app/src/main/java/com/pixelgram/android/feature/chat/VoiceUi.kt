package com.pixelgram.android.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlin.math.roundToInt

@Composable
fun VoiceButton(
    recorder: MediaRecorderUi,
    video: Boolean,
    hasPermission: () -> Boolean,
    onRequestPermission: () -> Unit,
    onTap: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val cancelAt = with(density) { CancelDistance.toPx() }
    val lockAt = with(density) { LockDistance.toPx() }
    val latestTap by rememberUpdatedState(onTap)
    var lift by remember { mutableStateOf(0f) }
    val scale by animateFloatAsState(if (recorder.active && !recorder.locked) 1.35f else 1f, label = "mic-scale")
    val pulse by animateFloatAsState(1f + recorder.level * 0.7f, tween(90), label = "mic-level")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        AnimatedVisibility(
            recorder.active && !recorder.locked,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.offset { IntOffset(0, (-LockOffset.toPx() + lift.coerceIn(-lockAt, 0f) * 0.5f).roundToInt()) },
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 4.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(20.dp))
                    Icon(Icons.Rounded.KeyboardArrowUp, null, Modifier.size(20.dp))
                }
            }
        }
        if (recorder.active && !recorder.locked) {
            Box(
                Modifier.size(56.dp).graphicsLayer {
                    scaleX = scale * pulse
                    scaleY = scale * pulse
                    translationX = recorder.drag
                }.clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = recorder.drag
                }
                .clip(if (recorder.active) CircleShape else RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(recorder.locked) {
                    if (recorder.locked) {
                        awaitEachGesture {
                            awaitFirstDown()
                            recorder.finish()
                        }
                        return@pointerInput
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (!hasPermission()) {
                            onRequestPermission()
                            return@awaitEachGesture
                        }
                        val pressedAt = System.currentTimeMillis()
                        val released = withTimeoutOrNull(TapMs) { waitForUpOrCancellation() }
                        if (released != null) {
                            latestTap()
                            return@awaitEachGesture
                        }
                        if (!recorder.start()) return@awaitEachGesture
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        var cancelled = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val dx = change.position.x - down.position.x
                            val dy = change.position.y - down.position.y
                            change.consume()
                            if (!recorder.locked) {
                                recorder.drag = dx.coerceAtMost(0f)
                                lift = dy.coerceAtMost(0f)
                                if (dx < -cancelAt) {
                                    recorder.cancel()
                                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                    cancelled = true
                                }
                                if (dy < -lockAt) {
                                    recorder.locked = true
                                    recorder.drag = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                }
                            }
                        }
                        lift = 0f
                        if (cancelled || recorder.locked) return@awaitEachGesture
                        recorder.drag = 0f
                        if (System.currentTimeMillis() - pressedAt < TapMs) {
                            recorder.cancel()
                            latestTap()
                            return@awaitEachGesture
                        }
                        recorder.finish()
                    }
                },
        ) {
            androidx.compose.animation.AnimatedContent(
                when {
                    recorder.locked -> 2
                    video -> 1
                    else -> 0
                },
                transitionSpec = { (androidx.compose.animation.scaleIn(initialScale = 0.4f) + fadeIn()) togetherWith (androidx.compose.animation.scaleOut(targetScale = 0.4f) + fadeOut()) },
                label = "record-icon",
            ) { state ->
            Icon(
                when (state) {
                    2 -> Icons.AutoMirrored.Rounded.Send
                    1 -> Icons.Rounded.Videocam
                    else -> Icons.Rounded.Mic
                },
                stringResource(R.string.voice_record),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            }
        }
    }
}

@Composable
fun RecordingBar(recorder: MediaRecorderUi) {
    val blink = rememberInfiniteTransition(label = "rec").animateFloat(1f, 0.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "rec-dot")
    val seconds = recorder.elapsed / 1000
    val centis = (recorder.elapsed % 1000) / 10
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
    ) {
        Box(Modifier.size(10.dp).graphicsLayer { alpha = blink.value }.clip(CircleShape).background(Color(0xFFE53935)))
        Spacer(Modifier.width(10.dp))
        Text("%d:%02d,%02d".format(seconds / 60, seconds % 60, centis), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        if (recorder.locked) {
            IconButton(onClick = { recorder.cancel() }) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.error) }
        } else {
            val fade = (1f + recorder.drag / 300f).coerceIn(0.2f, 1f)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = fade; translationX = recorder.drag * 0.4f }) {
                Icon(Icons.Rounded.KeyboardArrowLeft, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.voice_slide_cancel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
        }
    }
}

private val CancelDistance = 120.dp
private val LockDistance = 90.dp
private val LockOffset = 92.dp
private const val TapMs = 300L
