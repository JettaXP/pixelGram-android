package com.pixelgram.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.settings.Backup
import com.pixelgram.android.settings.BackupRead
import com.pixelgram.android.td.LIST_MAIN
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

@Composable
fun BackupImportDialog(file: File, onDone: (String?) -> Unit) {
    val graph = LocalGraph.current
    val scope = rememberCoroutineScope()
    val read by produceState<BackupRead?>(null, file) { value = Backup.read(file) }
    val imported = stringResource(R.string.backup_imported)
    val failed = stringResource(R.string.backup_failed, file.name)
    when (val result = read) {
        null -> Unit
        is BackupRead.Valid -> AlertDialog(
            onDismissRequest = { onDone(null) },
            icon = { Icon(Icons.Rounded.Settings, null) },
            title = { Text(stringResource(R.string.backup_import_title)) },
            text = { Text(stringResource(R.string.backup_import_text, file.name)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch { onDone(if (graph.appearance.importValues(result.values)) imported else failed) }
                }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.backup_import)) }
            },
            dismissButton = { TextButton(onClick = { onDone(null) }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_cancel)) } },
        )
        else -> AlertDialog(
            onDismissRequest = { onDone(null) },
            icon = { Icon(Icons.Rounded.Settings, null) },
            title = { Text(stringResource(R.string.backup_import_title)) },
            text = { Text(stringResource(if (result is BackupRead.Desktop) R.string.backup_desktop else R.string.backup_invalid)) },
            confirmButton = { TextButton(onClick = { onDone(null) }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPickerSheet(onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val rows = snapshot.lists[LIST_MAIN].orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.backup_pick_chat),
            style = MaterialTheme.typography.titleLargeEmphasized,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth()) {
            items(rows, key = { it.id }) { row ->
                val saved = row.userId == snapshot.meId && row.userId != 0L
                androidx.compose.material3.ListItem(
                    modifier = Modifier.clickable { onPick(row.id) },
                    leadingContent = {
                        Avatar(
                            title = row.title,
                            seed = row.id,
                            path = if (saved) null else row.photoPath,
                            minithumb = if (saved) null else row.minithumb,
                            size = 44.dp,
                            icon = if (saved) Icons.Rounded.Bookmark else null,
                            onNeedFile = { graph.store.requestFileAsync(row.photoFileId) },
                        )
                    },
                    headlineContent = {
                        Text(if (saved) stringResource(R.string.drawer_saved) else row.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                )
            }
        }
    }
}

suspend fun sendDocument(graph: AppGraph, chatId: Long, file: File) {
    graph.td.send(TdApi.SendMessage().apply {
        this.chatId = chatId
        inputMessageContent = TdApi.InputMessageDocument().apply {
            document = TdApi.InputDocument().apply {
                document = TdApi.InputFileLocal(file.absolutePath)
                disableContentTypeDetection = true
            }
            caption = TdApi.FormattedText("", emptyArray())
        }
    })
}
