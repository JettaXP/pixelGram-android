package com.pixelgram.android.feature.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import coil.compose.AsyncImage
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi
import java.io.File

internal fun isRichContent(content: TdApi.MessageContent): Boolean = when (content) {
    is TdApi.MessageVideo, is TdApi.MessageVoiceNote, is TdApi.MessageVideoNote, is TdApi.MessageSticker,
    is TdApi.MessageAnimatedEmoji, is TdApi.MessageLocation, is TdApi.MessageVenue, is TdApi.MessageContact, is TdApi.MessagePoll,
    is TdApi.MessageDice, is TdApi.MessageGame, is TdApi.MessageInvoice, is TdApi.MessageStory,
    is TdApi.MessageCall, is TdApi.MessageGift, is TdApi.MessageUpgradedGift, is TdApi.MessageChecklist,
    is TdApi.MessageGiveaway, is TdApi.MessagePaidMedia, is TdApi.MessageExpiredPhoto, is TdApi.MessageExpiredVideo,
    is TdApi.MessageUnsupported -> true
    else -> false
}

private fun localPath(file: TdApi.File?, files: Map<Int, String>): String? =
    file?.let { files[it.id] ?: it.local.path.takeIf { _ -> it.local.isDownloadingCompleted && it.local.path.isNotEmpty() } }

internal fun duration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

@Composable
internal fun RichContent(
    message: UiMessage,
    files: Map<Int, String>,
    onNeedFile: (Int) -> Unit,
    content: Color,
    onOpenUser: (Long) -> Unit,
    onVote: (IntArray) -> Unit,
    onPay: () -> Unit = {},
) {
    when (val c = message.content) {
        is TdApi.MessageVideo -> VideoBubble(c.video, files, onNeedFile)
        is TdApi.MessageVoiceNote -> VoiceRow(c.voiceNote, files, onNeedFile, content)
        is TdApi.MessageVideoNote -> VideoNoteView(c.videoNote, files, onNeedFile)
        is TdApi.MessageSticker -> StickerContent(c.sticker, files, onNeedFile)
        is TdApi.MessageAnimatedEmoji -> AnimatedEmojiContent(message, c, files, onNeedFile)
        is TdApi.MessageLocation -> PlaceCard(c.location, null, null, content)
        is TdApi.MessageVenue -> PlaceCard(c.venue.location, c.venue.title, c.venue.address, content)
        is TdApi.MessageContact -> ContactCard(c.contact, content, onOpenUser)
        is TdApi.MessagePoll -> PollView(c.poll, content, onVote)
        is TdApi.MessageDice -> DiceView(c.emoji, c.value, content)
        is TdApi.MessageGame -> InfoCard(Icons.Rounded.SportsEsports, c.game.title, c.game.description, content)
        is TdApi.MessageInvoice -> InfoCard(Icons.Rounded.ShoppingBag, c.productInfo.title, (if (c.currency == "XTR") "⭐ ${c.totalAmount}" else price(c.totalAmount, c.currency)) + " · " + c.productInfo.description.text, content, onClick = if (c.receiptMessageId == 0L) onPay else null)
        is TdApi.MessageStory -> StoryCard(c, content)
        is TdApi.MessageCall -> CallRow(c, message.outgoing, content)
        is TdApi.MessageGift -> GiftCard(c.gift.sticker, stringResource(R.string.content_gift), stringResource(R.string.content_gift_stars, c.gift.starCount), c.text.text, files, onNeedFile, content)
        is TdApi.MessageUpgradedGift -> GiftCard(null, c.gift.title, "#${c.gift.number}", c.text.text, files, onNeedFile, content)
        is TdApi.MessageChecklist -> ChecklistView(c.list, content)
        is TdApi.MessageGiveaway -> InfoCard(Icons.Rounded.CardGiftcard, stringResource(R.string.content_giveaway), stringResource(R.string.content_giveaway_winners, c.winnerCount), content)
        is TdApi.MessagePaidMedia -> InfoCard(Icons.Rounded.ShoppingBag, stringResource(R.string.content_paid_media), stringResource(R.string.content_gift_stars, c.starCount), content, onClick = onPay)
        is TdApi.MessageExpiredPhoto -> InfoCard(Icons.Rounded.HelpOutline, stringResource(R.string.content_expired_photo), null, content)
        is TdApi.MessageExpiredVideo -> InfoCard(Icons.Rounded.HelpOutline, stringResource(R.string.content_expired_video), null, content)
        else -> InfoCard(Icons.Rounded.HelpOutline, stringResource(R.string.content_unsupported), stringResource(R.string.content_unsupported_sub), content)
    }
}

private fun price(amount: Long, currency: String): String = runCatching {
    val format = java.text.NumberFormat.getCurrencyInstance()
    val cur = java.util.Currency.getInstance(currency)
    format.currency = cur
    format.format(amount / Math.pow(10.0, cur.defaultFractionDigits.toDouble()))
}.getOrDefault("$amount $currency")

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, content: Color, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(4.dp)
            .widthIn(min = 200.dp, max = 300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(content.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Column(Modifier.weight(1f, fill = false)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.7f), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoBubble(video: TdApi.Video, files: Map<Int, String>, onNeedFile: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val thumb = localPath(video.thumbnail?.file, files)
    video.thumbnail?.file?.takeIf { thumb == null }?.let { LaunchedEffect(it.id) { onNeedFile(it.id) } }
    val ratio = if (video.width > 0 && video.height > 0) (video.width.toFloat() / video.height).coerceIn(0.6f, 1.9f) else 1.6f
    Box(
        Modifier
            .padding(2.dp)
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable {
                onNeedFile(video.video.id)
                open = true
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(model = thumb?.let(::File) ?: video.minithumbnail?.data, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Text(duration(video.duration), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
    if (open) {
        val path = localPath(video.video, files)
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                if (path == null) {
                    LoadingIndicator(color = Color.White)
                } else {
                    VideoPlayer(path, muted = false, loop = false, controls = true, modifier = Modifier.fillMaxWidth().aspectRatio(ratio))
                }
                androidx.compose.material3.IconButton(onClick = { open = false }, modifier = Modifier.align(Alignment.TopStart).padding(top = 32.dp, start = 8.dp)) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.action_close), tint = Color.White)
                }
            }
        }
    }
}

@Composable
internal fun VideoPlayer(path: String, muted: Boolean, loop: Boolean, controls: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (muted) 0f else 1f
            prepare()
            playWhenReady = true
        }
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    DisposableEffect(player) { onDispose { player.release() } }
    var playing by remember { mutableStateOf(true) }
    Box(modifier.then(if (controls) Modifier.clickable { playing = !playing; player.playWhenReady = playing; if (player.playbackState == Player.STATE_ENDED) player.seekTo(0) } else Modifier)) {
        PlayerSurface(player, Modifier.fillMaxSize())
        if (controls && !playing) {
            Box(Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
    }
}

object VoicePlayer {
    private var player: ExoPlayer? = null
    private val state = MutableStateFlow<Pair<Int, Boolean>>(0 to false)
    val playing = state.asStateFlow()

    fun toggle(context: Context, fileId: Int, path: String) {
        val current = state.value
        val exo = player ?: ExoPlayer.Builder(context.applicationContext).build().also { created ->
            player = created
            created.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    state.value = state.value.first to isPlaying
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) state.value = 0 to false
                }
            })
        }
        if (current.first == fileId) {
            if (exo.isPlaying) exo.pause() else exo.play()
            return
        }
        exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
        exo.prepare()
        exo.play()
        state.value = fileId to true
    }

    fun progress(fileId: Int): Float {
        val exo = player ?: return 0f
        if (state.value.first != fileId || exo.duration <= 0) return 0f
        return (exo.currentPosition.toFloat() / exo.duration).coerceIn(0f, 1f)
    }
}

private fun waveform(data: ByteArray, bars: Int): List<Float> {
    if (data.isEmpty()) return List(bars) { 0.3f }
    val count = data.size * 8 / 5
    val values = IntArray(count) { i ->
        val bit = i * 5
        val byte = bit / 8
        val shift = bit % 8
        val raw = ((data[byte].toInt() and 0xFF) or ((data.getOrNull(byte + 1)?.toInt() ?: 0) and 0xFF shl 8)) shr shift
        raw and 31
    }
    return List(bars) { b ->
        val from = b * count / bars
        val to = ((b + 1) * count / bars).coerceAtLeast(from + 1).coerceAtMost(count)
        ((from until to).maxOfOrNull { values[it] } ?: 0) / 31f
    }
}

@Composable
private fun VoiceRow(voice: TdApi.VoiceNote, files: Map<Int, String>, onNeedFile: (Int) -> Unit, content: Color) {
    val context = LocalContext.current
    val path = localPath(voice.voice, files)
    val state by VoicePlayer.playing.collectAsState()
    val active = state.first == voice.voice.id
    val playing = active && state.second
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(playing) {
        while (playing) {
            progress = VoicePlayer.progress(voice.voice.id)
            delay(50)
        }
        if (!active) progress = 0f
    }
    var wanted by remember { mutableStateOf(false) }
    LaunchedEffect(path, wanted) { if (wanted && path != null) { wanted = false; VoicePlayer.toggle(context, voice.voice.id, path) } }
    val bars = remember(voice.waveform) { waveform(voice.waveform, VoiceBars) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).widthIn(min = 220.dp),
    ) {
        androidx.compose.material3.FilledIconButton(
            onClick = {
                if (path != null) VoicePlayer.toggle(context, voice.voice.id, path) else {
                    onNeedFile(voice.voice.id)
                    wanted = true
                }
            },
            modifier = Modifier.size(44.dp),
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null) }
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(28.dp)) {
                bars.forEachIndexed { i, v ->
                    val lit = (i + 0.5f) / bars.size <= progress
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight(0.15f + 0.85f * v)
                            .clip(CircleShape)
                            .background(if (lit) MaterialTheme.colorScheme.primary else content.copy(alpha = 0.35f)),
                    )
                }
            }
            Text(duration(voice.duration), style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoNoteView(note: TdApi.VideoNote, files: Map<Int, String>, onNeedFile: (Int) -> Unit) {
    val path = localPath(note.video, files)
    if (path == null) LaunchedEffect(note.video.id) { onNeedFile(note.video.id) }
    val thumb = localPath(note.thumbnail?.file, files)
    var sound by remember { mutableStateOf(false) }
    Box(
        Modifier.padding(4.dp).size(VideoNoteSize).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { sound = !sound },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(model = thumb?.let(::File) ?: note.minithumbnail?.data, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (path != null) VideoPlayer(path, muted = !sound, loop = true, controls = false, modifier = Modifier.fillMaxSize()) else LoadingIndicator()
        if (!sound) {
            Box(Modifier.align(Alignment.BottomCenter).padding(10.dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.VolumeOff, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 18.dp)) {
            Text(duration(note.duration), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun StickerContent(sticker: TdApi.Sticker, files: Map<Int, String>, onNeedFile: (Int) -> Unit) {
    StickerView(sticker, files, onNeedFile, Modifier.padding(4.dp).size(StickerSize))
}

@Composable
internal fun StickerView(sticker: TdApi.Sticker, files: Map<Int, String>, onNeedFile: (Int) -> Unit, modifier: Modifier) {
    val path = localPath(sticker.sticker, files)
    if (path == null) LaunchedEffect(sticker.sticker.id) { onNeedFile(sticker.sticker.id) }
    val thumb = localPath(sticker.thumbnail?.file, files)
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            path != null && sticker.format is TdApi.StickerFormatTgs -> {
                val json by androidx.compose.runtime.produceState<String?>(null, path) { value = com.pixelgram.android.ui.components.EmojiStickers.tgs(path) }
                val text = json
                if (text != null) {
                    val composition by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.JsonString(text), cacheKey = "tgs:$path")
                    com.airbnb.lottie.compose.LottieAnimation(composition, modifier = Modifier.fillMaxSize(), iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever)
                }
            }
            path != null && sticker.format is TdApi.StickerFormatWebm -> VideoPlayer(path, muted = true, loop = true, controls = false, modifier = Modifier.fillMaxSize())
            path != null -> AsyncImage(model = File(path), contentDescription = sticker.emoji, modifier = Modifier.fillMaxSize())
            thumb != null -> AsyncImage(model = File(thumb), contentDescription = sticker.emoji, modifier = Modifier.fillMaxSize())
            else -> Text(sticker.emoji, fontSize = 64.sp)
        }
    }
}

@Composable
private fun PlaceCard(location: TdApi.Location, title: String?, address: String?, content: Color) {
    val context = LocalContext.current
    val coords = "%.5f, %.5f".format(location.latitude, location.longitude)
    InfoCard(Icons.Rounded.LocationOn, title ?: stringResource(R.string.content_location), address?.takeIf { it.isNotBlank() } ?: coords, content) {
        val label = Uri.encode(title ?: coords)
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}($label)")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

@Composable
private fun ContactCard(contact: TdApi.Contact, content: Color, onOpenUser: (Long) -> Unit) {
    val name = listOf(contact.firstName, contact.lastName).filter { it.isNotBlank() }.joinToString(" ")
    InfoCard(Icons.Rounded.Person, name.ifBlank { contact.phoneNumber }, contact.phoneNumber.takeIf { it.isNotBlank() }?.let { if (it.startsWith("+")) it else "+$it" }, content) {
        if (contact.userId != 0L) onOpenUser(contact.userId)
    }
}

@Composable
private fun PollView(poll: TdApi.Poll, content: Color, onVote: (IntArray) -> Unit) {
    val voted = poll.options.any { it.isChosen }
    val results = voted || poll.isClosed
    var picked by remember(poll.id) { mutableStateOf(setOf<Int>()) }
    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp).widthIn(min = 240.dp, max = 300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(poll.question.text, style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(
                when {
                    poll.isClosed -> R.string.content_poll_closed
                    poll.type is TdApi.PollTypeQuiz -> R.string.content_quiz
                    poll.isAnonymous -> R.string.content_poll_anonymous
                    else -> R.string.content_poll_public
                },
            ),
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.7f),
        )
        poll.options.forEachIndexed { index, option ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !results) {
                        if (poll.allowsMultipleAnswers) picked = if (index in picked) picked - index else picked + index
                        else onVote(intArrayOf(index))
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (results) {
                        Text("${option.votePercentage}%", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(40.dp))
                    } else {
                        Icon(if (index in picked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Text(option.text.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (option.isChosen) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                if (results) {
                    LinearProgressIndicator(
                        progress = { option.votePercentage / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = content.copy(alpha = 0.12f),
                    )
                }
            }
        }
        if (poll.allowsMultipleAnswers && !results) {
            androidx.compose.material3.TextButton(onClick = { onVote(picked.sorted().toIntArray()) }, enabled = picked.isNotEmpty()) { Text(stringResource(R.string.content_poll_vote)) }
        }
        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.content_poll_votes, poll.totalVoterCount, poll.totalVoterCount), style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.7f))
    }
}

@Composable
private fun DiceView(emoji: String, value: Int, content: Color) {
    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 64.sp)
        if (value > 0) Text(value.toString(), style = MaterialTheme.typography.titleLarge, color = content)
    }
}

@Composable
private fun StoryCard(story: TdApi.MessageStory, content: Color) {
    InfoCard(Icons.Rounded.AutoStories, stringResource(R.string.content_story), stringResource(R.string.content_story_open), content) {
        com.pixelgram.android.feature.stories.StoryViewer.open(story.storyPosterChatId, listOf(story.storyPosterChatId))
    }
}

@Composable
private fun CallRow(call: TdApi.MessageCall, outgoing: Boolean, content: Color) {
    val missed = call.discardReason is TdApi.CallDiscardReasonMissed || call.discardReason is TdApi.CallDiscardReasonDeclined
    val title = stringResource(
        when {
            missed && outgoing -> R.string.content_call_cancelled
            missed -> R.string.content_call_missed
            outgoing -> if (call.isVideo) R.string.content_video_call_out else R.string.content_call_out
            else -> if (call.isVideo) R.string.content_video_call_in else R.string.content_call_in
        },
    )
    InfoCard(
        if (missed) Icons.Rounded.CallMissed else if (call.isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
        title,
        call.duration.takeIf { it > 0 }?.let(::duration),
        content,
    )
}

@Composable
private fun GiftCard(sticker: TdApi.Sticker?, title: String, subtitle: String, text: String, files: Map<Int, String>, onNeedFile: (Int) -> Unit, content: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp).width(220.dp).clip(RoundedCornerShape(18.dp)).background(content.copy(alpha = 0.08f)).padding(12.dp),
    ) {
        if (sticker != null) StickerContent(sticker, files, onNeedFile) else Icon(Icons.Rounded.CardGiftcard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.7f))
        if (text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChecklistView(list: TdApi.Checklist, content: Color) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp).widthIn(min = 220.dp, max = 300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(list.title.text, style = MaterialTheme.typography.titleSmall)
        list.tasks.forEach { task ->
            val done = task.completedBy != null
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (done) MaterialTheme.colorScheme.primary else content.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Text(task.text.text, style = MaterialTheme.typography.bodyMedium, color = if (done) content.copy(alpha = 0.7f) else content)
            }
        }
        val done = list.tasks.count { it.completedBy != null }
        Text(stringResource(R.string.content_checklist_done, done, list.tasks.size), style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.7f))
    }
}

internal fun serviceText(context: Context, content: TdApi.MessageContent, sender: String, name: (Long) -> String): String? {
    fun s(id: Int, vararg args: Any) = context.getString(id, *args)
    return when (content) {
        is TdApi.MessageChatAddMembers -> {
            val ids = content.memberUserIds.toList()
            if (ids.size == 1 && name(ids[0]) == sender) s(R.string.service_joined, sender) else s(R.string.service_added, sender, ids.joinToString(", ") { name(it) })
        }
        is TdApi.MessageChatJoinByLink -> s(R.string.service_joined_link, sender)
        is TdApi.MessageChatJoinByRequest -> s(R.string.service_joined_request, sender)
        is TdApi.MessageChatDeleteMember -> if (name(content.userId) == sender) s(R.string.service_left, sender) else s(R.string.service_removed, sender, name(content.userId))
        is TdApi.MessageChatChangeTitle -> s(R.string.service_title, sender, content.title)
        is TdApi.MessageChatChangePhoto -> s(R.string.service_photo, sender)
        is TdApi.MessageChatDeletePhoto -> s(R.string.service_photo_removed, sender)
        is TdApi.MessagePinMessage -> s(R.string.service_pinned, sender)
        is TdApi.MessageBasicGroupChatCreate -> s(R.string.service_group_created, sender, content.title)
        is TdApi.MessageSupergroupChatCreate -> s(R.string.service_group_created, sender, content.title)
        is TdApi.MessageChatUpgradeTo, is TdApi.MessageChatUpgradeFrom -> s(R.string.service_upgraded)
        is TdApi.MessageScreenshotTaken -> s(R.string.service_screenshot, sender)
        is TdApi.MessageContactRegistered -> s(R.string.service_registered, sender)
        is TdApi.MessageChatSetMessageAutoDeleteTime -> if (content.messageAutoDeleteTime == 0) s(R.string.service_autodelete_off, sender) else s(R.string.service_autodelete, sender, autoDelete(context, content.messageAutoDeleteTime))
        is TdApi.MessageForumTopicCreated -> s(R.string.service_topic_created, sender, content.name)
        is TdApi.MessageForumTopicEdited -> s(R.string.service_topic_edited, sender)
        is TdApi.MessageCustomServiceAction -> content.text
        is TdApi.MessageVideoChatStarted -> s(R.string.service_video_chat_started, sender)
        is TdApi.MessageVideoChatEnded -> s(R.string.service_video_chat_ended, duration(content.duration))
        is TdApi.MessageVideoChatScheduled -> s(R.string.service_video_chat_scheduled, sender)
        is TdApi.MessageInviteVideoChatParticipants -> s(R.string.service_video_chat_invite, sender, content.userIds.joinToString(", ") { name(it) })
        is TdApi.MessageGameScore -> s(R.string.service_game_score, sender, content.score)
        is TdApi.MessagePaymentSuccessful -> s(R.string.service_payment, price(content.totalAmount, content.currency))
        is TdApi.MessageGiftedPremium -> s(R.string.service_premium_gift, name(content.gifterUserId), content.monthCount)
        is TdApi.MessageChatBoost -> s(R.string.service_boost, sender, content.boostCount)
        is TdApi.MessageChatSetTheme -> s(R.string.service_theme, sender)
        is TdApi.MessageChatSetBackground -> s(R.string.service_background, sender)
        is TdApi.MessageBotWriteAccessAllowed -> s(R.string.service_bot_access)
        is TdApi.MessageWebAppDataSent -> s(R.string.service_webapp_data)
        is TdApi.MessageProximityAlertTriggered -> s(R.string.service_proximity, content.distance)
        else -> null
    }
}

private fun autoDelete(context: Context, seconds: Int): String = when {
    seconds % 86400 == 0 -> context.resources.getQuantityString(R.plurals.time_days, seconds / 86400, seconds / 86400)
    seconds % 3600 == 0 -> context.resources.getQuantityString(R.plurals.time_hours, seconds / 3600, seconds / 3600)
    else -> context.resources.getQuantityString(R.plurals.time_minutes, (seconds / 60).coerceAtLeast(1), (seconds / 60).coerceAtLeast(1))
}

private const val VoiceBars = 32
private val VideoNoteSize = 220.dp
private val StickerSize = 180.dp

@Composable
private fun AnimatedEmojiContent(message: UiMessage, content: TdApi.MessageAnimatedEmoji, files: Map<Int, String>, onNeedFile: (Int) -> Unit) {
    val sticker = content.animatedEmoji?.sticker
    val size = EmojiSize
    if (sticker == null) {
        Text(content.emoji, fontSize = 64.sp, modifier = Modifier.padding(8.dp))
        return
    }
    val key = message.chatId to message.id
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var runs by remember(key) { mutableStateOf(1) }
    var effect by remember(key) { mutableStateOf<TdApi.Sticker?>(null) }
    var effectRuns by remember(key) { mutableStateOf(0) }
    val remote by com.pixelgram.android.td.EmojiTaps.remote.collectAsState()
    val incoming = remote[key]
    LaunchedEffect(incoming) {
        if (incoming == null) return@LaunchedEffect
        com.pixelgram.android.td.EmojiTaps.consume(key)
        runs++
        if (effect == null) {
            effect = incoming
            effectRuns++
        }
    }
    Box(
        Modifier.padding(4.dp).size(size).clickable(interactionSource = null, indication = null) {
            runs++
            if (effect == null && message.chatId != 0L) {
                scope.launch {
                    val result = com.pixelgram.android.td.EmojiTaps.click(message.chatId, message.id)
                    if (result != null && effect == null) {
                        effect = result
                        effectRuns++
                    }
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        OneShotSticker(sticker, files, onNeedFile, runs, Modifier.fillMaxSize(), fallback = content.emoji) {}
        effect?.let { shown ->
            val shift = with(androidx.compose.ui.platform.LocalDensity.current) { (size * EffectShift).roundToPx() }
            androidx.compose.ui.window.Popup(
                alignment = Alignment.Center,
                offset = androidx.compose.ui.unit.IntOffset(if (message.outgoing) -shift else shift, 0),
            ) {
                OneShotSticker(
                    shown,
                    files,
                    onNeedFile,
                    effectRuns,
                    Modifier.size(size * EffectScale).graphicsLayer { if (!message.outgoing) scaleX = -1f },
                    fallback = null,
                ) { effect = null }
            }
        }
    }
}

@Composable
private fun OneShotSticker(
    sticker: TdApi.Sticker,
    files: Map<Int, String>,
    onNeedFile: (Int) -> Unit,
    runs: Int,
    modifier: Modifier,
    fallback: String?,
    onEnd: () -> Unit,
) {
    val path = localPath(sticker.sticker, files)
    if (path == null) LaunchedEffect(sticker.sticker.id) { onNeedFile(sticker.sticker.id) }
    if (path == null || sticker.format !is TdApi.StickerFormatTgs) {
        if (path != null) AsyncImage(model = File(path), contentDescription = sticker.emoji, modifier = modifier)
        else if (fallback != null) Box(modifier, contentAlignment = Alignment.Center) { Text(fallback, fontSize = 64.sp) }
        if (fallback == null && path != null) LaunchedEffect(runs) { delay(1500); onEnd() }
        return
    }
    val json by androidx.compose.runtime.produceState<String?>(null, path) { value = com.pixelgram.android.ui.components.EmojiStickers.tgs(path) }
    val text = json ?: return
    val composition by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.JsonString(text), cacheKey = "tgs:$path")
    val animatable = com.airbnb.lottie.compose.rememberLottieAnimatable()
    LaunchedEffect(composition, runs) {
        val loaded = composition ?: return@LaunchedEffect
        if (runs > 0) animatable.animate(loaded, iterations = 1)
        onEnd()
    }
    com.airbnb.lottie.compose.LottieAnimation(composition, { animatable.progress }, modifier = modifier)
}

private val EmojiSize = 128.dp
private const val EffectScale = 3f
private const val EffectShift = 1f
