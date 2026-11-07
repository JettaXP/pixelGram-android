package com.pixelgram.android.feature.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.ProfileKey
import com.pixelgram.android.ui.components.Avatar
import org.drinkless.tdlib.TdApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContactsTab(graph: AppGraph, navigator: Navigator, onOpenDrawer: () -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    var ids by remember { mutableStateOf<List<Long>?>(null) }
    LaunchedEffect(Unit) { ids = runCatching { graph.td.send(TdApi.GetContacts()).userIds.toList() }.getOrDefault(emptyList()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_contacts), style = MaterialTheme.typography.titleLargeEmphasized) },
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) } },
            )
        },
    ) { padding ->
        val list = ids
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        val users = list.mapNotNull { snapshot.users[it] }.sortedWith(compareByDescending<com.pixelgram.android.td.UserInfo> { it.online }.thenBy { it.name.lowercase() })
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 8.dp + com.pixelgram.android.ui.components.LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(users, key = { _, it -> it.id }) { index, user ->
                androidx.compose.material3.SegmentedListItem(
                    onClick = { navigator.open(ProfileKey(user.id)) },
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, users.size),
                    colors = com.pixelgram.android.ui.components.segmentedColors(),
                    leadingContent = {
                        Avatar(user.name, user.id, snapshot.files[user.photoFileId], user.minithumb, 48.dp, onNeedFile = { graph.store.requestFileAsync(user.photoFileId) })
                    },
                    supportingContent = {
                        Text(
                            stringResource(if (user.online) R.string.chat_online else R.string.chat_last_seen_recently),
                            color = if (user.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                ) {
                    Text(user.name)
                }
            }
        }
    }
}
