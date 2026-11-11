package com.pixelgram.android.feature.player

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.music.Lyrics
import com.pixelgram.android.music.LyricsResult
import com.pixelgram.android.music.PlayerState
import com.pixelgram.android.music.RepeatMode
import com.pixelgram.android.music.toInput
import com.pixelgram.android.ui.components.ChatPickerSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

enum class PlayerView { Cover, Lyrics, Queue }

object FullPlayer {
    var open by mutableStateOf(false)
    var view by mutableStateOf(PlayerView.Cover)
}

private val Speeds = listOf(1f, 1.5f, 2f)

@Composable
fun FullPlayerOverlay(graph: AppGraph) {
    val state by graph.music.state.collectAsState()
    LaunchedEffect(state.track == null) { if (state.track == null) FullPlayer.open = false }
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(FullPlayer.open) {
        if (FullPlayer.open) {
            drag.snapTo(0f)
            graph.music.loadProfile(graph.store.snapshot.value.meId)
        }
    }
    var back by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = FullPlayer.open) { progress ->
        try {
            progress.collect { back = it.progress }
            FullPlayer.open = false
        } catch (_: CancellationException) {
        } finally {
            back = 0f
        }
    }
    val scrim by animateFloatAsState(if (FullPlayer.open) ScrimAlpha * (1f - back) else 0f, tween(SheetMs), label = "scrim")
    if (scrim > 0f) Box(Modifier.fillMaxSize().zIndex(9f).background(Color.Black.copy(alpha = scrim)))
    AnimatedVisibility(
        visible = FullPlayer.open,
        enter = slideInVertically(tween(SheetMs, easing = SheetEase)) { it },
        exit = slideOutVertically(tween(SheetCloseMs, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { it },
        modifier = Modifier.zIndex(10f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = drag.value
                    val shrink = 1f - back * 0.1f
                    scaleX = shrink
                    scaleY = shrink
                    shape = RoundedCornerShape(topStart = SheetCorner + (back * 24).dp, topEnd = SheetCorner + (back * 24).dp, bottomStart = (back * 48).dp, bottomEnd = (back * 48).dp)
                    clip = true
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (drag.value > size.height * DismissShare) {
                                    FullPlayer.open = false
                                } else {
                                    drag.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { drag.animateTo(0f) } },
                    ) { change, amount ->
                        change.consume()
                        scope.launch { drag.snapTo((drag.value + amount).coerceAtLeast(0f)) }
                    }
                },
        ) {
            PlayerScreen(graph, state)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerScreen(graph: AppGraph, state: PlayerState) {
    val audio = state.audio
    val covers by graph.covers.found.collectAsState()
    val coverKey = graph.covers.key(audio, large = true)
    val coverPath = coverKey?.let { covers[it] } ?: state.track?.cover
    val base = MaterialTheme.colorScheme
    val palette by produceState<List<Color>?>(null, coverPath) {
        value = coverPath?.let { withContext(Dispatchers.IO) { runCatching { coverPalette(it) }.getOrNull() } }
    }
    val dark = base.surface.luminance() < 0.5f
    val seed = palette?.getOrNull(3) ?: palette?.getOrNull(1)
    val themed = remember(seed, dark) { seed?.let { schemeFromSeed(it.toArgb(), dark) } } ?: base
    MaterialTheme(colorScheme = animatedScheme(themed)) {
        val colors = MaterialTheme.colorScheme
        val music = graph.music
        val profile by music.profile.collectAsState()
        Surface(color = colors.surfaceContainerLow, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                AmbientBackdrop(palette?.take(3), state.playing, Modifier.fillMaxSize())
                androidx.compose.animation.SharedTransitionLayout(Modifier.fillMaxSize()) {
                val shared = this
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { FullPlayer.open = false }) {
                            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.player_collapse))
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                state.source ?: stringResource(R.string.player_music),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (state.index >= 0 && state.queue.size > 1) {
                                AnimatedContent(
                                    targetState = state.index + 1,
                                    transitionSpec = {
                                        val up = state.direction
                                        (slideInVertically { it * up } + fadeIn()) togetherWith (slideOutVertically { -it * up } + fadeOut())
                                    },
                                    label = "track-number",
                                ) { number ->
                                    Text(stringResource(R.string.player_track_of, number, state.queue.size), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                                }
                            }
                        }
                        PlayerMenu(graph, state, music.isSaved(audio, profile))
                    }
                    val enterFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                    val enterScale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
                    val exitFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                    val trackSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    AnimatedContent(
                        targetState = FullPlayer.view,
                        transitionSpec = {
                            (fadeIn(enterFade) + scaleIn(enterScale, initialScale = 0.94f)) togetherWith fadeOut(exitFade)
                        },
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                        label = "player-view",
                    ) { view ->
                        when (view) {
                            PlayerView.Cover -> AnimatedContent(
                                modifier = with(shared) { Modifier.sharedBounds(rememberSharedContentState(CoverKey), this@AnimatedContent, boundsTransform = { _, _ -> spring(dampingRatio = 0.68f, stiffness = 260f) }, resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds) },
                                targetState = audio,
                                contentKey = { it?.audio?.id },
                                transitionSpec = {
                                    val dir = state.direction
                                    (slideInHorizontally(trackSpatial) { it / 3 * dir } + fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith
                                        (slideOutHorizontally(trackSpatial) { -it / 3 * dir } + fadeOut() + scaleOut(targetScale = 0.9f))
                                },
                                contentAlignment = Alignment.Center,
                                label = "cover",
                            ) { shown ->
                                val key = graph.covers.key(shown, large = true)
                                val path = key?.let { covers[it] } ?: if (shown?.audio?.id == state.audio?.audio?.id) state.track?.cover else null
                                BigCover(path, shown?.albumCoverMinithumbnail?.data, state.playing)
                            }
                            PlayerView.Lyrics -> LyricsPane(state, music::seekTo)
                            PlayerView.Queue -> QueuePane(graph, state)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val slot by animateDpAsState(if (FullPlayer.view != PlayerView.Cover) 68.dp else 0.dp, spring(dampingRatio = 0.68f, stiffness = 260f), label = "slot")
                        Box(Modifier.width(slot).height(56.dp)) {
                            androidx.compose.animation.AnimatedVisibility(FullPlayer.view != PlayerView.Cover, enter = fadeIn(), exit = fadeOut()) {
                                SmallCover(
                                    coverPath,
                                    state.track?.minithumb,
                                    56.dp,
                                    with(shared) { Modifier.sharedBounds(rememberSharedContentState(CoverKey), this@AnimatedVisibility, boundsTransform = { _, _ -> spring(dampingRatio = 0.68f, stiffness = 260f) }, resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds) },
                                )
                            }
                        }
                        AnimatedContent(
                            targetState = state.track?.title.orEmpty() to state.track?.performer?.takeIf { it.isNotBlank() },
                            transitionSpec = {
                                val dir = state.direction
                                ContentTransform(
                                    slideInHorizontally { it / 4 * dir } + fadeIn(),
                                    slideOutHorizontally { -it / 4 * dir } + fadeOut(),
                                    sizeTransform = SizeTransform(clip = false),
                                )
                            },
                            modifier = Modifier.weight(1f),
                            label = "title",
                        ) { (title, performer) ->
                            Column {
                                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (performer != null) Text(performer, style = MaterialTheme.typography.titleSmall, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (audio != null) {
                            val saved = music.isSaved(audio, profile)
                            val pop by animateFloatAsState(if (saved) 1f else 0.85f, spring(dampingRatio = 0.4f, stiffness = 600f), label = "heart")
                            IconToggleButton(checked = saved, onCheckedChange = { music.toggleProfile(audio) }) {
                                Icon(
                                    if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    stringResource(if (saved) R.string.player_remove_profile else R.string.player_add_profile),
                                    tint = if (saved) colors.primary else colors.onSurfaceVariant,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = pop
                                        scaleY = pop
                                    },
                                )
                            }
                        }
                    }
                    WavySeekBar(
                        fraction = if (state.duration > 0) state.position.toFloat() / state.duration else 0f,
                        onSeek = { music.seekTo((it * state.duration).toLong()) },
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        playing = state.playing,
                        durationMs = state.duration,
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                        Text(formatTime(state.position), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Text(formatTime(state.duration), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    ) {
                        IconButton(onClick = music::toggleShuffle) {
                            Icon(Icons.Rounded.Shuffle, stringResource(R.string.player_shuffle), tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant)
                        }
                        MorphControls(
                            playing = state.playing,
                            onPrevious = music::previous,
                            onPlay = music::toggle,
                            onNext = music::next,
                            onHold = music::scrub,
                        )
                        IconButton(onClick = music::cycleRepeat) {
                            AnimatedContent(state.repeat, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }, label = "repeat") { mode ->
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Repeat,
                                        stringResource(R.string.player_repeat),
                                        tint = if (mode == RepeatMode.Off) colors.onSurfaceVariant else colors.primary,
                                    )
                                    if (mode == RepeatMode.One) {
                                        Text(
                                            "1",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                            color = colors.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    BottomRow(state, music::setSpeed)
                }
                ScrubPill(state.scrubbing, Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 64.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomRow(state: PlayerState, onSpeed: (Float) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val groupColors = ToggleButtonDefaults.colors(
        containerColor = colors.secondaryContainer.copy(alpha = GroupIdleAlpha),
        contentColor = colors.onSecondaryContainer,
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                Speeds.forEachIndexed { position, speed ->
                    ToggleButton(
                        checked = state.speed == speed,
                        onCheckedChange = { onSpeed(speed) },
                        shapes = when (position) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            Speeds.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = groupColors,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(40.dp),
                    ) {
                        Text(if (speed == 1.5f) "1.5x" else "${speed.toInt()}x", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                listOf(PlayerView.Lyrics to Icons.Rounded.FormatQuote, PlayerView.Queue to Icons.AutoMirrored.Rounded.QueueMusic).forEachIndexed { position, (view, icon) ->
                    ToggleButton(
                        checked = FullPlayer.view == view,
                        onCheckedChange = { FullPlayer.view = if (FullPlayer.view == view) PlayerView.Cover else view },
                        shapes = if (position == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        colors = groupColors,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(40.dp),
                    ) {
                        Icon(icon, stringResource(if (view == PlayerView.Lyrics) R.string.player_lyrics else R.string.player_queue), Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientBackdrop(cover: List<Color>?, playing: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val palette = cover ?: listOf(scheme.primary, scheme.tertiary, scheme.secondary)
    val blobs = palette.map { animateColorAsState(it, tween(1200), label = "blob").value }
    var phase by remember { mutableFloatStateOf(0f) }
    val speed = remember { Animatable(if (playing) 1f else 0f) }
    LaunchedEffect(playing) { speed.animateTo(if (playing) 1f else 0f, tween(StopMs)) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L && speed.value > 0f) {
                    phase = (phase + (now - last) / 1_000_000_000f / (DriftMs / 1000f) * 2f * Math.PI.toFloat() * speed.value) % (2f * Math.PI.toFloat())
                }
                last = now
            }
        }
    }
    val dark = scheme.surface.luminance() < 0.5f
    Box(modifier) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            BlobSpots.forEachIndexed { i, spot ->
                val turn = phase * spot.speed + i * 2.1f
                val center = Offset(w * (spot.x + 0.16f * sin(turn)), h * (spot.y + 0.1f * cos(turn + spot.y * 3f)))
                val radius = w * spot.radius * 1.6f
                val color = blobs[i % blobs.size].copy(alpha = if (dark) 0.6f else 0.45f)
                drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius), radius, center)
            }
        }
        Box(Modifier.fillMaxSize().background(scheme.surfaceContainerLow.copy(alpha = if (dark) 0.35f else 0.45f)))
    }
}

private class BlobSpot(val x: Float, val y: Float, val radius: Float, val speed: Float)

private val BlobSpots = listOf(
    BlobSpot(0.2f, 0.18f, 0.42f, 1f),
    BlobSpot(0.85f, 0.5f, 0.38f, 2f),
    BlobSpot(0.3f, 0.9f, 0.46f, 1f),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SmallCover(path: String?, mini: ByteArray?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val image: Any? = path?.let(::File) ?: mini
    Box(
        modifier.size(size).clip(if (image != null) RoundedCornerShape(16.dp) else MaterialShapes.Cookie12Sided.toShape()).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(size / 2.4f))
        if (image != null) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BigCover(path: String?, mini: ByteArray?, playing: Boolean) {
    val breathe by animateFloatAsState(if (playing) 1f else 0.9f, spring(dampingRatio = 0.6f, stiffness = 220f), label = "breathe")
    val spin = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                spin.animateTo(spin.value + 360f, tween(SpinMs, easing = LinearEasing))
                spin.snapTo(spin.value % 360f)
            }
        } else {
            val rest = kotlin.math.round(spin.value / CookieStep) * CookieStep
            spin.animateTo(rest, spring(dampingRatio = 0.55f, stiffness = 120f))
            spin.snapTo(spin.value % 360f)
        }
    }
    val image: Any? = path?.let(::File) ?: mini
    Box(Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (image != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = breathe
                        scaleY = breathe
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize(0.86f)
                    .graphicsLayer {
                        scaleX = breathe
                        scaleY = breathe
                        rotationZ = spin.value
                    }
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(96.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LyricsPane(state: PlayerState, onSeek: (Long) -> Unit) {
    val audio = state.audio
    val named = audio?.fileName?.substringBeforeLast('.').orEmpty()
    val split = named.split(" - ", " – ", limit = 2).takeIf { it.size == 2 }
    val artist = audio?.performer?.ifBlank { null } ?: split?.first()?.trim().orEmpty()
    val title = audio?.title?.ifBlank { null } ?: split?.last()?.trim() ?: named
    val result by produceState(Lyrics.cached(artist, title) ?: LyricsResult.Loading, artist, title) {
        value = Lyrics.cached(artist, title) ?: LyricsResult.Loading
        if (value == LyricsResult.Loading) value = withContext(Dispatchers.IO) { Lyrics.load(artist, title, audio?.duration ?: 0) }
    }
    val latest by rememberUpdatedState(state)
    val clock = remember { PlayerClock({ latest.position / 1000f }, { latest.playing }, { latest.speed }) }
    when (val lyrics = result) {
        LyricsResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        LyricsResult.NotFound -> LyricsState(Icons.Rounded.FormatQuote, stringResource(R.string.lyrics_none), stringResource(R.string.lyrics_none_sub))
        LyricsResult.Instrumental -> LyricsState(Icons.Rounded.MusicNote, stringResource(R.string.lyrics_instrumental), stringResource(R.string.lyrics_instrumental_sub))
        is LyricsResult.Plain -> Column(
            Modifier.fillMaxSize().edgeFade().verticalScroll(rememberScrollState()).padding(vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            lyrics.lines.forEach { line -> Text(line, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) }
        }
        is LyricsResult.Synced -> SyncedLyrics(lyrics.lines, clock, onSeek)
    }
}

@Composable
private fun QueuePane(graph: AppGraph, state: PlayerState) {
    val list = rememberLazyListState()
    LaunchedEffect(Unit) { if (state.index > 0) list.scrollToItem((state.index - 2).coerceAtLeast(0)) }
    var dragging by remember { mutableStateOf<Int?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val latest by rememberUpdatedState(state.queue)
    val rowPx = with(androidx.compose.ui.platform.LocalDensity.current) { QueueRow.toPx() }
    val covers by graph.covers.found.collectAsState()
    val files = graph.store.snapshot.collectAsState().value.files
    LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(state.queue, key = { _, audio -> audio.audio.id }) { position, audio ->
            val lifted = audio.audio.id == dragging
            val playing = position == state.index
            val rowColor by animateColorAsState(
                when {
                    lifted -> MaterialTheme.colorScheme.surfaceContainerHighest
                    playing -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0f)
                },
                label = "row",
            )
            val elevation by animateFloatAsState(if (lifted) 1f else 0f, label = "lift")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .then(if (lifted) Modifier else Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null))
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (lifted) offset else 0f
                        scaleX = 1f + elevation * 0.03f
                        scaleY = 1f + elevation * 0.03f
                        shadowElevation = elevation * 12f
                        shape = RoundedCornerShape(18.dp)
                        clip = true
                    }
                    .fillMaxWidth()
                    .height(QueueRow)
                    .background(rowColor)
                    .clickable(enabled = dragging == null) { if (position == state.index) graph.music.toggle() else graph.music.playAt(position) }
                    .padding(horizontal = 8.dp),
            ) {
                val thumbId = audio.albumCoverThumbnail?.file?.id ?: 0
                LaunchedEffect(thumbId) { if (thumbId != 0) graph.store.requestFileAsync(thumbId) }
                val key = graph.covers.key(audio)
                val path = files[thumbId] ?: key?.let { covers[it] }
                Box(contentAlignment = Alignment.Center) {
                    SmallCover(path, audio.albumCoverMinithumbnail?.data, 48.dp)
                    if (playing) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                            PlayPauseMorph(state.playing, 24.dp, Color.White)
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(audio.title.ifBlank { audio.fileName }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (audio.performer.isNotBlank()) Text(audio.performer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    Icons.Rounded.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(12.dp)
                        .pointerInput(audio.audio.id) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = audio.audio.id
                                    offset = 0f
                                },
                                onDragEnd = { dragging = null; offset = 0f },
                                onDragCancel = { dragging = null; offset = 0f },
                            ) { change, amount ->
                                change.consume()
                                offset += amount.y
                                val from = latest.indexOfFirst { it.audio.id == audio.audio.id }
                                if (from < 0) return@detectDragGestures
                                val steps = (offset / rowPx).roundToInt()
                                if (steps != 0) {
                                    val target = (from + steps).coerceIn(0, latest.lastIndex)
                                    if (target != from) {
                                        graph.music.reorder(from, target)
                                        offset -= (target - from) * rowPx
                                    }
                                }
                            }
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerMenu(graph: AppGraph, state: PlayerState, saved: Boolean) {
    val audio = state.audio
    if (audio == null) {
        Spacer(Modifier.width(48.dp))
        return
    }
    var open by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var forwarding by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val path = audio.audio.local.path.takeIf { audio.audio.local.isDownloadingCompleted && it.isNotEmpty() }
    val name = audio.fileName.ifBlank { "${audio.performer} - ${audio.title}.mp3" }
    val savedText = stringResource(R.string.player_saved_music)
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more)) }
        DropdownMenuPopup(expanded = open, onDismissRequest = { open = false }) {
            val groups = 3
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, groups)) {
                MenuRow(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.player_forward), 0, 2) {
                    open = false
                    forwarding = true
                }
                MenuRow(Icons.Rounded.Download, stringResource(R.string.player_save_to), 1, 2, trailing = true) {
                    open = false
                    saving = true
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, groups)) {
                MenuRow(
                    if (saved) Icons.Rounded.Delete else Icons.Rounded.Favorite,
                    stringResource(if (saved) R.string.player_remove_profile else R.string.player_add_profile),
                    0,
                    1,
                    error = saved,
                ) {
                    open = false
                    graph.music.toggleProfile(audio)
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(2, groups)) {
                MenuRow(Icons.Rounded.Info, stringResource(R.string.player_details), 0, 1) {
                    open = false
                    details = true
                }
            }
        }
    }
    if (forwarding) {
        ChatPickerSheet(
            title = stringResource(R.string.player_forward),
            onDismiss = { forwarding = false },
            onPick = { chatId ->
                forwarding = false
                scope.launch {
                    runCatching {
                        graph.td.send(TdApi.SendMessage().apply {
                            this.chatId = chatId
                            inputMessageContent = TdApi.InputMessageAudio().apply {
                                this.audio = audio.toInput()
                                caption = TdApi.FormattedText("", emptyArray())
                            }
                        })
                    }
                }
            },
        )
    }
    if (details) TrackDetails(audio, path, name, state.sampleRate) { details = false }
    if (saving) {
        SaveToSheet(
            canMusic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            inProfile = saved,
            onDismiss = { saving = false },
            onMusic = {
                saving = false
                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        path ?: runCatching { graph.td.send(TdApi.DownloadFile(audio.audio.id, 32, 0, 0, true)).local.path }.getOrNull()
                    } ?: return@launch
                    val ok = withContext(Dispatchers.IO) { saveToMusic(context, File(file), name, audio.mimeType) }
                    if (ok) android.widget.Toast.makeText(context, savedText, android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onSaved = {
                saving = false
                scope.launch {
                    val me = graph.store.snapshot.value.meId.takeIf { it != 0L } ?: runCatching { graph.td.send(TdApi.GetMe()).id }.getOrNull() ?: return@launch
                    runCatching {
                        graph.td.send(TdApi.SendMessage().apply {
                            chatId = me
                            inputMessageContent = TdApi.InputMessageAudio().apply {
                                this.audio = audio.toInput()
                                caption = TdApi.FormattedText("", emptyArray())
                            }
                        })
                    }
                }
            },
            onProfile = {
                saving = false
                if (!saved) graph.music.toggleProfile(audio)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, index: Int, count: Int, enabled: Boolean = true, error: Boolean = false, trailing: Boolean = false, onClick: () -> Unit) {
    val tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = tint, modifier = Modifier.weight(1f))
                if (trailing) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        leadingIcon = { Icon(icon, null, tint = tint) },
        enabled = enabled,
        shape = MenuDefaults.itemShape(index, count).shape,
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SaveToSheet(canMusic: Boolean, inProfile: Boolean, onDismiss: () -> Unit, onMusic: () -> Unit, onSaved: () -> Unit, onProfile: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.player_save_to), style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        val rows = listOf(
            Triple(Icons.Rounded.Download, R.string.player_save_music_folder, onMusic.takeIf { canMusic }),
            Triple(Icons.Rounded.Bookmark, R.string.player_save_saved, onSaved),
            Triple(Icons.Rounded.Favorite, R.string.player_add_profile, onProfile.takeIf { !inProfile }),
        )
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
            rows.forEachIndexed { index, (icon, label, action) ->
                androidx.compose.material3.SegmentedListItem(
                    onClick = { action?.invoke() },
                    enabled = action != null,
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, rows.size),
                    colors = com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                    leadingContent = { Icon(icon, null) },
                ) { Text(stringResource(label)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackDetails(audio: TdApi.Audio, path: String?, name: String, sampleRate: Int, onDismiss: () -> Unit) {
    val size = maxOf(audio.audio.size, audio.audio.expectedSize)
    val seconds = audio.duration
    val rows = listOfNotNull(
        stringResource(R.string.player_format) to name.substringAfterLast('.', "").ifBlank { audio.mimeType.substringAfter('/') }.uppercase(),
        if (size > 0 && seconds > 0) stringResource(R.string.player_bitrate) to "${size * 8 / seconds / 1000} kbps" else null,
        sampleRate.takeIf { it > 0 }?.let { stringResource(R.string.player_sample_rate) to "%.1f kHz".format(it / 1000f) },
        if (size > 0) stringResource(R.string.player_file_size) to android.text.format.Formatter.formatShortFileSize(LocalContext.current, size) else null,
        stringResource(R.string.player_duration) to formatTime(seconds * 1000L),
        stringResource(R.string.player_file_name) to name,
        path?.let { stringResource(R.string.player_file_path) to it },
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.player_details), style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        rows.forEach { (label, value) ->
            Column(Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun saveToMusic(context: android.content.Context, source: File, name: String, mime: String): Boolean = runCatching {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, name)
        put(MediaStore.Audio.Media.MIME_TYPE, mime.ifBlank { "audio/mpeg" })
        put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/pixelGram")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
    true
}.getOrDefault(false)

@Composable
private fun animatedScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(SchemeFadeMs)
    @Composable
    fun c(color: Color) = animateColorAsState(color, spec, label = "scheme").value
    return target.copy(
        primary = c(target.primary),
        onPrimary = c(target.onPrimary),
        primaryContainer = c(target.primaryContainer),
        onPrimaryContainer = c(target.onPrimaryContainer),
        secondary = c(target.secondary),
        secondaryContainer = c(target.secondaryContainer),
        onSecondaryContainer = c(target.onSecondaryContainer),
        tertiary = c(target.tertiary),
        surface = c(target.surface),
        onSurface = c(target.onSurface),
        onSurfaceVariant = c(target.onSurfaceVariant),
        surfaceContainerLow = c(target.surfaceContainerLow),
        surfaceContainer = c(target.surfaceContainer),
        surfaceContainerHigh = c(target.surfaceContainerHigh),
        surfaceContainerHighest = c(target.surfaceContainerHighest),
        inverseSurface = c(target.inverseSurface),
        inverseOnSurface = c(target.inverseOnSurface),
    )
}

@Composable
private fun ScrubPill(direction: Int, modifier: Modifier) {
    var shown by remember { mutableStateOf(1) }
    if (direction != 0) shown = direction
    AnimatedVisibility(
        visible = direction != 0,
        enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.6f) + fadeIn(),
        exit = scaleOut(targetScale = 0.8f) + fadeOut(),
        modifier = modifier,
    ) {
        val colors = MaterialTheme.colorScheme
        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = colors.primary, contentColor = colors.onPrimary, shadowElevation = 6.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val backward = shown < 0
                if (backward) Chevrons(true, colors.onPrimary)
                Text(if (backward) "3x" else "4x", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                if (!backward) Chevrons(false, colors.onPrimary)
            }
        }
    }
}

@Composable
private fun Chevrons(backward: Boolean, color: Color) {
    val flow = androidx.compose.animation.core.rememberInfiniteTransition(label = "chevrons")
    val phase by flow.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(ChevronMs, easing = LinearEasing)), label = "phase")
    androidx.compose.foundation.Canvas(Modifier.size(width = 26.dp, height = 18.dp)) {
        val stroke = 2.6.dp.toPx()
        val h = size.height
        val step = size.width / 2.6f
        for (k in 0..1) {
            val order = if (backward) 1 - k else k
            val wave = ((phase - order * 0.35f) % 1f + 1f) % 1f
            val alpha = 0.3f + 0.7f * (1f - kotlin.math.abs(wave * 2f - 1f))
            val x = step * 0.4f + k * step
            val tip = if (backward) x else x + step * 0.8f
            val tail = if (backward) x + step * 0.8f else x
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(tail, h * 0.15f)
                lineTo(tip, h / 2f)
                lineTo(tail, h * 0.85f)
            }
            drawPath(path, color.copy(alpha = alpha), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

private const val ChevronMs = 700
private const val SheetMs = 420
private const val SheetCloseMs = 260
private const val ScrimAlpha = 0.5f
private val SheetCorner = 24.dp
private val SheetEase = androidx.compose.animation.core.CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
private const val CoverKey = "player-cover"
private val QueueRow = 64.dp
private const val GroupIdleAlpha = 0.55f
private const val DismissShare = 0.2f
private const val DriftMs = 42_000
private const val StopMs = 1_200
private const val SpinMs = 24_000
private const val CookieStep = 30f
private const val SchemeFadeMs = 900
