package com.pixelgram.android.feature.chat

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

data class VideoEdit(val startMs: Long = 0L, val endMs: Long = -1L, val muted: Boolean = false, val quality: Int = QualityOriginal, val round: Boolean = false, val cropX: Float = 0.5f, val cropY: Float = 0.5f, val cropSize: Float = 1f) {
    val untouched get() = startMs == 0L && endMs < 0 && !muted && quality == QualityOriginal && !round

    companion object {
        const val QualityOriginal = 3
        val Heights = intArrayOf(360, 480, 720, 0)
    }
}

class VideoInfo(val width: Int, val height: Int, val durationMs: Long, val bytes: Long)

fun videoInfo(path: String): VideoInfo {
    val retriever = android.media.MediaMetadataRetriever()
    return runCatching {
        retriever.setDataSource(path)
        val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        if (rotation == 90 || rotation == 270) VideoInfo(h, w, duration, File(path).length()) else VideoInfo(w, h, duration, File(path).length())
    }.getOrDefault(VideoInfo(0, 0, 0, File(path).length())).also { runCatching { retriever.release() } }
}

@OptIn(UnstableApi::class)
suspend fun exportVideo(context: Context, path: String, edit: VideoEdit): String {
    if (edit.untouched) return path
    val target = File(context.cacheDir, "video-${System.currentTimeMillis()}.mp4")
    val clipping = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(edit.startMs)
        .apply { if (edit.endMs > 0) setEndPositionMs(edit.endMs) }
        .build()
    val item = MediaItem.Builder().setUri(android.net.Uri.fromFile(File(path))).setClippingConfiguration(clipping).build()
    val effects = mutableListOf<androidx.media3.common.Effect>()
    if (edit.round) {
        val info = videoInfo(path)
        if (info.width > 0 && info.height > 0) {
            val side = minOf(info.width, info.height) * edit.cropSize
            val left = (edit.cropX * info.width - side / 2f).coerceIn(0f, info.width - side)
            val top = (edit.cropY * info.height - side / 2f).coerceIn(0f, info.height - side)
            val l = left / info.width * 2f - 1f
            val r = (left + side) / info.width * 2f - 1f
            val t = 1f - top / info.height * 2f
            val b = 1f - (top + side) / info.height * 2f
            effects += androidx.media3.effect.Crop(l, r, b, t)
        }
        val side = VideoEdit.Heights[edit.quality].takeIf { it > 0 }?.coerceAtMost(640) ?: 480
        effects += Presentation.createForWidthAndHeight(side, side, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP)
    } else {
        val height = VideoEdit.Heights[edit.quality]
        if (height > 0) effects += Presentation.createForHeight(height)
    }
    val edited = EditedMediaItem.Builder(item).setRemoveAudio(edit.muted).setEffects(Effects(emptyList(), effects)).build()
    val ok = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                })
                .build()
            transformer.start(edited, target.absolutePath)
            continuation.invokeOnCancellation { transformer.cancel() }
        }
    }
    return if (ok && target.length() > 0) target.absolutePath else path
}

private fun clock(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
fun VideoEditScreen(path: String, initial: VideoEdit, allowRound: Boolean = true, onClose: () -> Unit, onDone: (VideoEdit) -> Unit) {
    val context = LocalContext.current
    val info = remember(path) { videoInfo(path) }
    val duration = info.durationMs.coerceAtLeast(1L)
    var edit by remember { mutableStateOf(initial) }
    var playing by remember { mutableStateOf(true) }
    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(File(path))))
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(edit.muted) { player.volume = if (edit.muted) 0f else 1f }
    LaunchedEffect(playing) { player.playWhenReady = playing }
    LaunchedEffect(edit.startMs, edit.endMs) {
        player.seekTo(edit.startMs)
        while (true) {
            val end = if (edit.endMs > 0) edit.endMs else duration
            if (player.currentPosition >= end || player.currentPosition < edit.startMs) player.seekTo(edit.startMs)
            delay(60)
        }
    }
    val end = if (edit.endMs > 0) edit.endMs else duration
    val estimate = run {
        val ratio = (end - edit.startMs).toFloat() / duration
        val scale = when (edit.quality) { 0 -> 0.18f; 1 -> 0.3f; 2 -> 0.6f; else -> 1f }
        (info.bytes * ratio * scale).toLong()
    }
    val outW = if (edit.round) 480 else if (VideoEdit.Heights[edit.quality] > 0 && info.height > 0) VideoEdit.Heights[edit.quality] * info.width / info.height else info.width
    val outH = if (edit.round) 480 else VideoEdit.Heights[edit.quality].takeIf { it > 0 } ?: info.height
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = Color.White) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.video_edit_title), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("${outW}x$outH, ${clock(end - edit.startMs)}, ~${android.text.format.Formatter.formatShortFileSize(context, estimate)}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.size(48.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth().clickable(interactionSource = null, indication = null) { playing = !playing }, contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().aspectRatio(if (info.height > 0) info.width.toFloat() / info.height else 1f)) {
                    androidx.media3.ui.compose.PlayerSurface(player, modifier = Modifier.fillMaxSize(), surfaceType = androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW)
                    if (edit.round) RoundCropOverlay(edit) { x, y, size -> edit = edit.copy(cropX = x, cropY = y, cropSize = size) }
                }
                if (!playing) Box(Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(clock(edit.startMs), color = Color.White, modifier = Modifier.weight(1f))
                    Text(clock(end), color = Color.White)
                }
                RangeSlider(
                    value = edit.startMs.toFloat()..end.toFloat(),
                    onValueChange = { range ->
                        val start = range.start.toLong()
                        val stop = range.endInclusive.toLong()
                        if (stop - start >= 500) edit = edit.copy(startMs = start, endMs = if (stop >= duration - 50) -1L else stop)
                    },
                    valueRange = 0f..duration.toFloat(),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.video_smaller), color = Color.White, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.video_higher), color = Color.White)
                }
                Slider(edit.quality.toFloat(), { edit = edit.copy(quality = it.toInt()) }, valueRange = 0f..3f, steps = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = edit.muted,
                        onClick = { edit = edit.copy(muted = !edit.muted) },
                        label = { Text(stringResource(if (edit.muted) R.string.video_muted else R.string.video_sound)) },
                        leadingIcon = { Icon(if (edit.muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(20.dp)) },
                        modifier = Modifier.height48(),
                    )
                    if (allowRound) FilterChip(
                        selected = edit.round,
                        onClick = { edit = edit.copy(round = !edit.round) },
                        label = { Text(stringResource(R.string.send_as_round)) },
                        leadingIcon = { Icon(Icons.Rounded.RadioButtonChecked, null, Modifier.size(20.dp)) },
                        modifier = Modifier.height48(),
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { playing = !playing }) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.action_cancel).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onDone(edit) }) { Text(stringResource(R.string.action_done).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun Modifier.height48(): Modifier = this.then(Modifier.height(48.dp))

@Composable
private fun RoundCropOverlay(edit: VideoEdit, onChange: (Float, Float, Float) -> Unit) {
    val latest = androidx.compose.runtime.rememberUpdatedState(edit)
    val handleSlop = with(androidx.compose.ui.platform.LocalDensity.current) { 32.dp.toPx() }
    androidx.compose.foundation.Canvas(
        Modifier.fillMaxSize().pointerInput(Unit) {
            fun geometry(e: VideoEdit): Triple<Float, Float, Float> {
                val side = minOf(size.width, size.height) * e.cropSize
                val cx = (e.cropX * size.width).coerceIn(side / 2f, size.width - side / 2f)
                val cy = (e.cropY * size.height).coerceIn(side / 2f, size.height - side / 2f)
                return Triple(cx, cy, side)
            }
            fun emit(cx: Float, cy: Float, side: Float) {
                val s = side.coerceIn(minOf(size.width, size.height) * 0.3f, minOf(size.width, size.height).toFloat())
                val x = cx.coerceIn(s / 2f, size.width - s / 2f)
                val y = cy.coerceIn(s / 2f, size.height - s / 2f)
                onChange(x / size.width, y / size.height, s / minOf(size.width, size.height))
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                val (cx0, cy0, side0) = geometry(latest.value)
                val r = side0 / 2f
                val handle = androidx.compose.ui.geometry.Offset(cx0 + r * 0.7071f, cy0 + r * 0.7071f)
                val resizing = (down.position - handle).getDistance() < handleSlop
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.none { it.pressed }) break
                    val (cx, cy, side) = geometry(latest.value)
                    if (resizing) {
                        val p = event.changes.first().position
                        val d = (p - androidx.compose.ui.geometry.Offset(cx, cy)).getDistance()
                        emit(cx, cy, d / 0.7071f)
                    } else {
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        emit(cx + pan.x, cy + pan.y, side * zoom)
                    }
                    event.changes.forEach { it.consume() }
                }
            }
        },
    ) {
        val side = minOf(size.width, size.height) * edit.cropSize
        val cx = (edit.cropX * size.width).coerceIn(side / 2f, size.width - side / 2f)
        val cy = (edit.cropY * size.height).coerceIn(side / 2f, size.height - side / 2f)
        val square = androidx.compose.ui.geometry.Rect(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f)
        val outside = androidx.compose.ui.graphics.Path().apply {
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            addRect(square)
        }
        drawPath(outside, Color.Black.copy(alpha = 0.65f))
        val corners = androidx.compose.ui.graphics.Path().apply {
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            addRect(square)
            addOval(square)
        }
        drawPath(corners, Color.Black.copy(alpha = 0.35f))
        drawRect(Color.White.copy(alpha = 0.7f), square.topLeft, square.size, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        drawCircle(Color.White, side / 2f, androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        val h = androidx.compose.ui.geometry.Offset(cx + side / 2f * 0.7071f, cy + side / 2f * 0.7071f)
        drawCircle(Color.White, 9.dp.toPx(), h)
    }
}
