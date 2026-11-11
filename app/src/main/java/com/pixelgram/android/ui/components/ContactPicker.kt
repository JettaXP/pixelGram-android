package com.pixelgram.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.td.UserInfo
import org.drinkless.tdlib.TdApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContactPickerSheet(onDismiss: () -> Unit, onPick: (UserInfo) -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    var ids by remember { mutableStateOf<List<Long>?>(null) }
    LaunchedEffect(Unit) { ids = runCatching { graph.td.send(TdApi.GetContacts()).userIds.toList() }.getOrDefault(emptyList()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.attach_contact), style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        val list = ids
        if (list == null) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@ModalBottomSheet
        }
        val users = list.mapNotNull { snapshot.users[it] }.sortedBy { it.name.lowercase() }
        LazyColumn {
            items(users, key = { it.id }) { user ->
                ListItem(
                    modifier = Modifier.clickable { onPick(user) },
                    leadingContent = { Avatar(user.name, user.id, user.localPhoto ?: snapshot.files[user.photoFileId], user.minithumb, 44.dp, onNeedFile = { graph.store.requestFileAsync(user.photoFileId) }) },
                    headlineContent = { Text(user.name) },
                    supportingContent = { Text(statusLine(user)) },
                )
            }
        }
    }
}
