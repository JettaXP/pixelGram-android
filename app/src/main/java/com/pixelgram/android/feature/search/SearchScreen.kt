package com.pixelgram.android.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.segmentedColors
import kotlinx.coroutines.delay
import org.drinkless.tdlib.TdApi

private data class Found(val chat: TdApi.Chat, val subtitle: String?)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(graph: AppGraph, navigator: Navigator) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Found>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val snapshot by graph.store.snapshot.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(query) {
        val text = query.trim()
        if (text.isEmpty()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        loading = true
        val ids = LinkedHashSet<Long>()
        runCatching { graph.td.send(TdApi.SearchChats().apply { this.query = text; limit = 50 }).chatIds }.getOrNull()?.forEach(ids::add)
        runCatching { graph.td.send(TdApi.SearchChatsOnServer().apply { this.query = text; limit = 50 }).chatIds }.getOrNull()?.forEach(ids::add)
        if (text.length >= 4) runCatching { graph.td.send(TdApi.SearchPublicChats().apply { this.query = text }).chatIds }.getOrNull()?.forEach(ids::add)
        results = ids.mapNotNull { id -> runCatching { graph.td.send(TdApi.GetChat(id)) }.getOrNull() }.map { chat ->
            val user = (chat.type as? TdApi.ChatTypePrivate)?.userId?.let { snapshot.users[it] }
            Found(chat, user?.username?.let { "@$it" })
        }
        loading = false
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search_chats)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp).focusRequester(focus),
                    )
                },
            )
        },
    ) { padding ->
        when {
            loading && results.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            query.isNotBlank() && results.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.search_nothing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(results, key = { _, it -> it.chat.id }) { index, found ->
                    val chat = found.chat
                    val photoId = chat.photo?.small?.id ?: 0
                    SegmentedListItem(
                        onClick = { navigator.open(ChatKey(chat.id)) },
                        shapes = ListItemDefaults.segmentedShapes(index, results.size),
                        colors = segmentedColors(),
                        leadingContent = {
                            Avatar(chat.title, chat.id, snapshot.files[photoId], chat.photo?.minithumbnail?.data, 48.dp, onNeedFile = { graph.store.requestFileAsync(photoId) })
                        },
                        supportingContent = found.subtitle?.let { { Text(it) } },
                    ) {
                        Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
