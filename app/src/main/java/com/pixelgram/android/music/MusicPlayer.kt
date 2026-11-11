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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
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
    val scrubbing: Int = 0,
)

class MusicPlayer(private val context: Context, private val td: TdClient) {
    var covers: CoverArt? = null
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateFlow = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = stateFlow.asStateFlow()
    private var queue: List<TdApi.Audio> = emptyList()
    private var original: List<TdApi.Audio> = emptyList()
    private var index = 0
    private var loadJob: Job? = null
    private var ticker: Job? = null
    private var controller: ListenableFuture<MediaController>? = null
    private val profileFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    val profile: StateFlow<Map<String, Int>> = profileFlow.asStateFlow()
    private var meId = 0L
    val cast = CastLink(context)
    private var currentPath: String? = null
    private var currentCover: String? = null
    private var castEnded = false
    private var castLoadedAt = 0L

    init {
        cast.onConnected = { main.launch { handOff() } }
        cast.onDisconnected = { position -> main.launch { takeBack(position) } }
        cast.onStatus = { main.launch { castStatus() } }
    }

    private fun mime(audio: TdApi.Audio): String = audio.mimeType.ifBlank { "audio/mpeg" }

    private fun castCurrent(position: Long, autoplay: Boolean) {
        val audio = stateFlow.value.audio ?: return
        val path = currentPath ?: return
        castEnded = false
        castLoadedAt = android.os.SystemClock.elapsedRealtime()
        cast.load(File(path), mime(audio), audio.title.ifBlank { audio.fileName }, audio.performer, currentCover?.let(::File), position, autoplay)
        cast.setSpeed(stateFlow.value.speed)
    }

    private fun handOff() {
        if (currentPath == null) return
        val position = exo.currentPosition
        val playing = stateFlow.value.playing
        fadeJob?.cancel()
        exo.pause()
        exo.volume = 1f
        castCurrent(position, playing)
        stateFlow.update { it.copy(playing = playing) }
        startTicker()
    }

    private fun takeBack(position: Long) {
        val path = currentPath ?: return
        val audio = stateFlow.value.audio ?: return
        if (exo.mediaItemCount == 0) {
            exo.setMediaItem(MediaItem.Builder().setUri(Uri.fromFile(File(path))).setMediaMetadata(MediaMetadata.Builder().setTitle(audio.title.ifBlank { audio.fileName }).setArtist(audio.performer).build()).build())
            exo.prepare()
        }
        exo.seekTo(position)
        exo.pause()
        ticker?.cancel()
        stateFlow.update { it.copy(playing = false, position = position) }
    }

    private fun castStatus() {
        if (!cast.active) return
        val settled = android.os.SystemClock.elapsedRealtime() - castLoadedAt > CastSettleMs
        if (settled && cast.finished && !castEnded && stateFlow.value.track != null) {
            castEnded = true
            when {
                stateFlow.value.repeat == RepeatMode.One -> castCurrent(0, true)
                index < queue.lastIndex -> load(index + 1, 1)
                stateFlow.value.repeat == RepeatMode.All && queue.isNotEmpty() -> load(0, 1)
                else -> stateFlow.update { it.copy(playing = false, position = it.duration) }
            }
            return
        }
        if (settled) stateFlow.update { it.copy(playing = cast.playing || it.loading, position = cast.position) }
    }

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
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (cast.active) return
            stateFlow.update { it.copy(playing = playWhenReady && exo.playbackState != Player.STATE_ENDED) }
            if (playWhenReady) startTicker() else ticker?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (cast.active) return
            if (playbackState == Player.STATE_ENDED) {
                when {
                    stateFlow.value.repeat == RepeatMode.One -> {
                        exo.seekTo(0)
                        exo.play()
                    }
                    index < queue.lastIndex -> load(index + 1, 1)
                    stateFlow.value.repeat == RepeatMode.All && queue.isNotEmpty() -> load(0, 1)
                    else -> {
                        ticker?.cancel()
                        stateFlow.update { it.copy(playing = false, position = it.duration) }
                    }
                }
            }
            if (playbackState == Player.STATE_READY && exo.playWhenReady) {
                stateFlow.update { it.copy(playing = true) }
                startTicker()
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
        if (cast.active) cast.setSpeed(speed)
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
        main.launch {
            val id = me.takeIf { it != 0L } ?: runCatching { td.send(TdApi.GetMe()).id }.getOrNull() ?: return@launch
            meId = id
            val list = runCatching { td.send(TdApi.GetUserProfileAudios(id, 0, 100)).audios.toList() }.getOrNull() ?: return@launch
            profileFlow.value = list.flatMap { audio -> audio.profileKeys().map { it to audio.audio.id } }.toMap()
        }
    }

    fun isSaved(audio: TdApi.Audio?, profile: Map<String, Int> = profileFlow.value): Boolean =
        audio != null && audio.profileKeys().any { it in profile }

    fun toggleProfile(audio: TdApi.Audio) {
        val keys = audio.profileKeys()
        val before = profileFlow.value
        val savedId = keys.firstNotNullOfOrNull { before[it] }
        profileFlow.value = if (savedId != null) before - keys.toSet() else before + keys.map { it to audio.audio.id }
        main.launch {
            runCatching { td.send(if (savedId != null) TdApi.RemoveProfileAudio(savedId) else TdApi.AddProfileAudio(audio.toInput())) }
                .onFailure { profileFlow.value = before }
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

    private var fadeJob: Job? = null

    fun toggle() {
        if (stateFlow.value.track == null) return
        fadeJob?.cancel()
        if (cast.active) {
            val playing = !stateFlow.value.playing
            if (castEnded) castCurrent(0, true) else cast.toggle()
            stateFlow.update { it.copy(playing = playing) }
            return
        }
        if (exo.playWhenReady && exo.playbackState != Player.STATE_ENDED) {
            stateFlow.update { it.copy(playing = false) }
            fadeJob = main.launch {
                fade(exo.volume, 0f)
                exo.pause()
                exo.volume = 1f
            }
        } else {
            if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0)
            exo.volume = 0f
            exo.play()
            fadeJob = main.launch { fade(0f, 1f) }
        }
    }

    private suspend fun fade(from: Float, to: Float) {
        val steps = FadeMs / FadeTickMs
        for (i in 1..steps) {
            exo.volume = from + (to - from) * i / steps
            delay(FadeTickMs.toLong())
        }
        exo.volume = to
    }


    fun next() {
        when {
            index < queue.lastIndex -> load(index + 1, 1)
            stateFlow.value.repeat == RepeatMode.All && queue.size > 1 -> load(0, 1)
        }
    }

    fun previous() {
        if (stateFlow.value.position > 3000 || index == 0) seekTo(0) else load(index - 1, -1)
    }

    fun seekTo(position: Long) {
        if (cast.active) cast.seek(position) else exo.seekTo(position)
        stateFlow.update { it.copy(position = position) }
    }

    private var scrubJob: Job? = null

    fun scrub(direction: Int) {
        scrubJob?.cancel()
        stateFlow.update { it.copy(scrubbing = direction) }
        if (direction == 0) return
        scrubJob = main.launch {
            while (isActive) {
                val duration = (if (cast.active) cast.duration else exo.duration).takeIf { it > 0 } ?: stateFlow.value.duration
                val step = if (direction > 0) ScrubForwardStepMs else -ScrubBackStepMs
                val target = (stateFlow.value.position + step).coerceIn(0, duration.coerceAtLeast(0))
                if (cast.active) cast.seek(target) else exo.seekTo(target)
                stateFlow.update { it.copy(position = target) }
                delay(ScrubTickMs)
            }
        }
    }


    fun stop() {
        fadeJob?.cancel()
        scrubJob?.cancel()
        loadJob?.cancel()
        if (cast.active) cast.client?.stop()
        currentPath = null
        currentCover = null
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
        scrub(0)
        ticker?.cancel()
        exo.stop()
        exo.clearMediaItems()
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
            val coverJob = main.async {
                cover?.let { runCatching { td.send(TdApi.DownloadFile(it.id, 32, 0, 0, true)).local.path }.getOrNull() }
                    ?.takeIf { it.isNotEmpty() }
                    ?.also { found -> stateFlow.update { s -> if (s.audio?.audio?.id == audio.audio.id) s.copy(track = s.track?.copy(cover = found)) else s } }
            }
            val external = covers?.let { art ->
                art.key(audio, large = true)?.let { key ->
                    kotlinx.coroutines.withTimeoutOrNull(ArtworkWaitMs) { art.found.first { key in it }[key] }
                }
            }
            val path = runCatching { td.send(TdApi.DownloadFile(audio.audio.id, 32, 0, 0, true)).local.path }.getOrNull()
            val coverPath = external ?: kotlinx.coroutines.withTimeoutOrNull(ArtworkWaitMs) { coverJob.await() }
            if (path.isNullOrEmpty()) {
                stateFlow.update { it.copy(loading = false) }
                return@launch
            }
            stateFlow.update { it.copy(loading = false, track = it.track?.copy(cover = coverPath ?: it.track.cover)) }
            currentPath = path
            currentCover = coverPath ?: stateFlow.value.track?.cover
            if (cast.active) {
                castCurrent(0, true)
                stateFlow.update { it.copy(playing = true) }
                startTicker()
                return@launch
            }
            val metadata = MediaMetadata.Builder()
                .setTitle(audio.title.ifBlank { audio.fileName })
                .setArtist(audio.performer)
                .apply { coverPath?.let { setArtworkUri(Uri.fromFile(File(it))) } }
                .build()
            exo.setMediaItem(MediaItem.Builder().setUri(Uri.fromFile(File(path))).setMediaMetadata(metadata).build())
            exo.prepare()
            exo.setPlaybackSpeed(stateFlow.value.speed)
            exo.volume = 1f
            exo.play()
            stateFlow.update { it.copy(playing = true) }
            startTicker()
            connect()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = main.launch {
            while (isActive) {
                if (cast.active) {
                    castStatus()
                    stateFlow.update { it.copy(duration = cast.duration.takeIf { d -> d > 0 } ?: it.duration) }
                } else {
                    stateFlow.update { it.copy(position = exo.currentPosition, duration = exo.duration.takeIf { d -> d > 0 } ?: it.duration) }
                }
                delay(250)
            }
        }
    }

    private fun connect() {
        if (controller != null) return
        controller = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
    }
}

private const val ScrubForwardStepMs = 400L
private const val ScrubBackStepMs = 300L
private const val ScrubTickMs = 100L
private const val FadeMs = 150
private const val ArtworkWaitMs = 3_000L
private const val FadeTickMs = 16
private const val CastSettleMs = 2_000L

fun TdApi.Audio.profileKeys(): List<String> = listOfNotNull(
    audio.remote?.uniqueId?.takeIf { it.isNotBlank() }?.let { "u:$it" },
    "m:${performer.trim().lowercase()}|${title.trim().lowercase()}|$duration".takeIf { title.isNotBlank() },
)

fun TdApi.Audio.toInput(): TdApi.InputAudio = TdApi.InputAudio().also {
    it.audio = TdApi.InputFileRemote(audio.remote.id)
    it.duration = duration
    it.title = title
    it.performer = performer
}
