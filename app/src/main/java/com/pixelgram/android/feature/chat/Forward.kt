package com.pixelgram.android.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.td.LIST_MAIN
import com.pixelgram.android.td.folderKey
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.FolderChips
import com.pixelgram.android.ui.components.FolderTab
import com.pixelgram.android.ui.components.Icon
import org.drinkless.tdlib.TdApi

@Immutable
data class ForwardDraft(
    val fromChatId: Long,
    val messages: List<UiMessage>,
    val hideSender: Boolean = false,
    val hideCaption: Boolean = false,
) {
    val ids: LongArray get() = messages.flatMap { m -> m.parts.map { it.id }.ifEmpty { listOf(m.id) } }.distinct().sorted().toLongArray()
    val hasCaption: Boolean get() = messages.any { m -> m.content !is TdApi.MessageText && m.text.isNotBlank() }
}

object ForwardDrafts {
    val map = mutableStateMapOf<Long, ForwardDraft>()
}

suspend fun sendForward(graph: com.pixelgram.android.AppGraph, toChatId: Long, draft: ForwardDraft) {
    val ids = draft.ids
    if (ids.isEmpty()) return
    graph.td.send(TdApi.ForwardMessages().apply {
        chatId = toChatId
        fromChatId = draft.fromChatId
        messageIds = ids
        sendCopy = draft.hideSender
        removeCaption = draft.hideSender && draft.hideCaption
    })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForwardPicker(onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    var query by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf(0) }
    val allTitle = stringResource(R.string.folder_all)
    val tabs = remember(snapshot.folders, allTitle) {
        listOf(FolderTab(0, allTitle, "All")) + snapshot.folders.map { FolderTab(it.id, it.title, it.icon ?: "Custom") }
    }
    val rows = (if (folder == 0) snapshot.lists[LIST_MAIN] else snapshot.lists[folderKey(folder)]).orEmpty()
        .filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(64.dp)) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                    Text(stringResource(R.string.forward_to_title), style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.align(Alignment.Center))
                }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.forward_search)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(8.dp))
                FolderChips(tabs, folder, com.pixelgram.android.settings.FolderTitle.NamesAndIcons, onSelect = { folder = it })
                if (rows.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.FolderOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.forward_empty), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.forward_empty_sub), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
                        items(rows, key = { it.id }) { row ->
                            val saved = row.userId == snapshot.meId && row.userId != 0L
                            ListItem(
                                modifier = Modifier.clickable { onPick(row.id) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Avatar(
                                        title = row.title,
                                        seed = row.id,
                                        path = if (saved) null else row.photoPath,
                                        minithumb = if (saved) null else row.minithumb,
                                        size = 48.dp,
                                        icon = if (saved) Icons.Rounded.Bookmark else null,
                                        onNeedFile = { graph.store.requestFileAsync(row.photoFileId) },
                                    )
                                },
                                headlineContent = { Text(if (saved) stringResource(R.string.drawer_saved) else row.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForwardOptionsMenu(
    draft: ForwardDraft,
    onChange: (ForwardDraft) -> Unit,
    onChangeRecipient: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    androidx.compose.ui.window.Popup(
        popupPositionProvider = remember {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize,
                ) = androidx.compose.ui.unit.IntOffset(anchorBounds.left, (anchorBounds.top - popupContentSize.height).coerceAtLeast(0))
            }
        },
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            shown,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f)),
            modifier = Modifier.padding(12.dp),
        ) {
            Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        OptionRow(
                            if (draft.hideSender) Icons.Rounded.Person else Icons.Rounded.PersonOff,
                            stringResource(if (draft.hideSender) R.string.forward_show_sender else R.string.forward_hide_sender),
                        ) { onChange(draft.copy(hideSender = !draft.hideSender, hideCaption = if (draft.hideSender) false else draft.hideCaption)) }
                        if (draft.hasCaption && draft.hideSender) {
                            OptionRow(
                                if (draft.hideCaption) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                                stringResource(if (draft.hideCaption) R.string.forward_show_caption else R.string.forward_hide_caption),
                            ) { onChange(draft.copy(hideCaption = !draft.hideCaption)) }
                        }
                        OptionRow(Icons.Rounded.SwapHoriz, stringResource(R.string.forward_change_recipient), onClick = onChangeRecipient)
                    }
                }
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        OptionRow(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.forward_send), onClick = onSend)
                        OptionRow(Icons.Rounded.Delete, stringResource(R.string.forward_cancel), MaterialTheme.colorScheme.error, onCancel)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(icon: ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun forwardTitle(draft: ForwardDraft): String =
    pluralStringResource(R.plurals.forward_messages, draft.messages.size, draft.messages.size)
