package com.pixelgram.android.feature.security

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.SessionsKey
import com.pixelgram.android.td.SensitiveRequests
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.SettingsSwitch
import com.pixelgram.android.ui.components.segmentPosition
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

private enum class Base { Everybody, Contacts, Nobody, Premium }

private class PrivacyKind(val setting: () -> TdApi.UserPrivacySetting, val title: Int, val question: Int, val nobody: Boolean = true, val premium: Boolean = false)

private val Kinds = listOf(
    PrivacyKind({ TdApi.UserPrivacySettingShowPhoneNumber() }, R.string.privacy_phone, R.string.privacy_phone_q),
    PrivacyKind({ TdApi.UserPrivacySettingShowStatus() }, R.string.privacy_status, R.string.privacy_status_q),
    PrivacyKind({ TdApi.UserPrivacySettingShowProfilePhoto() }, R.string.privacy_photos, R.string.privacy_photos_q),
    PrivacyKind({ TdApi.UserPrivacySettingShowLinkInForwardedMessages() }, R.string.privacy_forwards, R.string.privacy_forwards_q),
    PrivacyKind({ TdApi.UserPrivacySettingAllowCalls() }, R.string.privacy_calls, R.string.privacy_calls_q),
    PrivacyKind({ TdApi.UserPrivacySettingAllowPrivateVoiceAndVideoNoteMessages() }, R.string.privacy_voice, R.string.privacy_voice_q),
    PrivacyKind({ TdApi.UserPrivacySettingShowBirthdate() }, R.string.privacy_birthday, R.string.privacy_birthday_q),
    PrivacyKind({ TdApi.UserPrivacySettingShowBio() }, R.string.privacy_bio, R.string.privacy_bio_q),
    PrivacyKind({ TdApi.UserPrivacySettingShowProfileAudio() }, R.string.privacy_music, R.string.privacy_music_q),
    PrivacyKind({ TdApi.UserPrivacySettingAllowChatInvites() }, R.string.privacy_invites, R.string.privacy_invites_q, premium = true),
)

private class ParsedRules(val base: Base, val allow: List<Long>, val deny: List<Long>)

private fun parse(rules: TdApi.UserPrivacySettingRules?): ParsedRules {
    var base = Base.Nobody
    val allow = ArrayList<Long>()
    val deny = ArrayList<Long>()
    rules?.rules?.forEach { rule ->
        when (rule) {
            is TdApi.UserPrivacySettingRuleAllowAll -> base = Base.Everybody
            is TdApi.UserPrivacySettingRuleAllowContacts -> if (base != Base.Everybody) base = Base.Contacts
            is TdApi.UserPrivacySettingRuleAllowPremiumUsers -> if (base == Base.Nobody) base = Base.Premium
            is TdApi.UserPrivacySettingRuleAllowUsers -> allow += rule.userIds.toList()
            is TdApi.UserPrivacySettingRuleAllowChatMembers -> allow += rule.chatIds.toList()
            is TdApi.UserPrivacySettingRuleRestrictUsers -> deny += rule.userIds.toList()
            is TdApi.UserPrivacySettingRuleRestrictChatMembers -> deny += rule.chatIds.toList()
            else -> Unit
        }
    }
    return ParsedRules(base, allow, deny)
}

private fun build(base: Base, allow: List<Long>, deny: List<Long>): TdApi.UserPrivacySettingRules {
    val list = ArrayList<TdApi.UserPrivacySettingRule>()
    val allowUsers = allow.filter { it > 0 }.toLongArray()
    val allowChats = allow.filter { it < 0 }.toLongArray()
    val denyUsers = deny.filter { it > 0 }.toLongArray()
    val denyChats = deny.filter { it < 0 }.toLongArray()
    if (base != Base.Everybody) {
        if (allowUsers.isNotEmpty()) list += TdApi.UserPrivacySettingRuleAllowUsers(allowUsers)
        if (allowChats.isNotEmpty()) list += TdApi.UserPrivacySettingRuleAllowChatMembers(allowChats)
    }
    if (base != Base.Nobody) {
        if (denyUsers.isNotEmpty()) list += TdApi.UserPrivacySettingRuleRestrictUsers(denyUsers)
        if (denyChats.isNotEmpty()) list += TdApi.UserPrivacySettingRuleRestrictChatMembers(denyChats)
    }
    when (base) {
        Base.Everybody -> list += TdApi.UserPrivacySettingRuleAllowAll()
        Base.Contacts -> list += TdApi.UserPrivacySettingRuleAllowContacts()
        Base.Premium -> {
            list += TdApi.UserPrivacySettingRuleAllowContacts()
            list += TdApi.UserPrivacySettingRuleAllowPremiumUsers()
        }
        Base.Nobody -> list += TdApi.UserPrivacySettingRuleRestrictAll()
    }
    return TdApi.UserPrivacySettingRules(list.toTypedArray())
}

@Composable
private fun baseLabel(base: Base): String = stringResource(when (base) {
    Base.Everybody -> R.string.privacy_everybody
    Base.Contacts -> R.string.privacy_contacts
    Base.Nobody -> R.string.privacy_nobody
    Base.Premium -> R.string.privacy_premium
})

@Composable
private fun summary(parsed: ParsedRules?): String {
    if (parsed == null) return "…"
    val extra = listOfNotNull(parsed.deny.size.takeIf { it > 0 }?.let { "-$it" }, parsed.allow.size.takeIf { it > 0 }?.let { "+$it" })
    return baseLabel(parsed.base) + if (extra.isEmpty()) "" else " (${extra.joinToString(", ")})"
}

@Composable
fun PrivacySection(graph: AppGraph, navigator: Navigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reload by remember { mutableStateOf(0) }
    val rules by produceState<Map<Int, ParsedRules>>(emptyMap(), reload) {
        val out = HashMap<Int, ParsedRules>()
        Kinds.forEachIndexed { i, kind -> out[i] = parse(runCatching { graph.td.send(TdApi.GetUserPrivacySettingRules(kind.setting())) }.getOrNull()) }
        value = out
    }
    val password by produceState<TdApi.PasswordState?>(null) { value = runCatching { graph.td.send(TdApi.GetPasswordState()) }.getOrNull() }
    var autoDelete by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { autoDelete = runCatching { graph.td.send(TdApi.GetDefaultMessageAutoDeleteTime()).time }.getOrNull() }
    val blocked by produceState<Int?>(null, reload) { value = runCatching { graph.td.send(TdApi.GetBlockedMessageSenders(TdApi.BlockListMain(), 0, 1)).totalCount }.getOrNull() }
    val sessions by produceState<Int?>(null) { value = runCatching { graph.td.send(TdApi.GetActiveSessions()).sessions.size }.getOrNull() }
    var archive by remember { mutableStateOf<TdApi.ArchiveChatListSettings?>(null) }
    LaunchedEffect(Unit) { archive = runCatching { graph.td.send(TdApi.GetArchiveChatListSettings()) }.getOrNull() }
    var ttl by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { ttl = runCatching { graph.td.send(TdApi.GetAccountTtl()).days }.getOrNull() }
    var confirm by remember { mutableStateOf(SensitiveRequests.enabled(context)) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var pickAutoDelete by remember { mutableStateOf(false) }
    var pickTtl by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }
    var showWebsites by remember { mutableStateOf(false) }
    var passcode by remember { mutableStateOf(false) }
    val on = stringResource(R.string.privacy_on)
    val off = stringResource(R.string.privacy_off)
    val value: @Composable (String) -> Unit = { text -> Text(text, color = MaterialTheme.colorScheme.primary, maxLines = 1) }

    SettingsGroup(stringResource(R.string.section_security)) {
        SettingsItem(stringResource(R.string.privacy_2fa), segmentPosition(0, 7), icon = Icons.Rounded.VerifiedUser, trailing = { value(if (password?.hasPassword == true) on else off) }, onClick = {})
        SettingsItem(stringResource(R.string.privacy_auto_delete), segmentPosition(1, 7), icon = Icons.Rounded.Timer, trailing = { value(autoDeleteLabel(autoDelete ?: 0)) }, onClick = { pickAutoDelete = true })
        SettingsItem(stringResource(R.string.privacy_passcode), segmentPosition(2, 7), icon = Icons.Rounded.Lock, trailing = { value(if (PasscodeLock.enabled(context)) on else off) }, onClick = { passcode = true })
        SettingsItem(stringResource(R.string.privacy_blocked), segmentPosition(3, 7), icon = Icons.Rounded.Block, trailing = { value(blocked?.toString() ?: "…") }, onClick = { showBlocked = true })
        SettingsItem(stringResource(R.string.settings_devices), segmentPosition(4, 7), icon = Icons.Rounded.Devices, trailing = { value(sessions?.toString() ?: "…") }, onClick = { navigator.open(SessionsKey) })
        SettingsItem(stringResource(R.string.privacy_websites), segmentPosition(5, 7), icon = Icons.Rounded.Shield, onClick = { showWebsites = true })
        SettingsSwitch(stringResource(R.string.sensitive_setting), confirm, { v ->
            confirm = v
            SensitiveRequests.setEnabled(context, v)
        }, segmentPosition(6, 7), stringResource(R.string.sensitive_setting_sub))
    }
    SettingsGroup(stringResource(R.string.privacy_title)) {
        Kinds.forEachIndexed { i, kind ->
            SettingsItem(stringResource(kind.title), segmentPosition(i, Kinds.size), trailing = { value(summary(rules[i])) }, onClick = { editing = i })
        }
    }
    SettingsGroup(stringResource(R.string.privacy_unknown_header)) {
        SettingsSwitch(
            stringResource(R.string.privacy_archive_mute),
            archive?.archiveAndMuteNewChatsFromUnknownUsers == true,
            { v ->
                val current = archive ?: TdApi.ArchiveChatListSettings(false, false, false)
                val next = TdApi.ArchiveChatListSettings(v, current.keepUnmutedChatsArchived, current.keepChatsFromFoldersArchived)
                archive = next
                scope.launch { runCatching { graph.td.send(TdApi.SetArchiveChatListSettings(next)) } }
            },
            com.pixelgram.android.ui.components.SegmentPosition.Single,
            stringResource(R.string.privacy_archive_mute_sub),
        )
    }
    SettingsGroup(stringResource(R.string.privacy_delete_header)) {
        SettingsItem(stringResource(R.string.privacy_if_away), com.pixelgram.android.ui.components.SegmentPosition.Single, trailing = { value(ttl?.let { ttlLabel(it) } ?: "…") }, onClick = { pickTtl = true })
    }
    SettingsGroup(stringResource(R.string.privacy_contacts_header)) {
        SettingsItem(stringResource(R.string.privacy_clear_payment), segmentPosition(0, 2), onClick = {
            scope.launch {
                runCatching { graph.td.send(TdApi.DeleteSavedOrderInfo()) }
                runCatching { graph.td.send(TdApi.DeleteSavedCredentials()) }
                android.widget.Toast.makeText(context, context.getString(R.string.privacy_done), android.widget.Toast.LENGTH_SHORT).show()
            }
        })
        SettingsItem(stringResource(R.string.privacy_delete_synced), segmentPosition(1, 2), onClick = {
            scope.launch {
                runCatching { graph.td.send(TdApi.ClearImportedContacts()) }
                android.widget.Toast.makeText(context, context.getString(R.string.privacy_done), android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }
    Spacer(Modifier.height(24.dp))

    editing?.let { index ->
        val kind = Kinds[index]
        PrivacyRuleEditor(graph, kind, rules[index] ?: ParsedRules(Base.Everybody, emptyList(), emptyList()), onDismiss = { editing = null }) { saved ->
            editing = null
            scope.launch {
                runCatching { graph.td.send(TdApi.SetUserPrivacySettingRules(kind.setting(), saved)) }
                reload++
            }
        }
    }
    if (pickAutoDelete) ChoiceDialog(stringResource(R.string.privacy_auto_delete), listOf(0, 86400, 604800, 2678400), autoDelete ?: 0, { autoDeleteLabel(it) }, { pickAutoDelete = false }) { picked ->
        pickAutoDelete = false
        autoDelete = picked
        scope.launch { runCatching { graph.td.send(TdApi.SetDefaultMessageAutoDeleteTime(TdApi.MessageAutoDeleteTime(picked))) } }
    }
    if (pickTtl) ChoiceDialog(stringResource(R.string.privacy_if_away), listOf(30, 90, 180, 365, 548, 730), ttl ?: 365, { ttlLabel(it) }, { pickTtl = false }) { picked ->
        pickTtl = false
        ttl = picked
        scope.launch { runCatching { graph.td.send(TdApi.SetAccountTtl(TdApi.AccountTtl(picked))) } }
    }
    if (showBlocked) BlockedUsersDialog(graph, onDismiss = { showBlocked = false; reload++ })
    if (showWebsites) WebsitesDialog(graph) { showWebsites = false }
    if (passcode) PasscodeSettingsDialog { passcode = false }
}

@Composable
private fun autoDeleteLabel(seconds: Int): String = when (seconds) {
    0 -> stringResource(R.string.privacy_off)
    86400 -> stringResource(R.string.privacy_day)
    604800 -> stringResource(R.string.privacy_week)
    else -> stringResource(R.string.privacy_month)
}

@Composable
private fun ttlLabel(days: Int): String = when {
    days >= 365 -> stringResource(R.string.privacy_months, days / 30)
    else -> stringResource(R.string.privacy_months, (days / 30).coerceAtLeast(1))
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(option) }.padding(vertical = 4.dp)) {
                        RadioButton(option == selected, { onPick(option) })
                        Text(label(option))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullDialog(title: String, onDismiss: () -> Unit, fab: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
                )
            },
            floatingActionButton = { fab?.invoke() },
        ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { content() } }
    }
}

@Composable
private fun PrivacyRuleEditor(graph: AppGraph, kind: PrivacyKind, initial: ParsedRules, onDismiss: () -> Unit, onSave: (TdApi.UserPrivacySettingRules) -> Unit) {
    var base by remember { mutableStateOf(initial.base) }
    val allow = remember { mutableStateListOf<Long>().apply { addAll(initial.allow) } }
    val deny = remember { mutableStateListOf<Long>().apply { addAll(initial.deny) } }
    var picking by remember { mutableStateOf<Boolean?>(null) }
    FullDialog(stringResource(kind.title), { onSave(build(base, allow, deny)) }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val options = buildList {
                add(Base.Everybody)
                add(Base.Contacts)
                if (kind.premium) add(Base.Premium)
                if (kind.nobody) add(Base.Nobody)
            }
            SettingsGroup(stringResource(kind.question)) {
                options.forEachIndexed { i, option ->
                    SettingsItem(baseLabel(option), segmentPosition(i, options.size), trailing = { RadioButton(base == option, { base = option }) }, onClick = { base = option })
                }
            }
            SettingsGroup(stringResource(R.string.privacy_exceptions)) {
                val rows = buildList {
                    if (base != Base.Everybody) add(true)
                    if (base != Base.Nobody) add(false)
                }
                rows.forEachIndexed { i, allowRow ->
                    val count = if (allowRow) allow.size else deny.size
                    SettingsItem(
                        stringResource(if (allowRow) R.string.privacy_always_allow else R.string.privacy_never_allow),
                        segmentPosition(i, rows.size),
                        trailing = { Text(if (count == 0) stringResource(R.string.privacy_add_users) else stringResource(R.string.privacy_users_count, count), color = MaterialTheme.colorScheme.primary) },
                        onClick = { picking = allowRow },
                    )
                }
            }
            Text(stringResource(R.string.privacy_exceptions_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
            Spacer(Modifier.height(96.dp))
        }
    }
    picking?.let { allowRow ->
        val target = if (allowRow) allow else deny
        PeoplePicker(graph, stringResource(if (allowRow) R.string.privacy_always_allow else R.string.privacy_never_allow), target.toList(), onDismiss = { picking = null }) { picked ->
            target.clear()
            target.addAll(picked)
            (if (allowRow) deny else allow).removeAll(picked.toSet())
            picking = null
        }
    }
}

private class Candidate(val id: Long, val title: String, val subtitle: String, val photo: Int, val local: String?, val mini: ByteArray?)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PeoplePicker(graph: AppGraph, title: String, initial: List<Long>, onDismiss: () -> Unit, everything: Boolean = false, onDone: (List<Long>) -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    val selected = remember { mutableStateListOf<Long>().apply { addAll(initial) } }
    var query by remember { mutableStateOf("") }
    val contacts by produceState(emptyList<Long>()) { value = runCatching { graph.td.send(TdApi.GetContacts()).userIds.toList() }.getOrDefault(emptyList()) }
    val candidates = remember(contacts, snapshot, initial) {
        val users = (contacts + initial.filter { it > 0 }).distinct().mapNotNull { id ->
            snapshot.users[id]?.let { Candidate(id, it.name, it.username?.let { u -> "@$u" }.orEmpty(), it.photoFileId, it.localPhoto, it.minithumb) }
        }
        val groups = snapshot.lists[com.pixelgram.android.td.LIST_MAIN].orEmpty().filter { if (everything) it.userId == 0L || it.userId !in contacts else it.kind == com.pixelgram.android.td.ChatKind.Group }.map { row ->
            Candidate(row.id, row.title, "", row.photoFileId, row.photoPath, row.minithumb)
        }
        if (everything) (groups + users.map { u -> Candidate(snapshot.lists[com.pixelgram.android.td.LIST_MAIN].orEmpty().firstOrNull { it.userId == u.id }?.id ?: u.id, u.title, u.subtitle, u.photo, u.local, u.mini) }).distinctBy { it.id }.sortedBy { it.title.lowercase() }
        else (users + groups).sortedBy { it.title.lowercase() }
    }
    val shown = candidates.filter { query.isBlank() || it.title.contains(query, true) || it.subtitle.contains(query, true) }
    FullDialog(title, onDismiss, fab = {
        FloatingActionButton(onClick = { onDone(selected.toList()) }) { Icon(Icons.Rounded.Check, stringResource(R.string.action_done)) }
    }) {
        Column(Modifier.fillMaxSize()) {
            val chips = remember { mutableStateListOf<Long>().apply { addAll(initial) } }
            androidx.compose.runtime.LaunchedEffect(selected.toList()) { selected.forEach { if (it !in chips) chips.add(it) } }
            var focused by remember { mutableStateOf<Long?>(null) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()).padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                chips.forEach { id ->
                    androidx.compose.animation.AnimatedVisibility(
                        visible = id in selected,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.6f),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.6f) + androidx.compose.animation.shrinkHorizontally(),
                    ) {
                        val c = candidates.firstOrNull { it.id == id }
                        val active = focused == id
                        val bg by androidx.compose.animation.animateColorAsState(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer, label = "chip")
                        val fg by androidx.compose.animation.animateColorAsState(if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer, label = "chip-fg")
                        Surface(
                            onClick = {
                                if (active) {
                                    selected.remove(id)
                                    focused = null
                                } else {
                                    focused = id
                                }
                            },
                            shape = CircleShape,
                            color = bg,
                            contentColor = fg,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                                Box(Modifier.size(28.dp)) {
                                    Avatar(c?.title.orEmpty(), id, c?.local ?: c?.photo?.let { snapshot.files[it] }, c?.mini, 28.dp)
                                    androidx.compose.animation.AnimatedVisibility(active, enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(), exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()) {
                                        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Close, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(c?.title ?: id.toString(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = IntrinsicWidth))
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                OutlinedTextField(query, { query = it }, placeholder = { Text(stringResource(R.string.privacy_search)) }, singleLine = true, shape = CircleShape, modifier = Modifier.weight(1f))
                if (selected.isNotEmpty()) TextButton(onClick = { selected.clear() }) { Text(stringResource(R.string.privacy_deselect_all)) }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
                item { Spacer(Modifier.height(8.dp)) }
                itemsIndexed(shown, key = { _, it -> it.id }) { index, c ->
                    val checked = c.id in selected
                    androidx.compose.material3.SegmentedListItem(
                        onClick = { if (checked) selected.remove(c.id) else selected.add(c.id) },
                        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, shown.size),
                        colors = com.pixelgram.android.ui.components.segmentedColors(),
                        leadingContent = {
                            Box {
                                Avatar(c.title, c.id, c.local ?: snapshot.files[c.photo], c.mini, 44.dp, onNeedFile = { if (c.photo != 0) graph.store.requestFileAsync(c.photo) })
                                androidx.compose.animation.AnimatedVisibility(checked, Modifier.align(Alignment.BottomEnd), enter = androidx.compose.animation.scaleIn(), exit = androidx.compose.animation.scaleOut()) {
                                    Box(Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        },
                        supportingContent = c.subtitle.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                    ) { Text(c.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}

private val IntrinsicWidth = 120.dp

@Composable
private fun BlockedUsersDialog(graph: AppGraph, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snapshot by graph.store.snapshot.collectAsState()
    var reload by remember { mutableStateOf(0) }
    val senders by produceState(emptyList<TdApi.MessageSender>(), reload) {
        value = runCatching { graph.td.send(TdApi.GetBlockedMessageSenders(TdApi.BlockListMain(), 0, 100)).senders.toList() }.getOrDefault(emptyList())
    }
    FullDialog(stringResource(R.string.privacy_blocked), onDismiss) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(senders.size) { i ->
                val sender = senders[i]
                val id = (sender as? TdApi.MessageSenderUser)?.userId ?: (sender as? TdApi.MessageSenderChat)?.chatId ?: 0L
                val user = snapshot.users[id]
                if (user == null && sender is TdApi.MessageSenderUser) LaunchedEffect(id) { runCatching { graph.td.send(TdApi.GetUser(id)) } }
                val name = user?.name ?: graph.store.chat(id)?.title ?: id.toString()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Avatar(name, id, user?.localPhoto ?: user?.photoFileId?.let { snapshot.files[it] }, user?.minithumb, 44.dp, onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) })
                    Spacer(Modifier.width(14.dp))
                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { graph.td.send(TdApi.SetMessageSenderBlockList(sender, null)) }
                            reload++
                        }
                    }) { Text(stringResource(R.string.privacy_unblock)) }
                }
            }
        }
    }
}

@Composable
private fun WebsitesDialog(graph: AppGraph, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var reload by remember { mutableStateOf(0) }
    val sites by produceState(emptyList<TdApi.ConnectedWebsite>(), reload) {
        value = runCatching { graph.td.send(TdApi.GetConnectedWebsites()).websites.toList() }.getOrDefault(emptyList())
    }
    FullDialog(stringResource(R.string.privacy_websites), onDismiss) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (sites.isEmpty()) item { Text(stringResource(R.string.privacy_no_websites), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
            items(sites, key = { it.id }) { site ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(site.domainName)
                        Text("${site.browser}, ${site.platform} · ${site.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { graph.td.send(TdApi.DisconnectWebsite(site.id)) }
                            reload++
                        }
                    }) { Text(stringResource(R.string.privacy_disconnect)) }
                }
            }
        }
    }
}
