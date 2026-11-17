package com.pixelgram.android.feature.chat

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Star
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.CustomEmojiView
import com.pixelgram.android.ui.components.Icon
import org.drinkless.tdlib.TdApi
import java.io.File

enum class PanelTab { Emoji, Gifs, Stickers }

private const val RecentId = -1L
private const val FavoritesId = -2L
private const val StandardId = 0L

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EmojiStickerPanel(
    onEmoji: (String) -> Unit,
    onCustomEmoji: (Long, String) -> Unit,
    onSticker: (TdApi.Sticker) -> Unit,
    onGif: (TdApi.Animation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(PanelTab.Emoji) }
    LaunchedEffect(PanelRequest.tab) {
        PanelRequest.tab?.let { tab = it }
        PanelRequest.tab = null
    }
    Box(modifier) {
        androidx.compose.animation.AnimatedContent(
            tab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val spatial = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.9f, stiffness = 1400f)
                (androidx.compose.animation.slideInHorizontally(spatial) { if (forward) it / 6 else -it / 6 } + androidx.compose.animation.fadeIn()) togetherWith
                    (androidx.compose.animation.slideOutHorizontally(spatial) { if (forward) -it / 6 else it / 6 } + androidx.compose.animation.fadeOut())
            },
            label = "panel-tab",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                PanelTab.Emoji -> EmojiTab(onEmoji, onCustomEmoji)
                PanelTab.Stickers -> StickersTab(onSticker)
                PanelTab.Gifs -> GifsTab(onGif)
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 4.dp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        ) {
            Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PanelTab.entries.forEach { t ->
                    val active = t == tab
                    Box(
                        Modifier.clip(CircleShape)
                            .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                            .clickable { tab = t }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(
                                when (t) {
                                    PanelTab.Emoji -> R.string.panel_emoji
                                    PanelTab.Gifs -> R.string.panel_gifs
                                    PanelTab.Stickers -> R.string.panel_stickers
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetHeader(sets: List<TdApi.StickerSetInfo>, selected: Long, leading: List<Pair<Long, androidx.compose.ui.graphics.vector.ImageVector>>, onSelect: (Long) -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    LazyRow(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(leading) { _, (id, icon) ->
            HeaderCell(id == selected, { onSelect(id) }) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
        }
        itemsIndexed(sets, key = { _, s -> s.id }) { _, set ->
            HeaderCell(set.id == selected, { onSelect(set.id) }) {
                val cover = set.covers?.firstOrNull()
                if (cover != null) StickerView(cover, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.size(28.dp), video = false)
                else Text(set.title.take(1), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HeaderCell(active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
internal fun EmojiTab(onEmoji: (String) -> Unit, onCustomEmoji: (Long, String) -> Unit) {
    val graph = LocalGraph.current
    val sets by produceState(emptyList<TdApi.StickerSetInfo>()) {
        value = runCatching { graph.td.send(TdApi.GetInstalledStickerSets(TdApi.StickerTypeCustomEmoji())).sets.toList() }.getOrDefault(emptyList())
    }
    var selected by rememberSaveable { mutableStateOf(StandardId) }
    Column(Modifier.fillMaxSize()) {
        SetHeader(sets, selected, listOf(StandardId to Icons.Rounded.EmojiEmotions)) { selected = it }
        if (selected == StandardId) {
            val dark = com.pixelgram.android.ui.theme.LocalDarkTheme.current
            val emojiStyle = com.pixelgram.android.ui.theme.LocalAppearance.current.emojiStyle
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val emojiFace = remember(emojiStyle) { com.pixelgram.android.ui.theme.AppFonts.emojiTypeface(ctx, emojiStyle) }
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    val themed = android.view.ContextThemeWrapper(context, if (dark) com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar else com.google.android.material.R.style.Theme_Material3_Light_NoActionBar)
                    androidx.emoji2.emojipicker.EmojiPickerView(themed).apply {
                        emojiGridColumns = 8
                        setOnEmojiPickedListener { item -> onEmoji(item.emoji) }
                        if (emojiFace != null) {
                            com.pixelgram.android.ui.theme.AppFonts.applyToPicker(this, emojiFace)
                            viewTreeObserver.addOnPreDrawListener {
                                com.pixelgram.android.ui.theme.AppFonts.applyToPicker(this, emojiFace)
                                true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val stickers = rememberSet(selected)
            LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 64.dp)) {
                items(stickers, key = { it.id }) { sticker ->
                    val id = (sticker.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId ?: 0L
                    Box(
                        Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).combinedClickable(onLongClick = { if (id != 0L) EmojiPreview.request = id to sticker.emoji }) { if (id != 0L) onCustomEmoji(id, sticker.emoji) }.padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) { CustomEmojiView(id, sticker.emoji, Modifier.fillMaxSize()) }
                }
            }
        }
    }
}

@Composable
private fun rememberSet(id: Long): List<TdApi.Sticker> {
    val graph = LocalGraph.current
    val stickers by produceState(emptyList<TdApi.Sticker>(), id) {
        value = runCatching {
            if (id == RecentId) graph.td.send(TdApi.GetRecentStickers(false)).stickers.toList()
            else graph.td.send(TdApi.GetStickerSet(id)).stickers.toList()
        }.getOrDefault(emptyList())
    }
    return stickers
}

private class StickerSection(val id: Long, val title: String, val stickers: List<TdApi.Sticker>)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StickersTab(onSticker: (TdApi.Sticker) -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val recentTitle = androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.stickers_recent)
    val favoritesTitle = androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.stickers_favorites)
    val sets by produceState(emptyList<TdApi.StickerSetInfo>()) {
        value = runCatching { graph.td.send(TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular())).sets.toList() }.getOrDefault(emptyList())
    }
    val sections by produceState(emptyList<StickerSection>(), sets) {
        val out = ArrayList<StickerSection>()
        val faves = runCatching { graph.td.send(TdApi.GetFavoriteStickers()).stickers.toList() }.getOrDefault(emptyList())
        if (faves.isNotEmpty()) out += StickerSection(FavoritesId, favoritesTitle, faves)
        val recent = runCatching { graph.td.send(TdApi.GetRecentStickers(false)).stickers.toList() }.getOrDefault(emptyList())
        if (recent.isNotEmpty()) out += StickerSection(RecentId, recentTitle, recent)
        value = out.toList()
        sets.forEach { info ->
            val stickers = runCatching { graph.td.send(TdApi.GetStickerSet(info.id)).stickers.toList() }.getOrDefault(emptyList())
            if (stickers.isNotEmpty()) {
                out += StickerSection(info.id, info.title, stickers)
                value = out.toList()
            }
        }
    }
    val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val starts = remember(sections) {
        var index = 0
        sections.associate { section -> (section.id to index).also { index += 1 + section.stickers.size } }
    }
    val selected by remember(sections) {
        androidx.compose.runtime.derivedStateOf {
            val first = grid.firstVisibleItemIndex
            starts.entries.lastOrNull { it.value <= first }?.key ?: sections.firstOrNull()?.id ?: RecentId
        }
    }
    Column(Modifier.fillMaxSize()) {
        SetHeader(sets, selected, listOf(FavoritesId to androidx.compose.material.icons.Icons.Rounded.Star, RecentId to Icons.Rounded.History).filter { (id, _) -> sections.any { it.id == id } }) { id ->
            starts[id]?.let { scope.launch { grid.animateScrollToItem(it) } }
        }
        LazyVerticalGrid(GridCells.Fixed(5), Modifier.fillMaxSize(), state = grid, contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 64.dp)) {
            sections.forEach { section ->
                item(key = "h${section.id}", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, contentType = "header") {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 6.dp),
                    )
                }
                items(section.stickers, key = { "${section.id}:${it.id}" }, contentType = { "sticker" }) { sticker ->
                    Box(
                        Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).combinedClickable(onLongClick = { MediaPreview.sticker = sticker }) { onSticker(sticker) }.padding(5.dp),
                        contentAlignment = Alignment.Center,
                    ) { StickerView(sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.fillMaxSize(), video = false) }
                }
            }
        }
    }
}

@Composable
private fun GifsTab(onGif: (TdApi.Animation) -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val gifs by produceState(emptyList<TdApi.Animation>()) {
        value = runCatching { graph.td.send(TdApi.GetSavedAnimations()).animations.toList() }.getOrDefault(emptyList())
    }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val gap = 4.dp
        val available = maxWidth - 8.dp
        val rows = remember(gifs, available) { gifRows(gifs, available.value, GifRowHeight.value, gap.value) }
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(rows.size) { r ->
                val row = rows[r]
                Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth().height(row.height.dp)) {
                    row.items.forEach { (gif, width) ->
                        val thumb = gif.thumbnail?.takeIf { it.format is TdApi.ThumbnailFormatJpeg || it.format is TdApi.ThumbnailFormatPng || it.format is TdApi.ThumbnailFormatWebp }?.file
                        val path = thumb?.let { snapshot.files[it.id] }
                        if (thumb != null && path == null) LaunchedEffect(thumb.id) { graph.store.requestFileAsync(thumb.id) }
                        Box(
                            Modifier.width(width.dp).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).combinedClickable(onLongClick = { MediaPreview.gif = gif }) { onGif(gif) },
                        ) {
                            AsyncImage(model = path?.let(::File) ?: gif.minithumbnail?.data, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

private class GifRow(val items: List<Pair<TdApi.Animation, Float>>, val height: Float)

private fun gifRows(gifs: List<TdApi.Animation>, width: Float, target: Float, gap: Float): List<GifRow> {
    val rows = ArrayList<GifRow>()
    var current = ArrayList<Pair<TdApi.Animation, Float>>()
    var sum = 0f
    fun ratio(gif: TdApi.Animation) = if (gif.width > 0 && gif.height > 0) (gif.width.toFloat() / gif.height).coerceIn(0.5f, 3f) else 1f
    for (gif in gifs) {
        val r = ratio(gif)
        current += gif to r
        sum += r
        val spacing = gap * (current.size - 1)
        if (sum * target + spacing >= width) {
            val height = (width - spacing) / sum
            rows += GifRow(current.map { (g, k) -> g to k * height }, height)
            current = ArrayList()
            sum = 0f
        }
    }
    if (current.isNotEmpty()) rows += GifRow(current.map { (g, k) -> g to k * target }, target)
    return rows
}

private val GifRowHeight = 110.dp

fun stickerInput(sticker: TdApi.Sticker): TdApi.InputMessageContent =
    TdApi.InputMessageSticker(TdApi.InputSticker().apply {
        this.sticker = TdApi.InputFileRemote(sticker.sticker.remote.id)
        width = sticker.width
        height = sticker.height
    }, sticker.emoji)

fun gifInput(gif: TdApi.Animation): TdApi.InputMessageContent =
    TdApi.InputMessageAnimation(TdApi.InputAnimation().apply {
        animation = TdApi.InputFileRemote(gif.animation.remote.id)
        duration = gif.duration
        width = gif.width
        height = gif.height
        addedStickerFileIds = IntArray(0)
    }, TdApi.FormattedText("", emptyArray()), false, false)

object PanelRequest {
    var tab by mutableStateOf<PanelTab?>(null)
}
