package com.pixelgram.android.feature.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.SegmentPosition
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.segmentPosition
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

object NowPlayingPrefs {
    private const val Prefs = "pixel_now_playing"
    const val None = "none"
    const val LastFm = "lastfm"
    const val StatsFm = "statsfm"

    fun service(context: android.content.Context): String = context.getSharedPreferences(Prefs, android.content.Context.MODE_PRIVATE).getString("service", None) ?: None
    fun username(context: android.content.Context): String = context.getSharedPreferences(Prefs, android.content.Context.MODE_PRIVATE).getString("username", "").orEmpty()
    fun apiKey(context: android.content.Context): String = context.getSharedPreferences(Prefs, android.content.Context.MODE_PRIVATE).getString("key", "").orEmpty()
    fun save(context: android.content.Context, service: String, username: String, key: String) {
        context.getSharedPreferences(Prefs, android.content.Context.MODE_PRIVATE).edit().putString("service", service).putString("username", username.trim()).putString("key", key.trim()).apply()
    }
}

@Composable
private fun FieldRow(value: String, onChange: (String) -> Unit, hint: String, position: SegmentPosition, max: Int, counter: Boolean = false) {
    val (index, count) = when (position) {
        SegmentPosition.Single -> 0 to 1
        SegmentPosition.Top -> 0 to 2
        SegmentPosition.Bottom -> 1 to 2
        SegmentPosition.Middle -> 1 to 3
    }
    androidx.compose.material3.SegmentedListItem(
        onClick = {},
        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, count),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        trailingContent = if (counter) ({ Text((max - value.length).toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) }) else null,
    ) {
        Box {
            if (value.isEmpty()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value,
                { onChange(it.take(max)) },
                singleLine = !counter,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 6.dp))
}

@Composable
private fun Note(text: androidx.compose.ui.text.AnnotatedString, onClick: (() -> Unit)? = null) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp).then(if (onClick != null) Modifier.combinedClickable(interactionSource = null, indication = null, onClick = onClick) else Modifier),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InfoRow(icon: ImageVector, tone: Color, text: String, position: SegmentPosition, copyValue: String?, tapCopies: Boolean = false, onClick: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box {
        SettingsItem(
            title = text,
            position = position,
            onClick = if (tapCopies) ({ menu = true }) else onClick,
            onLongClick = if (copyValue != null) ({
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                menu = true
            }) else null,
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_copy)) },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                onClick = {
                    menu = false
                    context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText(null, copyValue))
                    android.widget.Toast.makeText(context, context.getString(R.string.profile_copied), android.widget.Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountEditorScreen(graph: AppGraph, navigator: Navigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snapshot by graph.store.snapshot.collectAsState()
    val me = snapshot.users[snapshot.meId]
    var first by remember(me?.id) { mutableStateOf(me?.firstName.orEmpty()) }
    var last by remember(me?.id) { mutableStateOf(me?.lastName.orEmpty()) }
    var bio by remember(me?.id) { mutableStateOf("") }
    var full by remember { mutableStateOf<TdApi.UserFullInfo?>(null) }
    LaunchedEffect(snapshot.meId) {
        if (snapshot.meId == 0L) return@LaunchedEffect
        full = runCatching { graph.td.send(TdApi.GetUserFullInfo(snapshot.meId)) }.getOrNull()
        bio = full?.bio?.text.orEmpty()
    }
    val maxBio = if (me?.premium == true) 140 else 70
    val latestFirst = androidx.compose.runtime.rememberUpdatedState(first)
    val latestLast = androidx.compose.runtime.rememberUpdatedState(last)
    val latestBio = androidx.compose.runtime.rememberUpdatedState(bio)
    DisposableEffect(me?.id) {
        onDispose {
            val user = me ?: return@onDispose
            graph.scope.launch {
                if (latestFirst.value.isNotBlank() && (latestFirst.value != user.firstName || latestLast.value != user.lastName)) runCatching { graph.td.send(TdApi.SetName(latestFirst.value.trim(), latestLast.value.trim())) }
                if (full != null && latestBio.value != full?.bio?.text.orEmpty()) runCatching { graph.td.send(TdApi.SetBio(latestBio.value.trim())) }
            }
        }
    }
    var editingUsername by remember { mutableStateOf(false) }
    var editingBirthday by remember { mutableStateOf(false) }
    var nowPlaying by remember { mutableStateOf(false) }
    var personal by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    Header(stringResource(R.string.account_your_name))
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
        FieldRow(first, { first = it }, stringResource(R.string.account_first_name), SegmentPosition.Top, 64)
        FieldRow(last, { last = it }, stringResource(R.string.account_last_name), SegmentPosition.Bottom, 64)
    }
    Spacer(Modifier.height(12.dp))
    Column(Modifier.padding(horizontal = 12.dp)) {
        FieldRow(bio, { bio = it }, stringResource(R.string.profile_bio), SegmentPosition.Single, maxBio, counter = true)
    }
    Note(androidx.compose.ui.text.AnnotatedString(stringResource(R.string.account_bio_note)))

    Header(stringResource(R.string.account_your_info))
    SettingsGroup {
        val phone = me?.phone?.let { "+" + com.pixelgram.android.util.formatPhone(it) }.orEmpty()
        InfoRow(Icons.Rounded.Call, Color(0xFF34C759), phone, SegmentPosition.Top, phone, tapCopies = true) {}
        val username = me?.username
        InfoRow(Icons.Rounded.AlternateEmail, Color(0xFFF29A1F), username?.let { "@$it" } ?: stringResource(R.string.account_set_username), SegmentPosition.Middle, username?.let { "@$it" }) { navigator.open(com.pixelgram.android.nav.UsernameKey) }
        val birth = full?.birthdate
        InfoRow(Icons.Rounded.Cake, Color(0xFF2F95F2), birth?.let { formatBirth(it) } ?: stringResource(R.string.account_add_birthday), SegmentPosition.Bottom, null) { editingBirthday = true }
    }
    Note(androidx.compose.ui.text.AnnotatedString(stringResource(R.string.account_birthday_note)))

    Spacer(Modifier.height(8.dp))
    SettingsGroup {
        val service = remember(nowPlaying) { NowPlayingPrefs.service(context) }
        SettingsItem(
            stringResource(R.string.account_scrobbling),
            SegmentPosition.Top,
            onClick = { nowPlaying = true },
            trailing = { Text(serviceLabel(service), color = MaterialTheme.colorScheme.primary) },
        )
        val personalId = full?.personalChatId ?: 0L
        SettingsItem(
            stringResource(R.string.account_personal_channel),
            SegmentPosition.Middle,
            onClick = { personal = true },
            trailing = { Text(if (personalId != 0L) graph.store.chat(personalId)?.title.orEmpty() else stringResource(R.string.account_add), color = MaterialTheme.colorScheme.primary) },
        )
        SettingsItem(
            stringResource(R.string.account_automation),
            SegmentPosition.Bottom,
            onClick = { navigator.open(com.pixelgram.android.nav.BusinessKey) },
        )
    }
    Note(androidx.compose.ui.text.AnnotatedString(stringResource(R.string.account_automation_note)))

    Spacer(Modifier.height(8.dp))
    val accounts = remember { com.pixelgram.android.account.Accounts.all(context) }
    val active = remember { com.pixelgram.android.account.Accounts.active(context) }
    val others = accounts.filter { it.id != active && it.userId != 0L }
    SettingsGroup {
        val total = others.size + 1
        SettingsItem(
            stringResource(R.string.accounts_add),
            segmentPosition(0, total),
            icon = Icons.Rounded.PersonAdd,
            onClick = { com.pixelgram.android.account.Accounts.add(context) },
        )
        others.forEachIndexed { i, account ->
            val (index, count) = segmentPosition(i + 1, total).let { p -> (i + 1) to total }
            androidx.compose.material3.SegmentedListItem(
                onClick = { com.pixelgram.android.account.Accounts.switchTo(context, account.id) },
                shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, count),
                colors = com.pixelgram.android.ui.components.segmentedColors(),
                leadingContent = { com.pixelgram.android.ui.components.Avatar(account.name, account.userId, account.photo, null, 40.dp) },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
            ) { Text(account.name.ifBlank { stringResource(R.string.accounts_unnamed) }) }
        }
    }
    Spacer(Modifier.height(12.dp))
    SettingsGroup {
        SettingsItem(
            stringResource(R.string.settings_logout),
            SegmentPosition.Single,
            icon = Icons.AutoMirrored.Rounded.Logout,
            iconTint = MaterialTheme.colorScheme.error,
            onClick = { confirmLogout = true },
        )
    }
    Spacer(Modifier.height(32.dp))

    if (editingUsername) {
        var value by remember { mutableStateOf(me?.username.orEmpty()) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { editingUsername = false },
            title = { Text(stringResource(R.string.profile_username)) },
            text = {
                OutlinedTextField(value, { value = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(32) }, prefix = { Text("@") }, singleLine = true, isError = error != null, supportingText = error?.let { e -> { Text(e) } })
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { graph.td.send(TdApi.SetUsername(value)) }
                            .onSuccess { editingUsername = false }
                            .onFailure { error = (it as? com.pixelgram.android.td.TdException)?.error?.message ?: it.message }
                    }
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = { TextButton(onClick = { editingUsername = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (editingBirthday) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = full?.birthdate?.let { java.time.LocalDate.of(if (it.year > 0) it.year else 2000, it.month, it.day).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() },
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { editingBirthday = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    editingBirthday = false
                    scope.launch {
                        val birth = millis?.let {
                            val date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                            TdApi.Birthdate(date.dayOfMonth, date.monthValue, date.year)
                        }
                        runCatching { graph.td.send(TdApi.SetBirthdate(birth)) }
                        full = runCatching { graph.td.send(TdApi.GetUserFullInfo(snapshot.meId)) }.getOrNull() ?: full
                    }
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingBirthday = false
                    scope.launch {
                        runCatching { graph.td.send(TdApi.SetBirthdate(null)) }
                        full = runCatching { graph.td.send(TdApi.GetUserFullInfo(snapshot.meId)) }.getOrNull() ?: full
                    }
                }) { Text(stringResource(R.string.action_remove)) }
            },
        ) { androidx.compose.material3.DatePicker(state) }
    }
    if (nowPlaying) NowPlayingDialog { nowPlaying = false }
    if (personal) {
        val chats by produceState(emptyList<TdApi.Chat>()) {
            value = runCatching { graph.td.send(TdApi.GetSuitablePersonalChats()).chatIds.toList() }.getOrDefault(emptyList()).mapNotNull { id -> runCatching { graph.td.send(TdApi.GetChat(id)) }.getOrNull() }
        }
        AlertDialog(
            onDismissRequest = { personal = false },
            title = { Text(stringResource(R.string.account_personal_channel)) },
            text = {
                Column {
                    if (chats.isEmpty()) Text(stringResource(R.string.account_no_channels), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    chats.forEach { chat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {
                                personal = false
                                scope.launch {
                                    runCatching { graph.td.send(TdApi.SetPersonalChat(chat.id)) }
                                    full = runCatching { graph.td.send(TdApi.GetUserFullInfo(snapshot.meId)) }.getOrNull() ?: full
                                }
                            }).padding(vertical = 8.dp),
                        ) {
                            Avatar(chat.title, chat.id, chat.photo?.small?.id?.let { snapshot.files[it] }, chat.photo?.minithumbnail?.data, 36.dp, onNeedFile = { chat.photo?.small?.id?.let(graph.store::requestFileAsync) })
                            Spacer(Modifier.width(12.dp))
                            Text(chat.title)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { personal = false }) { Text(stringResource(R.string.action_cancel)) } },
            dismissButton = if ((full?.personalChatId ?: 0L) != 0L) ({
                TextButton(onClick = {
                    personal = false
                    scope.launch {
                        runCatching { graph.td.send(TdApi.SetPersonalChat(0)) }
                        full = runCatching { graph.td.send(TdApi.GetUserFullInfo(snapshot.meId)) }.getOrNull() ?: full
                    }
                }) { Text(stringResource(R.string.action_remove)) }
            }) else null,
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.settings_logout)) },
            text = { Text(stringResource(R.string.settings_logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    graph.scope.launch {
                        runCatching { graph.td.send(TdApi.LogOut()) }
                        kotlinx.coroutines.delay(1_500)
                        com.pixelgram.android.account.Accounts.forgetActive(context.applicationContext)
                    }
                }) { Text(stringResource(R.string.settings_logout), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun formatBirth(b: TdApi.Birthdate): String {
    val month = java.time.Month.of(b.month).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
    return if (b.year > 0) "$month ${b.day}, ${b.year}" else "$month ${b.day}"
}

@Composable
private fun serviceLabel(service: String): String = when (service) {
    NowPlayingPrefs.LastFm -> "Last.fm"
    NowPlayingPrefs.StatsFm -> "Stats.fm"
    else -> stringResource(R.string.privacy_off)
}

@Composable
fun NowPlayingDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var service by remember { mutableStateOf(NowPlayingPrefs.service(context)) }
    var username by remember { mutableStateOf(NowPlayingPrefs.username(context)) }
    var key by remember { mutableStateOf(NowPlayingPrefs.apiKey(context)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.now_playing_title)) },
        text = {
            Column {
                listOf(NowPlayingPrefs.None, NowPlayingPrefs.LastFm, NowPlayingPrefs.StatsFm).forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().combinedClickable(interactionSource = null, indication = null) { service = option }) {
                        RadioButton(service == option, { service = option })
                        Text(serviceLabel(option))
                    }
                }
                Text(stringResource(R.string.now_playing_about), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                if (service != NowPlayingPrefs.None) {
                    OutlinedTextField(username, { username = it.take(43) }, label = { Text(stringResource(R.string.now_playing_username)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (service == NowPlayingPrefs.LastFm) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(key, { key = it.take(64) }, label = { Text(stringResource(R.string.now_playing_key)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text(
                            buildAnnotatedString {
                                append(stringResource(R.string.now_playing_key_note) + " ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("last.fm/api/account/create") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp).combinedClickable(interactionSource = null, indication = null) { com.pixelgram.android.nav.PendingNav.external = "https://www.last.fm/api/account/create" },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                NowPlayingPrefs.save(context, service, username, key)
                com.pixelgram.android.feature.profile.NowPlayingFeed.reset()
                onDismiss()
            }) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
