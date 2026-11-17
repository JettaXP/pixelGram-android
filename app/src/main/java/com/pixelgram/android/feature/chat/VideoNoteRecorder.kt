package com.pixelgram.android.feature.chat

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
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

class VideoNote(val path: String, val duration: Int)

class VideoNoteRecorder(private val context: Context, private val owner: LifecycleOwner, private val onDone: (VideoNote) -> Unit) : MediaRecorderUi {
    override var active by mutableStateOf(false)
        private set
    override var locked by mutableStateOf(false)
    override var drag by mutableFloatStateOf(0f)
    override var level by mutableFloatStateOf(0f)
        private set
    override var elapsed by mutableLongStateOf(0L)
        private set

    var controller: LifecycleCameraController? by mutableStateOf(null)
        private set
    private var recording: Recording? = null
    private var file: File? = null
    private var started = 0L
    private var send = false
    private var ticker: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    @SuppressLint("MissingPermission")
    override fun start(): Boolean {
        if (active) return true
        val camera = controller ?: LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.VIDEO_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            bindToLifecycle(owner)
        }.also { controller = it }
        val target = File(context.cacheDir, "round-${System.currentTimeMillis()}.mp4")
        val started = runCatching {
            camera.startRecording(FileOutputOptions.Builder(target).build(), AudioConfig.create(true), ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) finalize(event, target)
            }
        }.getOrNull() ?: return false
        recording = started
        file = target
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
        val duration = ((event.recordingStats.recordedDurationNanos / 1_000_000_000L)).toInt()
        if (send && !event.hasError() && duration >= 1) onDone(VideoNote(target.absolutePath, duration)) else target.delete()
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

    fun release() {
        cancel()
        runCatching { controller?.unbind() }
        controller = null
    }

    companion object {
        private const val MaxMs = 60_000L
    }
}

@Composable
fun VideoNoteOverlay(recorder: VideoNoteRecorder) {
    val camera = recorder.controller ?: return
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
                factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE } },
                update = { view -> if (view.controller !== camera) view.controller = camera },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
