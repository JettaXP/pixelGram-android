package com.pixelgram.android.feature.chat

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.util.Rational
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class VideoNote(val path: String, val duration: Int, val length: Int)

class VideoNoteRecorder(private val context: Context, private val owner: LifecycleOwner, private val onDone: (VideoNote) -> Unit) : MediaRecorderUi {
    override var active by mutableStateOf(false)
        private set
    override var locked by mutableStateOf(false)
    override var drag by mutableFloatStateOf(0f)
    override var level by mutableFloatStateOf(0f)
        private set
    override var elapsed by mutableLongStateOf(0L)
        private set

    var preview: Preview? by mutableStateOf(null)
        private set
    private var capture: VideoCapture<Recorder>? = null
    private var provider: ProcessCameraProvider? = null
    private var recording: Recording? = null
    private var started = 0L
    private var send = false
    private var ticker: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun prepare() {
        if (capture != null) return
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({ runCatching { provider = future.get() } }, ContextCompat.getMainExecutor(context))
    }

    private fun bind(camera: ProcessCameraProvider) {
        if (capture != null) return
        provider = camera
        val preview = Preview.Builder().build()
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD, androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))).build()
        val video = VideoCapture.Builder(recorder).setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY).build()
        val group = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(video)
            .setViewPort(ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build())
            .build()
        camera.unbindAll()
        camera.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, group)
        this.preview = preview
        capture = video
    }

    @SuppressLint("MissingPermission")
    override fun start(): Boolean {
        if (active) return true
        val video = capture ?: run {
            runCatching { bind(provider ?: ProcessCameraProvider.getInstance(context).get()) }
            capture
        } ?: return false
        val target = File(context.cacheDir, "round-${System.currentTimeMillis()}.mp4")
        val started = runCatching {
            video.output.prepareRecording(context, FileOutputOptions.Builder(target).build())
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Finalize) finalize(event, target)
                }
        }.getOrNull() ?: return false
        recording = started
        this.started = SystemClock.elapsedRealtime()
        send = false
        locked = false
        active = true
        ticker = scope.launch {
            while (isActive) {
                elapsed = SystemClock.elapsedRealtime() - this@VideoNoteRecorder.started
                if (elapsed >= MaxMs) finish()
                delay(50)
            }
        }
        return true
    }

    private fun finalize(event: VideoRecordEvent.Finalize, target: File) {
        if (!active) idle()
        val duration = (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
        if (!send || event.hasError() && event.error != VideoRecordEvent.Finalize.ERROR_NONE && duration < 1 || duration < 1) {
            target.delete()
            return
        }
        val retriever = android.media.MediaMetadataRetriever()
        val side = runCatching {
            retriever.setDataSource(target.absolutePath)
            val w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            minOf(w, h).takeIf { it > 0 } ?: DefaultLength
        }.getOrDefault(DefaultLength)
        runCatching { retriever.release() }
        onDone(VideoNote(target.absolutePath, duration, side))
    }

    override fun finish() {
        if (!active) return
        send = true
        stop()
    }

    override fun cancel() {
        if (!active) return
        send = false
        stop()
    }

    private fun stop() {
        ticker?.cancel()
        runCatching { recording?.stop() }
        recording = null
        active = false
        locked = false
        drag = 0f
        elapsed = 0L
    }

    private fun idle() {
        runCatching { provider?.unbindAll() }
        capture = null
        preview = null
    }

    fun release() {
        cancel()
        runCatching { provider?.unbindAll() }
        provider = null
        capture = null
        preview = null
    }

    companion object {
        private const val MaxMs = 60_000L
        private const val DefaultLength = 384
    }
}

@Composable
fun VideoNoteOverlay(recorder: VideoNoteRecorder) {
    val preview = recorder.preview ?: return
    val shown by animateFloatAsState(if (recorder.active) 1f else 0f, label = "round")
    if (shown == 0f) return
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f * shown)), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(280.dp).graphicsLayer {
                scaleX = 0.6f + 0.4f * shown
                scaleY = 0.6f + 0.4f * shown
                alpha = shown
            }.clip(CircleShape).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                update = { view -> preview.surfaceProvider = view.surfaceProvider },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
