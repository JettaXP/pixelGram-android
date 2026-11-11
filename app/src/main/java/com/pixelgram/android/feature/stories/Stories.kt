package com.pixelgram.android.feature.stories

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import coil.compose.AsyncImage
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.td.StoryOwner
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.util.chatListTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File

object StoryViewer {
    var chatId by mutableStateOf<Long?>(null)
    var queue by mutableStateOf<List<Long>>(emptyList())

    fun open(chatId: Long, queue: List<Long>) {
        this.queue = queue
        this.chatId = chatId
    }
}

object StoryComposer {
    var open by mutableStateOf(false)
}

@Composable
fun StoriesRow(graph: AppGraph, owners: List<StoryOwner>, modifier: Modifier = Modifier) {
    val snapshot by graph.store.snapshot.collectAsState()
    val me = snapshot.meId
    val mine = owners.firstOrNull { it.chatId == me }
    val others = owners.filter { it.chatId != me }
    val order = listOfNotNull(mine?.chatId) + others.map { it.chatId }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        item(key = "me") {
            val user = snapshot.users[me]
            StoryBubble(
                title = stringResource(R.string.stories_mine),
                seed = me,
                path = user?.photoFileId?.let { snapshot.files[it] },
                ring = mine?.let { if (it.unread) RingState.Unread else RingState.Read } ?: RingState.None,
                add = true,
                onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) },
                onClick = { if (mine != null) StoryViewer.open(me, order) else StoryComposer.open = true },
                onAdd = { StoryComposer.open = true },
            )
        }
        items(others, key = { it.chatId }) { owner ->
            val chat = graph.store.chat(owner.chatId)
            val photo = chat?.photo?.small?.id ?: 0
            StoryBubble(
                title = chat?.title.orEmpty(),
                seed = owner.chatId,
                path = snapshot.files[photo],
                ring = if (owner.unread) RingState.Unread else RingState.Read,
                add = false,
                onNeedFile = { graph.store.requestFileAsync(photo) },
                onClick = { StoryViewer.open(owner.chatId, order) },
                onAdd = {},
            )
        }
    }
}

private enum class RingState { None, Unread, Read }

@Composable
private fun StoryBubble(
    title: String,
    seed: Long,
    path: String?,
    ring: RingState,
    add: Boolean,
    onNeedFile: () -> Unit,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(RowItemWidth).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Box {
            val brush = when (ring) {
                RingState.Unread -> Brush.sweepGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary))
                RingState.Read -> Brush.linearGradient(listOf(scheme.outlineVariant, scheme.outlineVariant))
                RingState.None -> null
            }
            Box(
                Modifier
                    .size(RingSize)
                    .then(if (brush != null) Modifier.border(if (ring == RingState.Unread) 2.5.dp else 1.5.dp, brush, com.pixelgram.android.ui.theme.LocalAvatarShape.current) else Modifier)
                    .padding(4.dp),
            ) {
                Avatar(title = title, seed = seed, path = path, minithumb = null, size = RingSize - 8.dp, onNeedFile = onNeedFile)
            }
            if (add) {
                Surface(
                    onClick = onAdd,
                    shape = CircleShape,
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    border = androidx.compose.foundation.BorderStroke(2.dp, scheme.surface),
                    modifier = Modifier.align(Alignment.BottomEnd).size(22.dp),
                ) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.stories_add), Modifier.padding(2.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun StoryOverlays(graph: AppGraph) {
    val chatId = StoryViewer.chatId
    AnimatedVisibility(
        visible = chatId != null,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = Modifier.zIndex(20f),
    ) {
        var shown by remember { mutableStateOf(chatId ?: 0L) }
        if (chatId != null) shown = chatId
        StoryViewerScreen(graph, shown)
    }
    if (StoryComposer.open) StoryComposerSheet(graph) { StoryComposer.open = false }
}

private class LoadedStory(val story: TdApi.Story, val path: String?, val video: Boolean, val durationMs: Long)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StoryViewerScreen(graph: AppGraph, chatId: Long) {
    val owner = graph.store.snapshot.collectAsState().value.stories.firstOrNull { it.chatId == chatId }
    var ids by remember(chatId) { mutableStateOf(owner?.storyIds.orEmpty()) }
    var index by remember(chatId) { mutableIntStateOf(ids.indexOfFirst { it > (owner?.maxRead ?: 0) }.coerceAtLeast(0)) }
    LaunchedEffect(owner?.storyIds) {
        val fresh = owner?.storyIds ?: return@LaunchedEffect
        if (ids.isEmpty()) index = fresh.indexOfFirst { it > owner.maxRead }.coerceAtLeast(0)
        else ids.getOrNull(index)?.let { current -> fresh.indexOf(current).takeIf { it >= 0 }?.let { index = it } }
        ids = fresh
    }
    var paused by remember { mutableStateOf(false) }
    val progress = remember(chatId, index) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    BackHandler { StoryViewer.chatId = null }
    val nextChat: (Int) -> Unit = { direction ->
        val queue = StoryViewer.queue
        val at = queue.indexOf(chatId)
        val target = queue.getOrNull(at + direction)
        StoryViewer.chatId = target
    }
    val go: (Int) -> Unit = { direction ->
        val target = index + direction
        when {
            target < 0 -> nextChat(-1)
            target > ids.lastIndex -> nextChat(1)
            else -> index = target
        }
    }
    val storyId = ids.getOrNull(index)
    val loaded by produceState<LoadedStory?>(null, chatId, storyId) {
        value = null
        if (storyId == null) return@produceState
        value = withContext(Dispatchers.IO) { load(graph, chatId, storyId) }
    }
    DisposableEffect(chatId, storyId) {
        if (storyId != null) {
            graph.td.fire(TdApi.OpenStory(chatId, storyId))
            graph.store.markStoriesRead(chatId, storyId)
        }
        onDispose { if (storyId != null) graph.td.fire(TdApi.CloseStory(chatId, storyId)) }
    }
    val story = loaded
    LaunchedEffect(story, paused) {
        if (story == null || paused) return@LaunchedEffect
        val left = ((1f - progress.value) * story.durationMs).toInt()
        progress.animateTo(1f, tween(left, easing = LinearEasing))
        go(1)
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = drag.value
                val scale = 1f - (drag.value / size.height * 0.2f).coerceIn(0f, 0.2f)
                scaleX = scale
                scaleY = scale
            }
            .background(Color.Black)
            .pointerInput(chatId) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (drag.value > size.height * 0.18f) StoryViewer.chatId = null else drag.animateTo(0f)
                        }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch { drag.snapTo((drag.value + amount).coerceAtLeast(0f)) }
                }
            }
            .pointerInput(chatId, index) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val start = System.currentTimeMillis()
                    paused = true
                    val up = waitForUpOrCancellation()
                    paused = false
                    if (up != null && System.currentTimeMillis() - start < HoldMs) {
                        go(if (down.position.x < size.width / 3f) -1 else 1)
                    }
                }
            },
    ) {
        when {
            story == null -> LoadingIndicator(Modifier.align(Alignment.Center))
            story.video && story.path != null -> StoryVideo(story.path, paused)
            story.path != null -> AsyncImage(model = File(story.path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth().height(3.dp)) {
                ids.forEachIndexed { i, _ ->
                    val fill = when {
                        i < index -> 1f
                        i == index -> progress.value
                        else -> 0f
                    }
                    Box(Modifier.weight(1f).fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(fill).fillMaxSize().background(Color.White))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                val chat = graph.store.chat(chatId)
                val files = graph.store.snapshot.collectAsState().value.files
                val photo = chat?.photo?.small?.id ?: 0
                Avatar(title = chat?.title.orEmpty(), seed = chatId, path = files[photo], minithumb = null, size = 36.dp, onNeedFile = { graph.store.requestFileAsync(photo) })
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(chat?.title.orEmpty(), color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    story?.let { Text(chatListTime(it.story.date), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall) }
                }
                IconButton(onClick = { StoryViewer.chatId = null }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close), tint = Color.White) }
            }
        }
        val caption = story?.story?.caption?.text.orEmpty()
        if (caption.isNotBlank()) {
            Text(
                caption,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun StoryVideo(path: String, paused: Boolean) {
    val context = LocalContext.current
    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    LaunchedEffect(paused) { player.playWhenReady = !paused }
    DisposableEffect(player) { onDispose { player.release() } }
    PlayerSurface(player, Modifier.fillMaxSize())
}

private suspend fun load(graph: AppGraph, chatId: Long, storyId: Int): LoadedStory? = runCatching {
    val story = graph.td.send(TdApi.GetStory(chatId, storyId, false))
    when (val content = story.content) {
        is TdApi.StoryContentPhoto -> {
            val size = content.photo.sizes.maxByOrNull { it.width * it.height } ?: return null
            val file = graph.td.send(TdApi.DownloadFile(size.photo.id, 32, 0, 0, true))
            LoadedStory(story, file.local.path.takeIf { it.isNotEmpty() }, false, PhotoMs)
        }
        is TdApi.StoryContentVideo -> {
            val file = graph.td.send(TdApi.DownloadFile(content.video.video.id, 32, 0, 0, true))
            LoadedStory(story, file.local.path.takeIf { it.isNotEmpty() }, true, (content.video.duration * 1000).toLong().coerceAtLeast(1000))
        }
        else -> LoadedStory(story, null, false, PhotoMs)
    }
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StoryComposerSheet(graph: AppGraph, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<File?>(null) }
    var video by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var posting by remember { mutableStateOf(false) }
    val cameraFile = remember { File(File(context.cacheDir, "stories").apply { mkdirs() }, "camera.jpg") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && cameraFile.length() > 0) {
            picked = cameraFile
            video = false
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val type = context.contentResolver.getType(uri).orEmpty()
                val file = withContext(Dispatchers.IO) {
                    runCatching {
                        val target = File(File(context.cacheDir, "stories").apply { mkdirs() }, "pick." + if (type.startsWith("video")) "mp4" else "jpg")
                        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                        target
                    }.getOrNull()
                }
                picked = file
                video = type.startsWith("video")
            }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.stories_new), style = MaterialTheme.typography.titleLargeEmphasized)
            val file = picked
            if (file == null) {
                ListItem(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable {
                        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", cameraFile))
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    leadingContent = { Icon(Icons.Rounded.PhotoCamera, null) },
                    headlineContent = { Text(stringResource(R.string.stories_camera)) },
                )
                ListItem(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    leadingContent = { Icon(Icons.Rounded.PhotoLibrary, null) },
                    headlineContent = { Text(stringResource(R.string.stories_gallery)) },
                )
            } else {
                Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    if (video) StoryVideo(file.absolutePath, paused = false) else AsyncImage(model = file, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text(stringResource(R.string.stories_caption)) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        posting = true
                        scope.launch {
                            val ok = post(graph, file, video, caption)
                            posting = false
                            if (ok) onDismiss()
                        }
                    },
                    enabled = !posting,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (posting) LoadingIndicator(Modifier.size(24.dp)) else Text(stringResource(R.string.stories_post))
                }
            }
        }
    }
}

private suspend fun post(graph: AppGraph, file: File, video: Boolean, caption: String): Boolean = runCatching {
    val me = graph.store.snapshot.value.meId
    val stored = withContext(Dispatchers.IO) {
        File(file.parentFile, "post-${System.currentTimeMillis()}.${file.extension}").also { file.copyTo(it, overwrite = true) }
    }
    val content: TdApi.InputStoryContent = if (video) {
        val seconds = withContext(Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(stored.absolutePath)
                (retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000.0
            } finally {
                retriever.release()
            }
        }
        TdApi.InputStoryContentVideo(TdApi.InputFileLocal(stored.absolutePath), IntArray(0), seconds, 0.0, false)
    } else {
        TdApi.InputStoryContentPhoto(TdApi.InputFileLocal(stored.absolutePath), IntArray(0))
    }
    graph.td.send(TdApi.PostStory().apply {
        chatId = me
        this.content = content
        this.caption = TdApi.FormattedText(caption, emptyArray())
        privacySettings = TdApi.StoryPrivacySettingsEveryone(LongArray(0))
        albumIds = IntArray(0)
        activePeriod = ActivePeriod
        isPostedToChatPage = true
    })
    true
}.getOrDefault(false)

private val RingSize = 64.dp
private val RowItemWidth = 72.dp
private const val HoldMs = 250L
private const val PhotoMs = 5_000L
private const val ActivePeriod = 86_400
