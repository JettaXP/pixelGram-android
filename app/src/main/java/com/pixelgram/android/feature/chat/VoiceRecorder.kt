package com.pixelgram.android.feature.chat

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class VoiceRecording(val path: String, val duration: Int, val waveform: ByteArray)

class VoiceRecorder(private val context: Context) {
    var active by mutableStateOf(false)
        private set
    var locked by mutableStateOf(false)
    var level by mutableFloatStateOf(0f)
        private set
    var elapsed by mutableLongStateOf(0L)
        private set
    var drag by mutableFloatStateOf(0f)

    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var started = 0L
    private var sampler: Job? = null
    private val samples = ArrayList<Short>()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun start(): Boolean {
        if (active) return true
        val ogg = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val target = File(context.cacheDir, "voice-${System.currentTimeMillis()}.${if (ogg) "ogg" else "m4a"}")
        val created = runCatching {
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                if (ogg) {
                    setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                } else {
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
                setAudioSamplingRate(SampleRate)
                setAudioEncodingBitRate(BitRate)
                setAudioChannels(1)
                setOutputFile(target.absolutePath)
                prepare()
                start()
            }
        }.getOrNull() ?: return false
        recorder = created
        file = target
        started = SystemClock.elapsedRealtime()
        samples.clear()
        elapsed = 0L
        locked = false
        active = true
        sampler = scope.launch {
            while (isActive) {
                val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                samples += amplitude.coerceIn(0, Short.MAX_VALUE.toInt()).toShort()
                level = (amplitude / 12000f).coerceIn(0f, 1f)
                elapsed = SystemClock.elapsedRealtime() - started
                delay(SampleMs)
            }
        }
        return true
    }

    fun stop(): VoiceRecording? {
        if (!active) return null
        val target = file
        val duration = ((SystemClock.elapsedRealtime() - started) / 1000L).toInt()
        release()
        if (target == null || duration < 1) {
            target?.delete()
            return null
        }
        return VoiceRecording(target.absolutePath, duration, waveform(samples.toShortArray()))
    }

    fun cancel() {
        val target = file
        release()
        target?.delete()
    }

    private fun release() {
        sampler?.cancel()
        sampler = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        active = false
        locked = false
        level = 0f
        drag = 0f
    }

    companion object {
        private const val SampleRate = 48000
        private const val BitRate = 32000
        private const val SampleMs = 50L

        fun waveform(source: ShortArray): ByteArray {
            val resultSamples = 100
            val samples = IntArray(resultSamples)
            var sampleIndex = 0L
            var peakSample = 0
            val sampleRate = maxOf(1, source.size / resultSamples)
            var index = 0
            for (value in source) {
                val sample = kotlin.math.abs(value.toInt())
                if (sample > peakSample) peakSample = sample
                if (sampleIndex++ % sampleRate == 0L) {
                    if (index < resultSamples) samples[index++] = peakSample
                    peakSample = 0
                }
            }
            var peak = (samples.sumOf { it.toLong() } * 1.8f / resultSamples).toInt()
            if (peak < 2500) peak = 2500
            for (i in 0 until resultSamples) if (samples[i] > peak) samples[i] = peak
            val bytes = ByteArray(resultSamples * 5 / 8 + 1 + 4)
            for (i in 0 until resultSamples) {
                val value = minOf(31, kotlin.math.abs(samples[i]) * 31 / peak) and 31
                val bitOffset = i * 5
                val at = bitOffset / 8
                val bits = value shl (bitOffset % 8)
                bytes[at] = (bytes[at].toInt() or (bits and 0xFF)).toByte()
                bytes[at + 1] = (bytes[at + 1].toInt() or ((bits shr 8) and 0xFF)).toByte()
            }
            return bytes.copyOf(resultSamples * 5 / 8 + 1)
        }
    }
}
