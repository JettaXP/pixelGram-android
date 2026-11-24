package com.pixelgram.android.feature.chat

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.compose.PlayerSurface
import com.pixelgram.android.AppGraph
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.td.TdClient
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.MenuEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.RandomAccessFile

class VideoActions(
    val onReply: (UiMessage) -> Unit,
    val onForward: (UiMessage) -> Unit,
    val onDelete: (UiMessage) -> Unit,
    val onShowInChat: (UiMessage) -> Unit,
)

object VideoViewer {
    var message by mutableStateOf<UiMessage?>(null)
}

@UnstableApi
private class TdStreamSource(private val td: TdClient, private val fileId: Int) : BaseDataSource(true) {
    private var uri: Uri? = null
    private var file: RandomAccessFile? = null
    private var path: String = ""
    private var position = 0L
    private var remaining = 0L
    private var opened = false

    private fun info(): TdApi.File = runBlocking { td.send(TdApi.GetFile(fileId)) }

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        val current = info()
        val size = if (current.size > 0) current.size else current.expectedSize
        position = dataSpec.position
        remaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else (size - position).coerceAtLeast(0L)
        if (!current.local.isDownloadingCompleted) runBlocking { runCatching { td.send(TdApi.DownloadFile(fileId, StreamPriority, position, 0, false)) } }
        opened = true
        transferStarted(dataSpec)
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        var available = 0L
        var waited = 0
        while (available <= 0L) {
            available = runBlocking { runCatching { td.send(TdApi.GetFileDownloadedPrefixSize(fileId, position)).size }.getOrDefault(0L) }
            if (available > 0L) break
            if (waited % 40 == 0) runBlocking { runCatching { td.send(TdApi.DownloadFile(fileId, StreamPriority, position, 0, false)) } }
            Thread.sleep(25)
            waited++
            if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException()
        }
        if (file == null || path.isEmpty()) {
            path = info().local.path
            file = RandomAccessFile(path, "r")
        }
        val chunk = minOf(length.toLong(), remaining, available).toInt()
        val raf = file ?: return C.RESULT_END_OF_INPUT
        raf.seek(position)
        val read = raf.read(buffer, offset, chunk)
        if (read <= 0) return C.RESULT_END_OF_INPUT
        position += read
        remaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        runCatching { file?.close() }
        file = null
        path = ""
        uri = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun streamingPlayer(context: Context, graph: AppGraph, fileId: Int): ExoPlayer {
    val factory = DataSource.Factory { TdStreamSource(graph.td, fileId) }
    return ExoPlayer.Builder(context).build().apply {
        setMediaSource(ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri("tdfile://$fileId")))
        prepare()
        playWhenReady = true
    }
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoViewerHost(title: (UiMessage) -> String, actions: VideoActions) {
    val message = VideoViewer.message ?: return
    val video = (message.content as? TdApi.MessageVideo)?.video ?: return
    val graph = LocalGraph.current
    val context = LocalContext.current
    val close = { VideoViewer.message = null }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val player = remember(video.video.id) { streamingPlayer(context, graph, video.video.id) }
        DisposableEffect(player) { onDispose { player.release() } }
        var playing by remember { mutableStateOf(true) }
        var buffering by remember { mutableStateOf(true) }
        var position by remember { mutableLongStateOf(0L) }
        var length by remember { mutableLongStateOf(video.duration * 1000L) }
        var speed by remember { mutableFloatStateOf(1f) }
        var loop by remember { mutableStateOf(false) }
        var chrome by remember { mutableStateOf(true) }
        var scrubbing by remember { mutableStateOf<Float?>(null) }
        var menu by remember { mutableStateOf(false) }
        var settings by remember { mutableStateOf(false) }
        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying || player.playWhenReady && player.playbackState == Player.STATE_BUFFERING
                }
                override fun onPlaybackStateChanged(state: Int) {
                    buffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_ENDED) playing = false
                    if (player.duration > 0) length = player.duration
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
        LaunchedEffect(player) {
            while (true) {
                if (scrubbing == null) position = player.currentPosition
                delay(ProgressTickMs)
            }
        }
        LaunchedEffect(chrome, playing) {
            if (chrome && playing) {
                delay(ChromeHideMs)
                if (!menu && !settings) chrome = false
            }
        }
        val dc by produceState(0, video.video.id) {
            value = com.pixelgram.android.feature.profile.DataCenters.fromRemoteId(video.video.remote?.id)
        }
        val toggle = {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.playWhenReady = !player.playWhenReady
            playing = player.playWhenReady
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { chrome = !chrome },
                        onDoubleTap = { point ->
                            val step = if (point.x < size.width / 2) -SeekStepMs else SeekStepMs
                            player.seekTo((player.currentPosition + step).coerceIn(0L, player.duration.coerceAtLeast(0L)))
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val ratio = if (video.width > 0 && video.height > 0) video.width.toFloat() / video.height else 16f / 9f
            PlayerSurface(player, Modifier.fillMaxWidth().aspectRatio(ratio), surfaceType = androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW)
            if (buffering) LoadingIndicator(color = Color.White)
            AnimatedVisibility(chrome, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    ) {
                        IconButton(onClick = close) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = Color.White) }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(title(message), color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(viewerDate(context, message.date) + if (dc > 0) ", DC$dc" else "", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        Box {
                            IconButton(onClick = { settings = true }) { Icon(Icons.Rounded.Settings, stringResource(R.string.viewer_settings), tint = Color.White) }
                            DropdownMenu(settings, { settings = false }, shape = RoundedCornerShape(20.dp)) {
                                Text(stringResource(R.string.viewer_speed), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Speeds.forEach { value ->
                                        val chosen = value == speed
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(if (chosen) 14.dp else 8.dp))
                                                .background(if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                                                .clickable {
                                                    speed = value
                                                    player.playbackParameters = PlaybackParameters(value)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                        ) {
                                            Text(speedLabel(value), style = MaterialTheme.typography.labelLarge, color = if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.viewer_loop)) },
                                    leadingIcon = { Icon(Icons.Rounded.Repeat, null, tint = if (loop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                                    trailingIcon = { com.pixelgram.android.ui.components.PixelSwitch(checked = loop, onCheckedChange = { value -> loop = value; player.repeatMode = if (value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }) },
                                    onClick = {
                                        loop = !loop
                                        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                    },
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_menu), tint = Color.White) }
                            val saved = stringResource(R.string.viewer_saved)
                            val waiting = stringResource(R.string.viewer_wait)
                            com.pixelgram.android.ui.components.ExpressiveMenu(
                                menu,
                                { menu = false },
                                listOf(
                                    listOf(
                                        MenuEntry(Icons.Rounded.Download, stringResource(R.string.viewer_save)) {
                                            menu = false
                                            withVideo(graph, video.video.id, context, waiting) { path -> android.widget.Toast.makeText(context, if (saveVideo(context, File(path))) saved else waiting, android.widget.Toast.LENGTH_SHORT).show() }
                                        },
                                        MenuEntry(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.viewer_open_in)) {
                                            menu = false
                                            withVideo(graph, video.video.id, context, waiting) { path -> openWith(context, path, Intent.ACTION_VIEW) }
                                        },
                                        MenuEntry(Icons.Rounded.Share, stringResource(R.string.action_share)) {
                                            menu = false
                                            withVideo(graph, video.video.id, context, waiting) { path -> openWith(context, path, Intent.ACTION_SEND) }
                                        },
                                    ),
                                    listOf(
                                        MenuEntry(Icons.Rounded.Visibility, stringResource(R.string.viewer_show_in_chat)) { menu = false; close(); actions.onShowInChat(message) },
                                        MenuEntry(Icons.AutoMirrored.Rounded.Reply, stringResource(R.string.action_reply)) { menu = false; close(); actions.onReply(message) },
                                        MenuEntry(Icons.AutoMirrored.Rounded.Forward, stringResource(R.string.action_forward)) { menu = false; close(); actions.onForward(message) },
                                    ),
                                    listOf(
                                        MenuEntry(Icons.Rounded.Delete, stringResource(R.string.action_delete), error = true) { menu = false; close(); actions.onDelete(message) },
                                    ),
                                ),
                            )
                        }
                    }
                    Box(
                        Modifier.align(Alignment.Center).size(72.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).clickable { toggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        if (message.text.isNotBlank()) Text(message.text, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val fraction = (scrubbing ?: if (length > 0) position.toFloat() / length else 0f).coerceIn(0f, 1f)
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                LinearWavyProgressIndicator(
                                    progress = { fraction },
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f),
                                    amplitude = { if (playing && scrubbing == null) 1f else 0f },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Slider(
                                    value = fraction,
                                    onValueChange = { scrubbing = it },
                                    onValueChangeFinished = {
                                        scrubbing?.let { player.seekTo((it * length).toLong()) }
                                        scrubbing = null
                                    },
                                    colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            val shown = scrubbing?.let { (it * length).toLong() } ?: position
                            Text(
                                clock(shown) + " / " + clock(length),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun withVideo(graph: AppGraph, fileId: Int, context: Context, waiting: String, action: (String) -> Unit) {
    graph.scope.launchMain {
        val file = runCatching { graph.td.send(TdApi.GetFile(fileId)) }.getOrNull()
        if (file?.local?.isDownloadingCompleted == true) action(file.local.path)
        else {
            runCatching { graph.td.send(TdApi.DownloadFile(fileId, StreamPriority, 0, 0, false)) }
            android.widget.Toast.makeText(context, waiting, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchMain(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) =
    this.launch(kotlinx.coroutines.Dispatchers.Main, block = block)

private fun openWith(context: Context, path: String, action: String) {
    runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", File(path))
        val intent = Intent(action).apply {
            if (action == Intent.ACTION_SEND) {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            } else setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun saveVideo(context: Context, file: File): Boolean = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/pixelGram")
    }
    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
    true
}.getOrDefault(false)

private fun viewerDate(context: Context, seconds: Int): String {
    val date = java.util.Date(seconds * 1000L)
    val clockText = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(date)
    val day = java.util.Calendar.getInstance().apply { setTime(date) }
    val now = java.util.Calendar.getInstance()
    val sameDay = day.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) && day.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
    return if (sameDay) context.getString(R.string.viewer_today_at, clockText)
    else context.getString(R.string.viewer_date_at, java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(date), clockText)
}

private fun clock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(total / 60, total % 60)
}

private fun speedLabel(value: Float): String = if (value == value.toInt().toFloat()) "${value.toInt()}x" else "${value}x"

private val Speeds = listOf(0.5f, 1f, 1.5f, 2f)
private const val StreamPriority = 32
private const val ProgressTickMs = 100L
private const val ChromeHideMs = 3000L
private const val SeekStepMs = 10_000L
