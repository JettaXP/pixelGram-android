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

enum class RepeatMode { Off, All, One }

data class PlayerState(
    val track: Track? = null,
    val audio: TdApi.Audio? = null,
    val playing: Boolean = false,
    val loading: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val speed: Float = 1f,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val queue: List<TdApi.Audio> = emptyList(),
    val index: Int = -1,
    val direction: Int = 1,
    val source: String? = null,
    val sampleRate: Int = 0,
)

class MusicPlayer(private val context: Context, private val td: TdClient) {
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateFlow = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = stateFlow.asStateFlow()
    private var queue: List<TdApi.Audio> = emptyList()
    private var original: List<TdApi.Audio> = emptyList()
    private var index = 0
    private var loadJob: Job? = null
    private var ticker: Job? = null
    private var controller: ListenableFuture<MediaController>? = null
    private val profileFlow = MutableStateFlow<Set<Int>>(emptySet())
    val profile: StateFlow<Set<Int>> = profileFlow.asStateFlow()
    private var meId = 0L

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
                when {
                    stateFlow.value.repeat == RepeatMode.One -> {
                        exo.seekTo(0)
                        exo.play()
                    }
                    index < queue.lastIndex -> load(index + 1, 1)
                    stateFlow.value.repeat == RepeatMode.All && queue.isNotEmpty() -> load(0, 1)
                    else -> stateFlow.update { it.copy(playing = false, position = it.duration) }
                }
            }
            if (playbackState == Player.STATE_READY) {
                val rate = exo.audioFormat?.sampleRate ?: 0
                stateFlow.update { it.copy(duration = exo.duration.coerceAtLeast(0), sampleRate = rate) }
            }
        }
    }

    fun play(audios: List<TdApi.Audio>, start: Int, source: String? = null) {
        if (audios.isEmpty()) return
        original = audios
        val first = audios[start.coerceIn(0, audios.lastIndex)]
        queue = if (stateFlow.value.shuffle) listOf(first) + (audios - first).shuffled() else audios
        stateFlow.update { it.copy(source = source) }
        load(queue.indexOf(first).coerceAtLeast(0), 1)
    }

    fun playAt(position: Int) {
        if (position in queue.indices && position != index) load(position, if (position > index) 1 else -1)
    }

    fun reorder(from: Int, to: Int) {
        if (from !in queue.indices || to !in queue.indices || from == to) return
        val list = queue.toMutableList()
        list.add(to, list.removeAt(from))
        val current = queue.getOrNull(index)
        queue = list
        index = list.indexOf(current).coerceAtLeast(0)
        publishQueue()
    }

    fun setSpeed(speed: Float) {
        exo.setPlaybackSpeed(speed)
        stateFlow.update { it.copy(speed = speed) }
    }

    fun toggleShuffle() {
        val on = !stateFlow.value.shuffle
        val current = queue.getOrNull(index)
        queue = when {
            current == null -> queue
            on -> listOf(current) + (queue - current).shuffled()
            else -> original
        }
        index = queue.indexOf(current).coerceAtLeast(0)
        stateFlow.update { it.copy(shuffle = on) }
        publishQueue()
    }

    fun cycleRepeat() {
        stateFlow.update {
            it.copy(repeat = when (it.repeat) {
                RepeatMode.Off -> RepeatMode.All
                RepeatMode.All -> RepeatMode.One
                RepeatMode.One -> RepeatMode.Off
            })
        }
        publishQueue()
    }

    fun loadProfile(me: Long) {
        if (me == 0L) return
        meId = me
        main.launch {
            val list = runCatching { td.send(TdApi.GetUserProfileAudios(me, 0, 100)).audios.toList() }.getOrNull() ?: return@launch
            profileFlow.value = list.map { it.audio.id }.toSet()
        }
    }

    fun toggleProfile(audio: TdApi.Audio) {
        val id = audio.audio.id
        val saved = id in profileFlow.value
        profileFlow.update { if (saved) it - id else it + id }
        main.launch {
            runCatching { td.send(if (saved) TdApi.RemoveProfileAudio(id) else TdApi.AddProfileAudio(audio.toInput())) }
                .onFailure { profileFlow.update { if (saved) it + id else it - id } }
        }
    }

    private fun publishQueue() {
        val repeatAll = stateFlow.value.repeat == RepeatMode.All
        stateFlow.update {
            it.copy(
                queue = queue,
                index = index,
                hasNext = index < queue.lastIndex || (repeatAll && queue.size > 1),
                hasPrevious = index > 0,
            )
        }
    }

    fun toggle() {
        if (stateFlow.value.track == null) return
        if (exo.isPlaying) exo.pause() else {
            if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0)
            exo.play()
        }
    }

    fun next() {
        when {
            index < queue.lastIndex -> load(index + 1, 1)
            stateFlow.value.repeat == RepeatMode.All && queue.size > 1 -> load(0, 1)
        }
    }

    fun previous() {
        if (exo.currentPosition > 3000 || index == 0) exo.seekTo(0) else load(index - 1, -1)
    }

    fun seekTo(position: Long) {
        exo.seekTo(position)
        stateFlow.update { it.copy(position = position) }
    }

    private var scrubJob: Job? = null

    fun scrub(direction: Int) {
        scrubJob?.cancel()
        when {
            direction > 0 -> exo.setPlaybackSpeed(ScrubForward)
            direction < 0 -> scrubJob = main.launch {
                while (isActive) {
                    val target = (exo.currentPosition - ScrubBackStepMs).coerceAtLeast(0)
                    exo.seekTo(target)
                    stateFlow.update { it.copy(position = target) }
                    delay(ScrubTickMs)
                }
            }
            else -> exo.setPlaybackSpeed(stateFlow.value.speed)
        }
    }

    fun stop() {
        loadJob?.cancel()
        exo.stop()
        exo.clearMediaItems()
        queue = emptyList()
        original = emptyList()
        val keep = stateFlow.value
        stateFlow.value = PlayerState(speed = keep.speed, shuffle = keep.shuffle, repeat = keep.repeat)
    }

    private fun load(target: Int, direction: Int = 1) {
        index = target
        val audio = queue[target]
        loadJob?.cancel()
        val cover = audio.albumCoverThumbnail?.file
        stateFlow.update {
            it.copy(
                track = Track(audio.audio.id, audio.title.ifBlank { audio.fileName }, audio.performer, cover?.local?.path?.takeIf { cover.local.isDownloadingCompleted }, audio.albumCoverMinithumbnail?.data),
                audio = audio,
                loading = true,
                playing = false,
                position = 0,
                duration = audio.duration * 1000L,
                direction = direction,
            )
        }
        publishQueue()
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
            exo.setPlaybackSpeed(stateFlow.value.speed)
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

private const val ScrubForward = 4f
private const val ScrubBackStepMs = 300L
private const val ScrubTickMs = 100L

fun TdApi.Audio.toInput(): TdApi.InputAudio = TdApi.InputAudio().also {
    it.audio = TdApi.InputFileRemote(audio.remote.id)
    it.duration = duration
    it.title = title
    it.performer = performer
}
