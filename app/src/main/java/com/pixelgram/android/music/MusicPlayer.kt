package com.pixelgram.android.music

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.pixelgram.android.td.TdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

data class Track(val fileId: Int, val title: String, val performer: String, val cover: String?, val minithumb: ByteArray?)

data class PlayerState(
    val track: Track? = null,
    val playing: Boolean = false,
    val loading: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

class MusicPlayer(private val context: Context, private val td: TdClient) {
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateFlow = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = stateFlow.asStateFlow()
    private var queue: List<TdApi.Audio> = emptyList()
    private var index = 0
    private var loadJob: Job? = null
    private var ticker: Job? = null
    private var controller: ListenableFuture<MediaController>? = null

    private val exo: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(listener) }
    }

    val player: Player by lazy {
        object : ForwardingPlayer(exo) {
            override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()

            override fun isCommandAvailable(command: Int): Boolean = command in setOf(
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            ) || super.isCommandAvailable(command)

            override fun seekToNext() = next()
            override fun seekToNextMediaItem() = next()
            override fun seekToPrevious() = previous()
            override fun seekToPreviousMediaItem() = previous()
        }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            stateFlow.update { it.copy(playing = isPlaying) }
            if (isPlaying) startTicker() else ticker?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                if (index < queue.lastIndex) next() else stateFlow.update { it.copy(playing = false, position = it.duration) }
            }
            if (playbackState == Player.STATE_READY) stateFlow.update { it.copy(duration = exo.duration.coerceAtLeast(0)) }
        }
    }

    fun play(audios: List<TdApi.Audio>, start: Int) {
        if (audios.isEmpty()) return
        queue = audios
        load(start.coerceIn(0, audios.lastIndex))
    }

    fun toggle() {
        if (stateFlow.value.track == null) return
        if (exo.isPlaying) exo.pause() else {
            if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0)
            exo.play()
        }
    }

    fun next() {
        if (index < queue.lastIndex) load(index + 1)
    }

    fun previous() {
        if (exo.currentPosition > 3000 || index == 0) exo.seekTo(0) else load(index - 1)
    }

    fun seekTo(position: Long) = exo.seekTo(position)

    fun stop() {
        loadJob?.cancel()
        exo.stop()
        exo.clearMediaItems()
        queue = emptyList()
        stateFlow.value = PlayerState()
    }

    private fun load(target: Int) {
        index = target
        val audio = queue[target]
        loadJob?.cancel()
        val cover = audio.albumCoverThumbnail?.file
        stateFlow.value = PlayerState(
            track = Track(audio.audio.id, audio.title.ifBlank { audio.fileName }, audio.performer, cover?.local?.path?.takeIf { cover.local.isDownloadingCompleted }, audio.albumCoverMinithumbnail?.data),
            loading = true,
            duration = audio.duration * 1000L,
            hasNext = target < queue.lastIndex,
            hasPrevious = target > 0,
        )
        loadJob = main.launch {
            val path = runCatching { td.send(TdApi.DownloadFile(audio.audio.id, 32, 0, 0, true)).local.path }.getOrNull()
            val coverPath = cover?.let { runCatching { td.send(TdApi.DownloadFile(it.id, 16, 0, 0, true)).local.path }.getOrNull() }
            if (path.isNullOrEmpty()) {
                stateFlow.update { it.copy(loading = false) }
                return@launch
            }
            stateFlow.update { it.copy(loading = false, track = it.track?.copy(cover = coverPath ?: it.track.cover)) }
            val metadata = MediaMetadata.Builder()
                .setTitle(audio.title.ifBlank { audio.fileName })
                .setArtist(audio.performer)
                .apply { coverPath?.let { setArtworkUri(Uri.fromFile(File(it))) } }
                .build()
            exo.setMediaItem(MediaItem.Builder().setUri(Uri.fromFile(File(path))).setMediaMetadata(metadata).build())
            exo.prepare()
            exo.play()
            connect()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = main.launch {
            while (isActive) {
                stateFlow.update { it.copy(position = exo.currentPosition, duration = exo.duration.takeIf { d -> d > 0 } ?: it.duration) }
                delay(250)
            }
        }
    }

    private fun connect() {
        if (controller != null) return
        controller = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
    }
}
