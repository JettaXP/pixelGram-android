package com.pixelgram.android.feature.admin

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.ManageChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.td.StoreSnapshot
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.PixelSwitch
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.segmentPosition
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.text.DateFormat
import java.util.Date

object AdminPage {
    const val Main = ""
    const val Admins = "admins"
    const val Members = "members"
    const val Banned = "banned"
    const val Links = "links"
    const val Requests = "requests"
    const val Log = "log"
    const val Permissions = "permissions"
}

fun canManage(status: TdApi.ChatMemberStatus?): Boolean = status is TdApi.ChatMemberStatusCreator ||
    (status as? TdApi.ChatMemberStatusAdministrator)?.rights?.let { it.canChangeInfo || it.canRestrictMembers || it.canInviteUsers || it.canPromoteMembers || it.canManageChat } == true

private class ChatState(val chat: TdApi.Chat?, val group: TdApi.Supergroup?, val full: TdApi.SupergroupFullInfo?)

@Composable
private fun rememberChatState(graph: AppGraph, chatId: Long, reload: Int): ChatState {
    val state by produceState(ChatState(graph.store.chat(chatId), null, null), chatId, reload) {
        val chat = runCatching { graph.td.send(TdApi.GetChat(chatId)) }.getOrNull() ?: graph.store.chat(chatId)
        val id = (chat?.type as? TdApi.ChatTypeSupergroup)?.supergroupId ?: 0L
        val group = if (id != 0L) runCatching { graph.td.send(TdApi.GetSupergroup(id)) }.getOrNull() else null
        val full = if (id != 0L) runCatching { graph.td.send(TdApi.GetSupergroupFullInfo(id)) }.getOrNull() else null
        value = ChatState(chat, group, full)
    }
    return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminScaffold(title: String, navigator: Navigator, fab: (@Composable () -> Unit)? = null, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
        floatingActionButton = { fab?.invoke() },
    ) { padding -> content(Modifier.fillMaxSize().padding(padding)) }
}

@Composable
fun ManageChatScreen(graph: AppGraph, chatId: Long, page: String, navigator: Navigator) {
    when (page) {
        AdminPage.Admins, AdminPage.Members, AdminPage.Banned -> MembersPage(graph, chatId, page, navigator)
        AdminPage.Links -> LinksPage(graph, chatId, navigator)
        AdminPage.Requests -> RequestsPage(graph, chatId, navigator)
        AdminPage.Log -> LogPage(graph, chatId, navigator)
        AdminPage.Permissions -> PermissionsPage(graph, chatId, navigator)
        else -> MainPage(graph, chatId, navigator)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainPage(graph: AppGraph, chatId: Long, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var reload by remember { mutableIntStateOf(0) }
    val state = rememberChatState(graph, chatId, reload)
    val chat = state.chat
    val group = state.group
    val full = state.full
    val channel = group?.isChannel == true
    val owner = group?.status is TdApi.ChatMemberStatusCreator
    val rights = (group?.status as? TdApi.ChatMemberStatusAdministrator)?.rights
    val canInfo = owner || rights?.canChangeInfo == true
    val canRestrict = owner || rights?.canRestrictMembers == true
    val canInvite = owner || rights?.canInviteUsers == true
    var title by remember(chat?.title) { mutableStateOf(chat?.title.orEmpty()) }
    var about by remember(full?.description) { mutableStateOf(full?.description.orEmpty()) }
    var username by remember(group?.usernames) { mutableStateOf(group?.usernames?.editableUsername.orEmpty()) }
    var typeSheet by remember { mutableStateOf(false) }
    var reactionsSheet by remember { mutableStateOf(false) }
    var timerSheet by remember { mutableStateOf(false) }
    var slowSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val run: (TdApi.Function<*>) -> Unit = { fn ->
        scope.launch {
            runCatching { graph.td.send(fn) }.onFailure { error = it.message }
            reload++
        }
    }
    DisposableEffect(chatId) {
        onDispose {
            val c = chat ?: return@onDispose
            if (canInfo && title.isNotBlank() && title.trim() != c.title) graph.scope.launch { runCatching { graph.td.send(TdApi.SetChatTitle(chatId, title.trim())) } }
            if (canInfo && full != null && about.trim() != full.description) graph.scope.launch { runCatching { graph.td.send(TdApi.SetChatDescription(chatId, about.trim())) } }
        }
    }
    AdminScaffold(stringResource(if (channel) R.string.admin_manage_channel else R.string.admin_manage_group), navigator) { modifier ->
        if (chat == null) {
            Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@AdminScaffold
        }
        Column(modifier.verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                val snapshot by graph.store.snapshot.collectAsState()
                Avatar(chat.title, chatId, chat.photo?.small?.id?.let { snapshot.files[it] }, chat.photo?.minithumbnail?.data, 64.dp, onNeedFile = { chat.photo?.small?.id?.let(graph.store::requestFileAsync) })
                Spacer(Modifier.width(14.dp))
                OutlinedTextField(title, { title = it.take(128) }, enabled = canInfo, label = { Text(stringResource(R.string.admin_name)) }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f))
            }
            OutlinedTextField(
                about,
                { about = it.take(255) },
                enabled = canInfo,
                label = { Text(stringResource(R.string.admin_description)) },
                supportingText = { Text("${about.length}/255") },
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp)) }
            if (group != null) {
                val items = buildList<@Composable (Int, Int) -> Unit> {
                    if (canInfo) add { i, n ->
                        val public = group.usernames?.activeUsernames?.isNotEmpty() == true
                        SettingsItem(
                            stringResource(if (channel) R.string.admin_channel_type else R.string.admin_group_type),
                            segmentPosition(i, n),
                            subtitle = if (public) "t.me/" + group.usernames!!.activeUsernames.first() else stringResource(R.string.admin_private),
                            icon = if (public) Icons.Rounded.Public else Icons.Rounded.Lock,
                            onClick = { typeSheet = true },
                        )
                    }
                    if (canInvite) add { i, n ->
                        SettingsItem(stringResource(R.string.admin_invite_links), segmentPosition(i, n), subtitle = full?.inviteLink?.inviteLink?.removePrefix("https://"), icon = Icons.Rounded.Link, onClick = { navigator.open(ManageChatKey(chatId, AdminPage.Links)) })
                    }
                    if (canInfo) add { i, n ->
                        SettingsItem(stringResource(R.string.admin_reactions), segmentPosition(i, n), subtitle = reactionsLabel(chat.availableReactions), icon = Icons.Rounded.HowToReg, onClick = { reactionsSheet = true })
                    }
                    if (canInfo) add { i, n ->
                        SettingsItem(stringResource(R.string.admin_auto_delete), segmentPosition(i, n), subtitle = timerLabel(chat.messageAutoDeleteTime), icon = Icons.Rounded.Timer, onClick = { timerSheet = true })
                    }
                }
                if (items.isNotEmpty()) SettingsGroup { items.forEachIndexed { i, item -> item(i, items.size) } }
                SettingsGroup(stringResource(R.string.admin_settings)) {
                    val toggles = buildList<@Composable (Int, Int) -> Unit> {
                        if (channel && canInfo) add { i, n ->
                            SettingsItem(stringResource(R.string.admin_sign_messages), segmentPosition(i, n), onClick = { run(TdApi.ToggleSupergroupSignMessages(group.id, !group.signMessages, group.showMessageSender)) }) {
                                PixelSwitch(group.signMessages, { run(TdApi.ToggleSupergroupSignMessages(group.id, it, group.showMessageSender)) })
                            }
                        }
                        if (!channel && canInfo) add { i, n ->
                            SettingsItem(stringResource(R.string.admin_history), segmentPosition(i, n), subtitle = stringResource(R.string.admin_history_sub), onClick = { run(TdApi.ToggleSupergroupIsAllHistoryAvailable(group.id, full?.isAllHistoryAvailable != true)) }) {
                                PixelSwitch(full?.isAllHistoryAvailable == true, { run(TdApi.ToggleSupergroupIsAllHistoryAvailable(group.id, it)) })
                            }
                        }
                        if (!channel && owner) add { i, n ->
                            SettingsItem(stringResource(R.string.admin_topics), segmentPosition(i, n), subtitle = stringResource(R.string.admin_topics_sub), icon = Icons.Rounded.Forum, onClick = { run(TdApi.ToggleSupergroupIsForum(group.id, !group.isForum, group.hasForumTabs)) }) {
                                PixelSwitch(group.isForum, { run(TdApi.ToggleSupergroupIsForum(group.id, it, group.hasForumTabs)) })
                            }
                        }
                        if (canInfo) add { i, n ->
                            SettingsItem(stringResource(R.string.admin_protect), segmentPosition(i, n), subtitle = stringResource(R.string.admin_protect_sub), onClick = { run(TdApi.ToggleChatHasProtectedContent(chatId, !chat.hasProtectedContent)) }) {
                                PixelSwitch(chat.hasProtectedContent, { run(TdApi.ToggleChatHasProtectedContent(chatId, it)) })
                            }
                        }
                        if (!channel && canRestrict) add { i, n ->
                            SettingsItem(stringResource(R.string.admin_slow_mode), segmentPosition(i, n), subtitle = timerLabel(full?.slowModeDelay ?: 0), icon = Icons.Rounded.Timer, onClick = { slowSheet = true })
                        }
                    }
                    toggles.forEachIndexed { i, item -> item(i, toggles.size) }
                }
                SettingsGroup(stringResource(R.string.admin_people)) {
                    val rows = buildList<@Composable (Int, Int) -> Unit> {
                        if (!channel && canRestrict) add { i, n ->
                            SettingsItem(stringResource(R.string.admin_permissions), segmentPosition(i, n), icon = Icons.Rounded.Shield, onClick = { navigator.open(ManageChatKey(chatId, AdminPage.Permissions)) })
                        }
                        add { i, n ->
                            SettingsItem(stringResource(R.string.admin_admins), segmentPosition(i, n), subtitle = full?.administratorCount?.toString(), icon = Icons.Rounded.AdminPanelSettings, onClick = { navigator.open(ManageChatKey(chatId, AdminPage.Admins)) })
                        }
                        add { i, n ->
                            SettingsItem(stringResource(if (channel) R.string.admin_subscribers else R.string.admin_members), segmentPosition(i, n), subtitle = (full?.memberCount ?: group.memberCount).toString(), icon = Icons.Rounded.Group, onClick = { navigator.open(ManageChatKey(chatId, AdminPage.Members)) })
                        }
                        if (canInvite && (chat.pendingJoinRequests?.totalCount ?: 0) > 0 || group.joinByRequest) add { i, n ->
                            SettingsItem(stringResource(R.string.admin_requests), segmentPosition(i, n), subtitle = (chat.pendingJoinRequests?.totalCount ?: 0).toString(), icon = Icons.Rounded.HowToReg, onClick = { navigator.open(ManageChatKey(chatId, AdminPage.Requests)) })
                        }
                        if (canRestrict) add { i, n ->
                            SettingsItem(stringResource(R.string.admin_banned), segmentPosition(i, n), subtitle = full?.bannedCount?.toString(), icon = Icons.Rounded.Block, onClick = { navigator.open(ManageChatKey(chatId, AdminPage.Banned)) })
                        }
                        add { i, n ->
                            SettingsItem(stringResource(R.string.admin_log), segmentPosition(i, n), icon = Icons.AutoMirrored.Rounded.ReceiptLong, onClick = { navigator.open(ManageChatKey(chatId, AdminPage.Log)) })
                        }
                    }
                    rows.forEachIndexed { i, item -> item(i, rows.size) }
                }
                if (owner) {
                    SettingsGroup {
                        SettingsItem(
                            stringResource(if (channel) R.string.admin_delete_channel else R.string.admin_delete_group),
                            segmentPosition(0, 1),
                            icon = Icons.Rounded.Delete,
                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                            iconBackground = MaterialTheme.colorScheme.errorContainer,
                            onClick = { confirmDelete = true },
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (typeSheet && group != null) {
        ChoiceSheet(stringResource(if (channel) R.string.admin_channel_type else R.string.admin_group_type), onDismiss = { typeSheet = false }) {
            var public by remember { mutableStateOf(group.usernames?.activeUsernames?.isNotEmpty() == true) }
            listOf(false, true).forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(public == option, { public = option })
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (option) R.string.admin_public else R.string.admin_private))
                        Text(stringResource(if (option) R.string.admin_public_sub else R.string.admin_private_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (public) OutlinedTextField(username, { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(32) }, prefix = { Text("t.me/") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            else full?.inviteLink?.inviteLink?.let { link ->
                SegmentedListItem(
                    onClick = {
                        context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText(null, link))
                    },
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    colors = com.pixelgram.android.ui.components.segmentedColors(),
                    trailingContent = { Icon(Icons.Rounded.ContentCopy, null) },
                ) { Text(link, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Button(onClick = {
                typeSheet = false
                run(TdApi.SetSupergroupUsername(group.id, if (public) username.trim() else ""))
            }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.action_save)) }
        }
    }
    if (reactionsSheet && chat != null) {
        ChoiceSheet(stringResource(R.string.admin_reactions), onDismiss = { reactionsSheet = false }) {
            val current = when (val r = chat.availableReactions) {
                is TdApi.ChatAvailableReactionsAll -> 0
                is TdApi.ChatAvailableReactionsSome -> if (r.reactions.isEmpty()) 2 else 1
                else -> 0
            }
            listOf(R.string.admin_reactions_all, R.string.admin_reactions_basic, R.string.admin_reactions_none).forEachIndexed { index, label ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(current == index, {
                        reactionsSheet = false
                        val value: TdApi.ChatAvailableReactions = when (index) {
                            0 -> TdApi.ChatAvailableReactionsAll(11)
                            1 -> TdApi.ChatAvailableReactionsSome(BasicReactions.map { TdApi.ReactionTypeEmoji(it) }.toTypedArray(), 11)
                            else -> TdApi.ChatAvailableReactionsSome(emptyArray(), 0)
                        }
                        run(TdApi.SetChatAvailableReactions(chatId, value))
                    })
                    Text(stringResource(label))
                }
            }
        }
    }
    if (timerSheet && chat != null) {
        ChoiceSheet(stringResource(R.string.admin_auto_delete), onDismiss = { timerSheet = false }) {
            listOf(0, 86400, 604800, 2592000).forEach { seconds ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(chat.messageAutoDeleteTime == seconds, { timerSheet = false; run(TdApi.SetChatMessageAutoDeleteTime(chatId, seconds)) })
                    Text(timerLabel(seconds))
                }
            }
        }
    }
    if (slowSheet && full != null) {
        ChoiceSheet(stringResource(R.string.admin_slow_mode), onDismiss = { slowSheet = false }) {
            listOf(0, 10, 30, 60, 300, 900, 3600).forEach { seconds ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(full.slowModeDelay == seconds, { slowSheet = false; run(TdApi.SetChatSlowModeDelay(chatId, seconds)) })
                    Text(timerLabel(seconds))
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(if (channel) R.string.admin_delete_channel else R.string.admin_delete_group)) },
            text = { Text(stringResource(R.string.admin_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        runCatching { graph.td.send(TdApi.DeleteChat(chatId)) }.onSuccess { navigator.back(); navigator.back() }.onFailure { error = it.message }
                    }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private val BasicReactions = listOf("👍", "👎", "❤", "🔥", "🥰", "👏", "😁", "🤔", "🤯", "😱", "🎉", "🤩", "🙏", "👌", "😢")

@Composable
private fun reactionsLabel(r: TdApi.ChatAvailableReactions?): String = when (r) {
    is TdApi.ChatAvailableReactionsSome -> if (r.reactions.isEmpty()) stringResource(R.string.admin_reactions_none) else stringResource(R.string.admin_reactions_count, r.reactions.size)
    else -> stringResource(R.string.admin_reactions_all)
}

@Composable
private fun timerLabel(seconds: Int): String = when {
    seconds <= 0 -> stringResource(R.string.admin_off)
    seconds < 60 -> stringResource(R.string.admin_seconds, seconds)
    seconds < 3600 -> stringResource(R.string.admin_minutes, seconds / 60)
    seconds < 86400 -> stringResource(R.string.admin_hours, seconds / 3600)
    else -> stringResource(R.string.admin_days, seconds / 86400)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
            content()
        }
    }
}

private fun senderId(sender: TdApi.MessageSender?): Long = when (sender) {
    is TdApi.MessageSenderUser -> sender.userId
    is TdApi.MessageSenderChat -> sender.chatId
    else -> 0L
}

@Composable
private fun PersonRow(graph: AppGraph, snapshot: StoreSnapshot, id: Long, subtitle: String?, index: Int, count: Int, onClick: (() -> Unit)?, trailing: @Composable () -> Unit = {}) {
    val user = snapshot.users[id]
    if (user == null && id > 0) androidx.compose.runtime.LaunchedEffect(id) { runCatching { graph.td.send(TdApi.GetUser(id)) } }
    val chat = if (user == null) graph.store.chat(id) else null
    val name = user?.name ?: chat?.title ?: id.toString()
    val photo = user?.photoFileId ?: chat?.photo?.small?.id ?: 0
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    SegmentedListItem(
        onClick = { onClick?.invoke() },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        leadingContent = { Avatar(name, id, user?.localPhoto ?: snapshot.files[photo], user?.minithumb, 44.dp, onNeedFile = { if (photo != 0) graph.store.requestFileAsync(photo) }) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        trailingContent = trailing,
    ) { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MembersPage(graph: AppGraph, chatId: Long, page: String, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    val state = rememberChatState(graph, chatId, 0)
    val group = state.group
    val snapshot by graph.store.snapshot.collectAsState()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<TdApi.ChatMember?>(null) }
    var menuFor by remember { mutableStateOf<Long?>(null) }
    val meId = snapshot.meId
    val owner = group?.status is TdApi.ChatMemberStatusCreator
    val myRights = (group?.status as? TdApi.ChatMemberStatusAdministrator)?.rights
    val canRestrict = owner || myRights?.canRestrictMembers == true
    val canPromote = owner || myRights?.canPromoteMembers == true
    val members by produceState<List<TdApi.ChatMember>?>(null, group?.id, page, reload, query) {
        val id = group?.id ?: return@produceState
        val filter: TdApi.SupergroupMembersFilter = when (page) {
            AdminPage.Admins -> TdApi.SupergroupMembersFilterAdministrators()
            AdminPage.Banned -> TdApi.SupergroupMembersFilterBanned(query)
            else -> if (query.isBlank()) TdApi.SupergroupMembersFilterRecent() else TdApi.SupergroupMembersFilterSearch(query)
        }
        value = runCatching { graph.td.send(TdApi.GetSupergroupMembers(id, filter, 0, 200)).members.toList() }.getOrDefault(emptyList())
    }
    val title = stringResource(
        when (page) {
            AdminPage.Admins -> R.string.admin_admins
            AdminPage.Banned -> R.string.admin_banned
            else -> if (group?.isChannel == true) R.string.admin_subscribers else R.string.admin_members
        },
    )
    val setStatus: (Long, TdApi.ChatMemberStatus) -> Unit = { id, status ->
        scope.launch {
            runCatching { graph.td.send(TdApi.SetChatMemberStatus(chatId, if (id > 0) TdApi.MessageSenderUser(id) else TdApi.MessageSenderChat(id), status)) }
            reload++
        }
    }
    AdminScaffold(title, navigator) { modifier ->
        val list = members
        Column(modifier) {
            if (page != AdminPage.Admins) OutlinedTextField(
                query,
                { query = it },
                placeholder = { Text(stringResource(R.string.privacy_search)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (list == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                return@Column
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                itemsIndexed(list, key = { _, m -> senderId(m.memberId) }) { index, member ->
                    val id = senderId(member.memberId)
                    val subtitle = when (val s = member.status) {
                        is TdApi.ChatMemberStatusCreator -> stringResource(R.string.profile_owner)
                        is TdApi.ChatMemberStatusAdministrator -> member.tag.ifBlank { stringResource(R.string.profile_admin) }
                        is TdApi.ChatMemberStatusRestricted -> stringResource(R.string.admin_restricted)
                        is TdApi.ChatMemberStatusBanned -> if (s.bannedUntilDate == 0) stringResource(R.string.admin_banned_forever) else DateFormat.getDateInstance().format(Date(s.bannedUntilDate * 1000L))
                        else -> snapshot.users[id]?.let { com.pixelgram.android.ui.components.statusLine(it) }
                    }
                    Box {
                        PersonRow(graph, snapshot, id, subtitle, index, list.size, onClick = { if (id != meId) menuFor = id }) {
                            if (page == AdminPage.Banned && canRestrict) IconButton(onClick = { setStatus(id, TdApi.ChatMemberStatusLeft()) }) { Icon(Icons.Rounded.Close, stringResource(R.string.admin_unban)) }
                        }
                        DropdownMenu(menuFor == id, { menuFor = null }) {
                            val status = member.status
                            if (canPromote && status !is TdApi.ChatMemberStatusCreator && id > 0) DropdownMenuItem(
                                text = { Text(stringResource(if (status is TdApi.ChatMemberStatusAdministrator) R.string.admin_edit_rights else R.string.admin_promote)) },
                                leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, null) },
                                onClick = { menuFor = null; editing = member },
                            )
                            if (canPromote && status is TdApi.ChatMemberStatusAdministrator && status.canBeEdited) DropdownMenuItem(
                                text = { Text(stringResource(R.string.admin_dismiss)) },
                                leadingIcon = { Icon(Icons.Rounded.Close, null) },
                                onClick = { menuFor = null; setStatus(id, TdApi.ChatMemberStatusMember(0)) },
                            )
                            if (canRestrict && status !is TdApi.ChatMemberStatusCreator && status !is TdApi.ChatMemberStatusAdministrator) {
                                if (group?.isChannel != true) DropdownMenuItem(
                                    text = { Text(stringResource(if (status is TdApi.ChatMemberStatusRestricted) R.string.admin_unrestrict else R.string.admin_mute)) },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                                    onClick = {
                                        menuFor = null
                                        if (status is TdApi.ChatMemberStatusRestricted) setStatus(id, TdApi.ChatMemberStatusMember(0))
                                        else setStatus(id, TdApi.ChatMemberStatusRestricted(true, 0, TdApi.ChatPermissions()))
                                    },
                                )
                                if (status !is TdApi.ChatMemberStatusBanned) DropdownMenuItem(
                                    text = { Text(stringResource(R.string.admin_remove)) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    onClick = {
                                        menuFor = null
                                        scope.launch {
                                            val sender: TdApi.MessageSender = if (id > 0) TdApi.MessageSenderUser(id) else TdApi.MessageSenderChat(id)
                                            runCatching { graph.td.send(TdApi.SetChatMemberStatus(chatId, sender, TdApi.ChatMemberStatusBanned(0))) }
                                            runCatching { graph.td.send(TdApi.SetChatMemberStatus(chatId, sender, TdApi.ChatMemberStatusLeft())) }
                                            reload++
                                        }
                                    },
                                )
                                if (status !is TdApi.ChatMemberStatusBanned) DropdownMenuItem(
                                    text = { Text(stringResource(R.string.admin_ban), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Block, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuFor = null; setStatus(id, TdApi.ChatMemberStatusBanned(0)) },
                                )
                            }
                            if (status is TdApi.ChatMemberStatusBanned && canRestrict) DropdownMenuItem(
                                text = { Text(stringResource(R.string.admin_unban)) },
                                leadingIcon = { Icon(Icons.Rounded.Check, null) },
                                onClick = { menuFor = null; setStatus(id, TdApi.ChatMemberStatusLeft()) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.admin_open_profile)) },
                                leadingIcon = { Icon(Icons.Rounded.Group, null) },
                                onClick = { menuFor = null; navigator.open(com.pixelgram.android.nav.ProfileKey(if (id > 0) id else 0L, if (id > 0) 0L else id)) },
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
    editing?.let { member ->
        RightsSheet(group?.isChannel == true, (member.status as? TdApi.ChatMemberStatusAdministrator)?.rights ?: myRights ?: TdApi.ChatAdministratorRights().apply { canChangeInfo = true; canDeleteMessages = true; canInviteUsers = true; canRestrictMembers = true; canPinMessages = true; canManageChat = true; canPostMessages = true; canEditMessages = true; canManageVideoChats = true }, owner, onDismiss = { editing = null }) { rights ->
            editing = null
            setStatus(senderId(member.memberId), TdApi.ChatMemberStatusAdministrator(true, rights))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RightsSheet(channel: Boolean, initial: TdApi.ChatAdministratorRights, owner: Boolean, onDismiss: () -> Unit, onSave: (TdApi.ChatAdministratorRights) -> Unit) {
    val values = remember {
        mutableStateListOf(
            initial.canChangeInfo, initial.canPostMessages, initial.canEditMessages, initial.canDeleteMessages, initial.canRestrictMembers,
            initial.canInviteUsers, initial.canPinMessages, initial.canManageTopics, initial.canManageVideoChats, initial.canPostStories,
            initial.canEditStories, initial.canDeleteStories, initial.canPromoteMembers, initial.isAnonymous,
        )
    }
    val labels = listOf(
        R.string.right_change_info, R.string.right_post, R.string.right_edit, R.string.right_delete, R.string.right_ban,
        R.string.right_invite, R.string.right_pin, R.string.right_topics, R.string.right_calls, R.string.right_post_stories,
        R.string.right_edit_stories, R.string.right_delete_stories, R.string.right_promote, R.string.right_anonymous,
    )
    val visible = labels.indices.filter { i ->
        when (i) {
            1, 2 -> channel
            4, 6, 7, 13 -> !channel
            12 -> owner
            else -> true
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Text(stringResource(R.string.admin_rights), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            SettingsGroup {
                visible.forEachIndexed { pos, i ->
                    SettingsItem(stringResource(labels[i]), segmentPosition(pos, visible.size), onClick = { values[i] = !values[i] }) {
                        PixelSwitch(values[i], { values[i] = it })
                    }
                }
            }
            Button(onClick = {
                onSave(
                    TdApi.ChatAdministratorRights().apply {
                        canManageChat = true
                        canChangeInfo = values[0]
                        canPostMessages = values[1]
                        canEditMessages = values[2]
                        canDeleteMessages = values[3]
                        canRestrictMembers = values[4]
                        canInviteUsers = values[5]
                        canPinMessages = values[6]
                        canManageTopics = values[7]
                        canManageVideoChats = values[8]
                        canPostStories = values[9]
                        canEditStories = values[10]
                        canDeleteStories = values[11]
                        canPromoteMembers = values[12]
                        isAnonymous = values[13]
                        canManageTags = initial.canManageTags
                        canManageDirectMessages = initial.canManageDirectMessages
                        canSendWelcomeMessages = initial.canSendWelcomeMessages
                    },
                )
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp)) { Text(stringResource(R.string.action_save)) }
        }
    }
}

@Composable
private fun PermissionsPage(graph: AppGraph, chatId: Long, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val chat = graph.store.chat(chatId)
    val initial = chat?.permissions ?: TdApi.ChatPermissions()
    val values = remember {
        mutableStateListOf(
            initial.canSendBasicMessages, initial.canSendPhotos, initial.canSendVideos, initial.canSendAudios, initial.canSendDocuments,
            initial.canSendVoiceNotes, initial.canSendVideoNotes, initial.canSendOtherMessages, initial.canSendPolls, initial.canAddLinkPreviews,
            initial.canReactToMessages, initial.canInviteUsers, initial.canPinMessages, initial.canChangeInfo, initial.canCreateTopics, initial.canEditTag,
        )
    }
    val labels = listOf(
        R.string.perm_messages, R.string.perm_photos, R.string.perm_videos, R.string.perm_music, R.string.perm_files,
        R.string.perm_voice, R.string.perm_rounds, R.string.perm_stickers, R.string.perm_polls, R.string.perm_links,
        R.string.perm_reactions, R.string.perm_invite, R.string.perm_pin, R.string.perm_info, R.string.perm_topics, R.string.perm_tags,
    )
    var error by remember { mutableStateOf<String?>(null) }
    AdminScaffold(stringResource(R.string.admin_permissions), navigator, fab = {
        FloatingActionButton(onClick = {
            scope.launch {
                runCatching {
                    graph.td.send(
                        TdApi.SetChatPermissions(
                            chatId,
                            TdApi.ChatPermissions(values[0], values[3], values[4], values[1], values[2], values[6], values[5], values[8], values[7], values[9], values[10], values[15], values[13], values[11], values[12], values[14]),
                        ),
                    )
                }.onSuccess { navigator.back() }.onFailure { error = it.message }
            }
        }) { Icon(Icons.Rounded.Check, stringResource(R.string.action_save)) }
    }) { modifier ->
        Column(modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.admin_permissions_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 32.dp)) }
            SettingsGroup {
                labels.forEachIndexed { i, label ->
                    SettingsItem(stringResource(label), segmentPosition(i, labels.size), onClick = { values[i] = !values[i] }) {
                        PixelSwitch(values[i], { values[i] = it })
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinksPage(graph: AppGraph, chatId: Long, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snapshot by graph.store.snapshot.collectAsState()
    var reload by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    val links by produceState<List<TdApi.ChatInviteLink>?>(null, reload) {
        value = runCatching { graph.td.send(TdApi.GetChatInviteLinks(chatId, snapshot.meId, false, 0, "", 100)).inviteLinks.toList() }.getOrDefault(emptyList())
    }
    val copied = stringResource(R.string.profile_copied)
    AdminScaffold(stringResource(R.string.admin_invite_links), navigator, fab = {
        FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, stringResource(R.string.admin_new_link)) }
    }) { modifier ->
        val list = links
        if (list == null) {
            Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@AdminScaffold
        }
        LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            itemsIndexed(list, key = { _, l -> l.inviteLink }) { index, link ->
                var menu by remember { mutableStateOf(false) }
                Box {
                    SegmentedListItem(
                        onClick = { menu = true },
                        shapes = ListItemDefaults.segmentedShapes(index, list.size),
                        colors = com.pixelgram.android.ui.components.segmentedColors(),
                        leadingContent = { Icon(Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.primary) },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(context.resources.getQuantityString(R.plurals.admin_joined, link.memberCount, link.memberCount))
                                    if (link.pendingJoinRequestCount > 0) append(" · ").append(context.resources.getQuantityString(R.plurals.admin_pending, link.pendingJoinRequestCount, link.pendingJoinRequestCount))
                                    if (link.isPrimary) append(" · ").append(context.getString(R.string.admin_primary))
                                },
                            )
                        },
                    ) { Text(link.name.ifBlank { link.inviteLink.removePrefix("https://") }, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.admin_copy)) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = {
                            menu = false
                            context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText(null, link.inviteLink))
                            android.widget.Toast.makeText(context, copied, android.widget.Toast.LENGTH_SHORT).show()
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.admin_revoke), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = {
                            menu = false
                            scope.launch {
                                runCatching { graph.td.send(TdApi.RevokeChatInviteLink(chatId, link.inviteLink)) }
                                reload++
                            }
                        })
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
    if (creating) {
        var name by remember { mutableStateOf("") }
        var request by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.admin_new_link)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.take(32) }, label = { Text(stringResource(R.string.admin_link_name)) }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.admin_link_request), Modifier.weight(1f))
                        PixelSwitch(request, { request = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    creating = false
                    scope.launch {
                        runCatching { graph.td.send(TdApi.CreateChatInviteLink(chatId, name.trim(), 0, 0, request)) }
                        reload++
                    }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RequestsPage(graph: AppGraph, chatId: Long, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val snapshot by graph.store.snapshot.collectAsState()
    var reload by remember { mutableIntStateOf(0) }
    val requests by produceState<List<TdApi.ChatJoinRequest>?>(null, reload) {
        value = runCatching { graph.td.send(TdApi.GetChatJoinRequests(chatId, "", "", null, 100)).requests.toList() }.getOrDefault(emptyList())
    }
    val process: (Long?, Boolean) -> Unit = { userId, approve ->
        scope.launch {
            runCatching { graph.td.send(if (userId == null) TdApi.ProcessChatJoinRequests(chatId, "", approve) else TdApi.ProcessChatJoinRequest(chatId, userId, approve)) }
            reload++
        }
    }
    AdminScaffold(stringResource(R.string.admin_requests), navigator) { modifier ->
        val list = requests
        if (list == null) {
            Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@AdminScaffold
        }
        LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            if (list.size > 1) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Button(onClick = { process(null, true) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.admin_approve_all)) }
                    TextButton(onClick = { process(null, false) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.admin_decline_all)) }
                }
            }
            itemsIndexed(list, key = { _, r -> r.userId }) { index, request ->
                PersonRow(graph, snapshot, request.userId, request.bio.ifBlank { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(request.date * 1000L)) }, index, list.size, null) {
                    Row {
                        IconButton(onClick = { process(request.userId, false) }) { Icon(Icons.Rounded.Close, stringResource(R.string.admin_decline)) }
                        IconButton(onClick = { process(request.userId, true) }) { Icon(Icons.Rounded.Check, stringResource(R.string.admin_approve), tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            if (list.isEmpty()) item {
                Text(stringResource(R.string.admin_no_requests), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogPage(graph: AppGraph, chatId: Long, navigator: Navigator) {
    val snapshot by graph.store.snapshot.collectAsState()
    val events by produceState<List<TdApi.ChatEvent>?>(null) {
        value = runCatching { graph.td.send(TdApi.GetChatEventLog(chatId, "", 0, 100, null, LongArray(0))).events.toList() }.getOrDefault(emptyList())
    }
    AdminScaffold(stringResource(R.string.admin_log), navigator) { modifier ->
        val list = events
        if (list == null) {
            Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@AdminScaffold
        }
        LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            itemsIndexed(list, key = { _, e -> e.id }) { index, event ->
                val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.date * 1000L))
                PersonRow(graph, snapshot, senderId(event.memberId), "${eventText(event.action)} · $time", index, list.size, null)
            }
            if (list.isEmpty()) item {
                Text(stringResource(R.string.admin_log_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(32.dp))
            }
        }
    }
}

@Composable
private fun eventText(action: TdApi.ChatEventAction): String = when (action) {
    is TdApi.ChatEventMessageEdited -> stringResource(R.string.log_edited)
    is TdApi.ChatEventMessageDeleted -> stringResource(R.string.log_deleted)
    is TdApi.ChatEventMessagePinned -> stringResource(R.string.log_pinned)
    is TdApi.ChatEventMessageUnpinned -> stringResource(R.string.log_unpinned)
    is TdApi.ChatEventMemberJoined, is TdApi.ChatEventMemberJoinedByInviteLink, is TdApi.ChatEventMemberJoinedByRequest -> stringResource(R.string.log_joined)
    is TdApi.ChatEventMemberLeft -> stringResource(R.string.log_left)
    is TdApi.ChatEventMemberInvited -> stringResource(R.string.log_invited)
    is TdApi.ChatEventMemberPromoted -> stringResource(R.string.log_promoted)
    is TdApi.ChatEventMemberRestricted -> stringResource(R.string.log_restricted)
    is TdApi.ChatEventTitleChanged -> stringResource(R.string.log_title, action.newTitle)
    is TdApi.ChatEventDescriptionChanged -> stringResource(R.string.log_description)
    is TdApi.ChatEventPhotoChanged -> stringResource(R.string.log_photo)
    is TdApi.ChatEventUsernameChanged -> stringResource(R.string.log_username, action.newUsername)
    is TdApi.ChatEventPermissionsChanged -> stringResource(R.string.log_permissions)
    is TdApi.ChatEventInviteLinkRevoked, is TdApi.ChatEventInviteLinkDeleted, is TdApi.ChatEventInviteLinkEdited -> stringResource(R.string.log_links)
    else -> action.javaClass.simpleName.removePrefix("ChatEvent").replace(Regex("([a-z])([A-Z])"), "$1 $2")
}
