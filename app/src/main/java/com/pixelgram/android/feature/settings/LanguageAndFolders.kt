package com.pixelgram.android.feature.settings

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.PixelSwitch
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.segmentPosition
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.util.Locale

object AppLanguage {
    private const val Prefs = "app_language"
    private const val Key = "tag"

    fun saved(context: Context): String = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(Key, "").orEmpty()

    fun wrap(base: Context): Context {
        val tag = saved(base)
        if (tag.isEmpty() || Build.VERSION.SDK_INT >= 33) return base
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(config)
    }

    fun apply(context: Context, tag: String) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putString(Key, tag).apply()
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) android.os.LocaleList.getEmptyLocaleList() else android.os.LocaleList.forLanguageTags(tag)
        } else {
            (context as? Activity)?.recreate()
        }
    }
}

@Composable
fun LanguageSection() {
    val context = LocalContext.current
    var current by remember { mutableStateOf(AppLanguage.saved(context)) }
    val options = listOf("" to stringResource(R.string.language_system), "en" to "English", "ru" to "Русский")
    SettingsGroup {
        options.forEachIndexed { index, (tag, label) ->
            SettingsItem(
                title = label,
                position = segmentPosition(index, options.size),
                icon = Icons.Rounded.Language,
                onClick = {
                    current = tag
                    AppLanguage.apply(context, tag)
                },
            ) {
                RadioButton(selected = current == tag, onClick = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FoldersSection(graph: AppGraph) {
    val snapshot by graph.store.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Int?>(null) }
    val folders = snapshot.folders
    SettingsGroup(stringResource(R.string.folders_mine)) {
        folders.forEachIndexed { index, folder ->
            SettingsItem(
                title = folder.title,
                position = segmentPosition(index, folders.size + 1),
                icon = Icons.Rounded.Folder,
                onClick = { editing = folder.id },
            ) {
                IconButton(onClick = { deleting = folder.id }) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete)) }
            }
        }
        SettingsItem(
            title = stringResource(R.string.folders_create),
            position = segmentPosition(folders.size, folders.size + 1),
            icon = Icons.Rounded.Add,
            onClick = { creating = true },
        )
    }
    if (creating || editing != null) {
        val id = editing
        FolderDialog(graph, id) {
            creating = false
            editing = null
        }
    }
    deleting?.let { id ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.folders_delete)) },
            text = { Text(folders.firstOrNull { it.id == id }?.title.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch { runCatching { graph.td.send(TdApi.DeleteChatFolder(id, LongArray(0))) } }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun FolderDialog(graph: AppGraph, id: Int?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<TdApi.ChatFolder?>(null) }
    var name by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(false) }
    var others by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf(false) }
    var channels by remember { mutableStateOf(false) }
    var bots by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(id) {
        if (id != null) {
            runCatching { graph.td.send(TdApi.GetChatFolder(id)) }.getOrNull()?.let {
                loaded = it
                name = it.name.text.text
                contacts = it.includeContacts
                others = it.includeNonContacts
                groups = it.includeGroups
                channels = it.includeChannels
                bots = it.includeBots
            }
        }
    }
    val flags = listOf(
        R.string.folders_contacts to contacts,
        R.string.folders_non_contacts to others,
        R.string.folders_groups to groups,
        R.string.folders_channels to channels,
        R.string.folders_bots to bots,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (id == null) R.string.folders_create else R.string.folders_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(name, { name = it.take(12) }, label = { Text(stringResource(R.string.folders_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                flags.forEachIndexed { index, (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(stringResource(label), Modifier.weight(1f))
                        PixelSwitch(value, { v ->
                            when (index) {
                                0 -> contacts = v
                                1 -> others = v
                                2 -> groups = v
                                3 -> channels = v
                                else -> bots = v
                            }
                        })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (contacts || others || groups || channels || bots || (loaded?.includedChatIds?.isNotEmpty() == true)),
                onClick = {
                    val folder = (loaded ?: TdApi.ChatFolder().apply {
                        pinnedChatIds = LongArray(0)
                        includedChatIds = LongArray(0)
                        excludedChatIds = LongArray(0)
                        colorId = -1
                    }).apply {
                        this.name = TdApi.ChatFolderName(TdApi.FormattedText(name.trim(), emptyArray()), false)
                        includeContacts = contacts
                        includeNonContacts = others
                        includeGroups = groups
                        includeChannels = channels
                        includeBots = bots
                    }
                    scope.launch {
                        runCatching {
                            if (id == null) graph.td.send(TdApi.CreateChatFolder(folder)) else graph.td.send(TdApi.EditChatFolder(id, folder))
                        }
                    }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
