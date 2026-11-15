package com.pixelgram.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.feature.chat.ReactionGlyph
import com.pixelgram.android.feature.chat.StickerView
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.QuickReactionKey
import com.pixelgram.android.ui.components.CustomEmojiView
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.SegmentPosition
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.SettingsSwitch
import com.pixelgram.android.ui.components.segmentPosition
import com.pixelgram.android.ui.components.segmentedColors
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

val StandardReactions = listOf(
    "❤" to "Red Heart", "👍" to "Thumbs Up", "👎" to "Thumbs Down", "🔥" to "Fire", "🥰" to "Smiling Face with Hearts",
    "👏" to "Clapping Hands", "😁" to "Beaming Face", "🤔" to "Thinking Face", "🤯" to "Exploding Head", "😱" to "Screaming Face",
    "🤬" to "Face with Symbols", "😢" to "Crying Face", "🎉" to "Party Popper", "🤩" to "Star-Struck", "🤮" to "Vomiting Face",
    "💩" to "Pile of Poo", "🙏" to "Folded Hands", "👌" to "OK Hand", "🕊" to "Dove", "🤡" to "Clown Face", "🥱" to "Yawning Face",
    "🥴" to "Woozy Face", "😍" to "Heart Eyes", "🐳" to "Whale", "❤‍🔥" to "Heart on Fire", "🌚" to "New Moon Face", "🌭" to "Hot Dog",
    "💯" to "Hundred Points", "🤣" to "Rolling on the Floor", "⚡" to "High Voltage", "🍌" to "Banana", "🏆" to "Trophy",
    "💔" to "Broken Heart", "🤨" to "Raised Eyebrow", "😐" to "Neutral Face", "🍓" to "Strawberry", "🍾" to "Champagne",
    "💋" to "Kiss Mark", "🖕" to "Middle Finger", "😈" to "Smiling Devil", "😴" to "Sleeping Face", "😭" to "Loudly Crying",
    "🤓" to "Nerd Face", "👻" to "Ghost", "👨‍💻" to "Technologist", "👀" to "Eyes", "🎃" to "Jack-O-Lantern", "🙈" to "See-No-Evil Monkey",
    "😇" to "Halo", "😨" to "Fearful Face", "🤝" to "Handshake", "✍" to "Writing Hand", "🤗" to "Hugging Face", "🫡" to "Saluting Face",
    "🎅" to "Santa Claus", "🎄" to "Christmas Tree", "☃" to "Snowman", "💅" to "Nail Polish", "🤪" to "Zany Face", "🗿" to "Moai",
    "🆒" to "Cool", "💘" to "Heart with Arrow", "🙉" to "Hear-No-Evil Monkey", "🦄" to "Unicorn", "😘" to "Face Blowing a Kiss",
    "💊" to "Pill", "🙊" to "Speak-No-Evil Monkey", "😎" to "Smiling Face with Sunglasses", "👾" to "Alien Monster", "🤷‍♂" to "Man Shrugging",
    "🤷" to "Person Shrugging", "🤷‍♀" to "Woman Shrugging", "😡" to "Enraged Face",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StickersSettingsScreen(graph: AppGraph, navigator: Navigator) {
    val appearance by graph.appearance.state.collectAsState()
    val snapshot by graph.store.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    var reload by remember { mutableStateOf(0) }
    val stickers by produceState(emptyList<TdApi.StickerSetInfo>(), reload) {
        value = runCatching { graph.td.send(TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular())).sets.toList() }.getOrDefault(emptyList())
    }
    val emoji by produceState(0, reload) {
        value = runCatching { graph.td.send(TdApi.GetInstalledStickerSets(TdApi.StickerTypeCustomEmoji())).sets.size }.getOrDefault(0)
    }
    val archived by produceState(0) {
        value = runCatching { graph.td.send(TdApi.GetArchivedStickerSets(TdApi.StickerTypeRegular(), 0, 1)).totalCount }.getOrDefault(0)
    }
    val collapse = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(collapse.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.MediumFlexibleTopAppBar(
                scrollBehavior = collapse,
                title = { Text(stringResource(R.string.stickers_title)) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsGroup {
                SettingsItem(stringResource(R.string.stickers_archived), SegmentPosition.Top, null, Icons.Rounded.Archive) { Text(archived.toString(), color = MaterialTheme.colorScheme.primary) }
                SettingsItem(stringResource(R.string.stickers_emoji), SegmentPosition.Middle, null, Icons.Rounded.EmojiEmotions) { Text(emoji.toString(), color = MaterialTheme.colorScheme.primary) }
                SettingsItem(stringResource(R.string.stickers_quick_reaction), SegmentPosition.Bottom, null, Icons.Rounded.Favorite, onClick = { navigator.open(QuickReactionKey) }) {
                    ReactionGlyph(appearance.doubleTapEmoji, 26.dp)
                }
            }
            SettingsGroup(stringResource(R.string.stickers_settings)) {
                SettingsSwitch(stringResource(R.string.stickers_large_emoji), appearance.largeEmoji, { v -> graph.appearance.update { it.copy(largeEmoji = v) } }, SegmentPosition.Single, stringResource(R.string.stickers_large_emoji_sub))
            }
            if (stickers.isNotEmpty()) {
                SettingsGroup(stringResource(R.string.stickers_my_sets)) {
                    stickers.forEachIndexed { index, set ->
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(index, stickers.size),
                            colors = segmentedColors(),
                            leadingContent = {
                                Box(Modifier.size(44.dp)) {
                                    set.covers?.firstOrNull()?.let { StickerView(it, snapshot.files, { id -> graph.store.requestFileAsync(id) }, Modifier.fillMaxSize(), video = false) }
                                }
                            },
                            supportingContent = { Text(pluralStringResource(R.plurals.stickers_count, set.size, set.size)) },
                            trailingContent = {
                                IconButton(onClick = {
                                    scope.launch {
                                        runCatching { graph.td.send(TdApi.ChangeStickerSet(set.id, false, true)) }
                                        reload++
                                    }
                                }) { Icon(Icons.Rounded.Archive, stringResource(R.string.stickers_archive)) }
                            },
                        ) { Text(set.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickReactionScreen(graph: AppGraph, navigator: Navigator) {
    val appearance by graph.appearance.state.collectAsState()
    val premiumEmoji by produceState(emptyList<TdApi.Sticker>()) {
        val sets = runCatching { graph.td.send(TdApi.GetInstalledStickerSets(TdApi.StickerTypeCustomEmoji())).sets.toList() }.getOrDefault(emptyList())
        value = sets.take(4).flatMap { info -> runCatching { graph.td.send(TdApi.GetStickerSet(info.id)).stickers.toList() }.getOrDefault(emptyList()) }.take(64)
    }
    val select: (String) -> Unit = { key -> graph.appearance.update { it.copy(doubleTapEmoji = key) } }
    val collapse = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(collapse.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.MediumFlexibleTopAppBar(
                scrollBehavior = collapse,
                title = { Text(stringResource(R.string.stickers_quick_reaction)) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Surface(shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(stringResource(R.string.quick_reaction_sample), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    ReactionGlyph(appearance.doubleTapEmoji, 22.dp)
                }
            }
            Text(stringResource(R.string.quick_reaction_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
            if (premiumEmoji.isNotEmpty()) {
                SettingsGroup(stringResource(R.string.quick_reaction_premium)) {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(10.dp), maxItemsInEachRow = 8) {
                            premiumEmoji.forEach { sticker ->
                                val id = (sticker.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId ?: return@forEach
                                val key = "ce:$id"
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                        .background(if (appearance.doubleTapEmoji == key) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                        .clickable { select(key) }.padding(5.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CustomEmojiView(id, sticker.emoji, Modifier.fillMaxSize()) }
                            }
                        }
                    }
                }
            }
            SettingsGroup(stringResource(R.string.quick_reaction_standard)) {
                StandardReactions.forEachIndexed { index, (emoji, name) ->
                    SegmentedListItem(
                        onClick = { select(emoji) },
                        shapes = ListItemDefaults.segmentedShapes(index, StandardReactions.size),
                        colors = segmentedColors(),
                        leadingContent = { Text(emoji, style = MaterialTheme.typography.headlineSmall) },
                        trailingContent = { if (appearance.doubleTapEmoji == emoji) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    ) { Text(name) }
                }
            }
        }
    }
}
