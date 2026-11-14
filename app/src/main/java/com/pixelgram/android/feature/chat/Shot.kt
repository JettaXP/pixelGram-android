package com.pixelgram.android.feature.chat

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.height
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageShotDialog(messages: List<UiMessage>, group: Boolean, onSend: (String) -> Unit, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var names by remember { mutableStateOf(group) }
    var showBackground by remember { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(0) }
    var themeMenu by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val ordered = remember(messages) { messages.sortedBy { it.id } }
    val base = MaterialTheme.colorScheme
    val scheme = when (theme) {
        1 -> remember { if (Build.VERSION.SDK_INT >= 31) androidx.compose.material3.dynamicLightColorScheme(context) else androidx.compose.material3.lightColorScheme() }
        2 -> remember { if (Build.VERSION.SDK_INT >= 31) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.darkColorScheme() }
        else -> base
    }

    suspend fun render(): File? {
        val bitmap = runCatching { layer.toImageBitmap().asAndroidBitmap() }.getOrNull() ?: return null
        return withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shots").apply { mkdirs() }
            val file = File(dir, "shot-${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) }
                    Text(stringResource(R.string.shot_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.TopCenter) {
                    MaterialTheme(colorScheme = scheme) {
                    Column(
                        Modifier
                            .drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (showBackground) scheme.surfaceContainerLowest else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                    ) {
                        if (showDate) {
                            Surface(shape = RoundedCornerShape(12.dp), color = scheme.surfaceContainerHigh, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp)) {
                                Text(
                                    java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG).format(java.util.Date(ordered.first().date * 1000L)),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                        }
                        ordered.forEachIndexed { index, message ->
                            val prev = ordered.getOrNull(index - 1)
                            val next = ordered.getOrNull(index + 1)
                            val joinsAbove = prev?.senderId == message.senderId
                            val joinsBelow = next?.senderId == message.senderId
                            val sender = snapshot.users[message.senderId]
                            val showName = names && !message.outgoing && !joinsAbove
                            MessageBubble(
                                message = message,
                                senderName = if (showName) sender?.name ?: snapshot.lists.values.flatten().firstOrNull { it.id == message.senderId }?.title else null,
                                senderAvatar = if (names && !message.outgoing) {
                                    {
                                        if (!joinsBelow) Avatar(
                                            title = sender?.name.orEmpty(),
                                            seed = message.senderId,
                                            path = sender?.localPhoto ?: sender?.photoFileId?.let { snapshot.files[it] },
                                            minithumb = sender?.minithumb,
                                            size = 34.dp,
                                            onNeedFile = { sender?.photoFileId?.let(graph.store::requestFileAsync) },
                                        )
                                    }
                                } else null,
                                joinsAbove = joinsAbove,
                                joinsBelow = joinsBelow,
                                files = snapshot.files,
                                onNeedFile = graph.store::requestFileAsync,
                                onOpenPhoto = { _, _ -> },
                                reply = null,
                                replySender = null,
                            )
                        }
                    }
                    }
                    Box(
                        Modifier.matchParentSize().pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial).changes.forEach { if (it.pressed != it.previousPressed) it.consume() }
                            }
                        },
                    )
                }
                Text(stringResource(R.string.shot_preferences), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
                    val themes = listOf(stringResource(R.string.shot_theme_default), stringResource(R.string.shot_theme_light), stringResource(R.string.shot_theme_dark))
                    Box {
                        androidx.compose.material3.SegmentedListItem(
                            onClick = { themeMenu = true },
                            shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(0, 4),
                            colors = com.pixelgram.android.ui.components.segmentedColors(),
                            trailingContent = { Text(themes[theme], color = MaterialTheme.colorScheme.primary) },
                        ) { Text(stringResource(R.string.shot_theme)) }
                        androidx.compose.material3.DropdownMenu(themeMenu, onDismissRequest = { themeMenu = false }, shape = RoundedCornerShape(16.dp)) {
                            themes.forEachIndexed { index, label ->
                                androidx.compose.material3.DropdownMenuItem(text = { Text(label) }, onClick = {
                                    theme = index
                                    themeMenu = false
                                })
                            }
                        }
                    }
                    ShotSwitch(stringResource(R.string.shot_background), showBackground, 1) { showBackground = it }
                    ShotSwitch(stringResource(R.string.shot_date), showDate, 2) { showDate = it }
                    ShotSwitch(stringResource(R.string.shot_names), names, 3) { names = it }
                }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShotAction(Icons.Rounded.Download, stringResource(R.string.shot_save)) {
                        scope.launch {
                            val file = render() ?: return@launch
                            val ok = withContext(Dispatchers.IO) { saveToGallery(context, file) }
                            Toast.makeText(context, if (ok) R.string.shot_saved else R.string.shot_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                    ShotAction(Icons.Rounded.Share, stringResource(R.string.shot_share)) {
                        scope.launch {
                            val file = render() ?: return@launch
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                            val intent = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            runCatching { context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    }
                    ShotAction(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.shot_send), primary = true) { picking = true }
                }
            }
        }
    }
    if (picking) {
        ForwardPicker(
            onPick = { target ->
                picking = false
                scope.launch {
                    val file = render() ?: return@launch
                    runCatching {
                        graph.td.send(org.drinkless.tdlib.TdApi.SendMessage().apply {
                            chatId = target
                            inputMessageContent = org.drinkless.tdlib.TdApi.InputMessagePhoto().apply {
                                photo = org.drinkless.tdlib.TdApi.InputPhoto().apply {
                                    photo = org.drinkless.tdlib.TdApi.InputFileLocal(file.absolutePath)
                                    addedStickerFileIds = IntArray(0)
                                }
                                caption = org.drinkless.tdlib.TdApi.FormattedText("", emptyArray())
                            }
                        })
                    }
                    onSend(file.absolutePath)
                }
            },
            onDismiss = { picking = false },
        )
    }
}

private fun saveToGallery(context: android.content.Context, file: File): Boolean = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/pixelGram")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
    true
}.getOrDefault(false)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShotSwitch(label: String, checked: Boolean, index: Int, onChange: (Boolean) -> Unit) {
    androidx.compose.material3.SegmentedListItem(
        onClick = { onChange(!checked) },
        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, 4),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        trailingContent = { androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange) },
    ) { Text(label) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun androidx.compose.foundation.layout.RowScope.ShotAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, primary: Boolean = false, onClick: () -> Unit) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Icon(icon, null) }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
