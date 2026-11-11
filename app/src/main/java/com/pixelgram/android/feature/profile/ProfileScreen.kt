package com.pixelgram.android.feature.profile

import androidx.compose.ui.graphics.toArgb
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.ExpressiveQrCode
import com.pixelgram.android.ui.components.HiddenPhone
import com.pixelgram.android.ui.components.SegmentPosition
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.util.Phones
import com.pixelgram.android.util.formatPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.produceState
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsActive
import com.pixelgram.android.nav.ProfilePhotosKey
import com.pixelgram.android.nav.SettingsSectionKey

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(graph: AppGraph, userId: Long, chatId: Long, navigator: Navigator, onOpenDrawer: (() -> Unit)? = null, onOpenSettings: (() -> Unit)? = null) {
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val user = snapshot.users[userId]
    val chat = remember(chatId, snapshot) { if (chatId != 0L) graph.store.chat(chatId) else null }
    var full by remember { mutableStateOf<TdApi.UserFullInfo?>(null) }
    var showQr by remember { mutableStateOf(false) }
    var collectible by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(userId) {
        if (userId != 0L) full = runCatching { graph.td.send(TdApi.GetUserFullInfo(userId)) }.getOrNull()
    }
    val bio = full?.bio?.text.orEmpty()
    val self = userId != 0L && userId == snapshot.meId
    val pickAvatar = rememberAvatarPicker(graph)
    val title = user?.name ?: chat?.title.orEmpty()
    val username = user?.username
    val group = (chat?.type as? TdApi.ChatTypeSupergroup)?.takeIf { userId == 0L }
    val supergroup by produceState<TdApi.Supergroup?>(null, group?.supergroupId) {
        value = group?.let { runCatching { graph.td.send(TdApi.GetSupergroup(it.supergroupId)) }.getOrNull() }
    }
    val groupFull by produceState<TdApi.SupergroupFullInfo?>(null, group?.supergroupId) {
        value = group?.let { runCatching { graph.td.send(TdApi.GetSupergroupFullInfo(it.supergroupId)) }.getOrNull() }
    }
    val channelMembers = groupFull?.memberCount ?: supergroup?.memberCount ?: 0
    val channelSubtitle = if (group != null && channelMembers > 0) {
        androidx.compose.ui.res.pluralStringResource(if (group.isChannel) R.plurals.chat_subscribers else R.plurals.chat_members, channelMembers, channelMembers)
    } else {
        null
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            val status = user?.let { com.pixelgram.android.ui.components.statusLine(it) } ?: channelSubtitle
            ProfileHeader(
                graph = graph,
                userId = if (userId != 0L) userId else chatId,
                title = title,
                subtitle = status,
                photoFileId = user?.bigPhotoFileId ?: chat?.photo?.big?.id ?: 0,
                minithumb = user?.minithumb ?: chat?.photo?.minithumbnail?.data,
                verified = user?.verified == true || supergroup?.verificationStatus?.isVerified == true,
                showCamera = false,
                onAvatarClick = { if (userId != 0L && user?.deleted != true) navigator.open(ProfilePhotosKey(userId)) },
                icon = if (user?.deleted == true) com.pixelgram.android.ui.icons.GhostIcon else null,
                localPhoto = user?.localPhoto,
                topStart = {
                    if (onOpenDrawer != null) IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) }
                    else IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                topEnd = {
                    if (!self && userId != 0L && user?.deleted != true) {
                        Box {
                            var more by remember { mutableStateOf(false) }
                            IconButton(onClick = { more = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.profile_more)) }
                            ProfileMoreMenu(graph, navigator, userId, username, full, more, onDismiss = { more = false }, onQr = { showQr = true })
                        }
                    }
                },
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (self) {
                    ProfileAction(Icons.Rounded.AddAPhoto, stringResource(R.string.profile_set_photo), pickAvatar)
                    ProfileAction(Icons.Rounded.Edit, stringResource(R.string.profile_edit)) { navigator.open(SettingsSectionKey("Account")) }
                    if (onOpenSettings != null) ProfileAction(Icons.Rounded.Settings, stringResource(R.string.profile_settings), onOpenSettings)
                } else if (userId != 0L) {
                    ProfileAction(Icons.AutoMirrored.Rounded.Chat, stringResource(R.string.profile_message)) {
                        scope.launch {
                            val created = runCatching { graph.td.send(TdApi.CreatePrivateChat(userId, false)) }.getOrNull()
                            created?.let { navigator.open(ChatKey(it.id)) }
                        }
                    }
                    val context = LocalContext.current
                    val soon = stringResource(R.string.profile_calls_soon)
                    ProfileAction(Icons.Rounded.Call, stringResource(R.string.profile_call)) { android.widget.Toast.makeText(context, soon, android.widget.Toast.LENGTH_SHORT).show() }
                    ProfileAction(Icons.Rounded.Videocam, stringResource(R.string.profile_video)) { android.widget.Toast.makeText(context, soon, android.widget.Toast.LENGTH_SHORT).show() }
                    val privateChat = graph.store.chat(userId)
                    val muted = (privateChat?.notificationSettings?.muteFor ?: 0) > 0
                    ProfileAction(if (muted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, stringResource(if (muted) R.string.profile_unmute else R.string.profile_mute)) {
                        scope.launch {
                            runCatching {
                                graph.td.send(TdApi.SetChatNotificationSettings(userId, TdApi.ChatNotificationSettings().apply {
                                    useDefaultMuteFor = false
                                    muteFor = if (muted) 0 else Int.MAX_VALUE
                                    useDefaultSound = true
                                    useDefaultShowPreview = true
                                    useDefaultMuteStories = true
                                    useDefaultShowStoryPoster = true
                                    useDefaultDisablePinnedMessageNotifications = true
                                    useDefaultDisableMentionNotifications = true
                                }))
                            }
                        }
                    }
                }
            }
            if (group != null && chat != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileAction(Icons.AutoMirrored.Rounded.Chat, stringResource(R.string.profile_message)) { navigator.open(ChatKey(chatId)) }
                    val muted = chat.notificationSettings.muteFor > 0
                    ProfileAction(if (muted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, stringResource(if (muted) R.string.profile_unmute else R.string.profile_mute)) {
                        scope.launch {
                            runCatching {
                                graph.td.send(TdApi.SetChatNotificationSettings(chatId, TdApi.ChatNotificationSettings().apply {
                                    useDefaultMuteFor = false
                                    muteFor = if (muted) 0 else Int.MAX_VALUE
                                    useDefaultSound = true
                                    useDefaultShowPreview = true
                                    useDefaultMuteStories = true
                                    useDefaultShowStoryPoster = true
                                    useDefaultDisablePinnedMessageNotifications = true
                                    useDefaultDisableMentionNotifications = true
                                }))
                            }
                        }
                    }
                    val member = supergroup?.status?.let { it !is TdApi.ChatMemberStatusLeft && it !is TdApi.ChatMemberStatusBanned } == true
                    ProfileAction(if (member) Icons.AutoMirrored.Rounded.Logout else Icons.Rounded.PersonAdd, stringResource(if (member) R.string.chat_leave else R.string.profile_join)) {
                        scope.launch {
                            runCatching { graph.td.send(if (member) TdApi.LeaveChat(chatId) else TdApi.JoinChat(chatId)) }
                            navigator.back()
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val context = LocalContext.current
                val copiedText = stringResource(R.string.profile_copied)
                val copy: (String) -> Unit = { value ->
                    context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText(null, value))
                    android.widget.Toast.makeText(context, copiedText, android.widget.Toast.LENGTH_SHORT).show()
                }
                val about = groupFull?.description.orEmpty()
                val names = supergroup?.usernames?.activeUsernames?.toList().orEmpty()
                val rows = buildList {
                    if (about.isNotBlank()) add("about")
                    names.forEach { add("u:$it") }
                    groupFull?.inviteLink?.inviteLink?.takeIf { names.isEmpty() }?.let { add("link:$it") }
                    add("id")
                }
                SettingsGroup {
                    rows.forEachIndexed { index, row ->
                        val position = com.pixelgram.android.ui.components.segmentPosition(index, rows.size)
                        when {
                            row == "about" -> SettingsItem(about, position, stringResource(R.string.profile_description), Icons.Rounded.Info, pill = true, onLongClick = { copy(about) })
                            row.startsWith("u:") -> {
                                val name = row.removePrefix("u:")
                                SettingsItem("t.me/$name", position, stringResource(R.string.profile_link), Icons.Rounded.Link, pill = true, onClick = { collectible = name }, onLongClick = { copy("https://t.me/$name") })
                            }
                            row.startsWith("link:") -> {
                                val link = row.removePrefix("link:")
                                SettingsItem(link, position, stringResource(R.string.profile_link), Icons.Rounded.Link, pill = true, onLongClick = { copy(link) })
                            }
                            row == "id" -> SettingsItem(chatId.toString(), position, "ID", Icons.Rounded.Tag, pill = true, onLongClick = { copy(chatId.toString()) })
                        }
                    }
                }
            }
            full?.rating?.takeIf { !self || it.level > 0 }?.let { rating ->
                RatingChip(rating, user?.firstName.orEmpty())
            }
            full?.firstProfileAudio?.let { audio ->
                Spacer(Modifier.height(16.dp))
                val playlist = stringResource(R.string.player_playlist_of, playlistName(user))
                ProfileMusicCard(audio) {
                    com.pixelgram.android.feature.player.FullPlayer.view = com.pixelgram.android.feature.player.PlayerView.Queue
                    val current = graph.music.state.value
                    if (current.source == playlist && current.track != null) {
                        com.pixelgram.android.feature.player.FullPlayer.open = true
                    } else {
                        scope.launch {
                            val all = runCatching { graph.td.send(TdApi.GetUserProfileAudios(userId, 0, 100)).audios.toList() }.getOrNull()
                            graph.music.play(all?.takeIf { it.isNotEmpty() } ?: listOf(audio), 0, playlist)
                            com.pixelgram.android.feature.player.FullPlayer.open = true
                        }
                    }
                }
            }
            val phone = user?.phone.orEmpty()
            val showDetails = appearance.phoneDetails && !self && phone.isNotBlank()
            val phoneDetails by produceState<String?>(null, phone, showDetails) {
                value = if (!showDetails) null else withContext(Dispatchers.Default) {
                    runCatching { Phones.details(phone) }.getOrNull()?.let { d ->
                        listOfNotNull("${d.flag} ${d.country}", d.carrier).joinToString(" · ")
                    }
                }
            }
            val birthday = full?.birthdate
            Spacer(Modifier.height(8.dp))
            SettingsGroup {
                val context = LocalContext.current
                val copiedText = stringResource(R.string.profile_copied)
                val copy: (String) -> Unit = { value ->
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(null, value))
                    android.widget.Toast.makeText(context, copiedText, android.widget.Toast.LENGTH_SHORT).show()
                }
                val names = user?.usernames?.takeIf { it.isNotEmpty() } ?: listOfNotNull(username)
                val rows = buildList {
                    if (bio.isNotBlank()) add("bio")
                    names.forEach { add("u:$it") }
                    if (phone.isNotBlank()) add("phone")
                    if (birthday != null) add("birthday")
                    if (userId != 0L) add("id")
                }
                rows.forEachIndexed { index, row ->
                    val position = com.pixelgram.android.ui.components.segmentPosition(index, rows.size)
                    when {
                        row == "phone" -> SettingsItem(
                            "+$phone",
                            position,
                            subtitle = listOfNotNull(stringResource(R.string.profile_phone), phoneDetails?.takeIf { it.isNotBlank() }).joinToString(" · "),
                            icon = Icons.Rounded.Phone,
                            pill = true,
                            headlineContent = { HiddenPhone("+" + formatPhone(phone), hidden = appearance.hidePhone && self) },
                            onLongClick = { copy("+$phone") },
                        )
                        row.startsWith("u:") -> {
                            val name = row.removePrefix("u:")
                            SettingsItem(
                                "@$name",
                                position,
                                stringResource(if (name == names.first()) R.string.profile_username else R.string.profile_extra_username),
                                Icons.Rounded.AlternateEmail,
                                pill = true,
                                onClick = { collectible = name },
                                onLongClick = { copy("@$name") },
                                trailing = if (name == names.first()) {
                                    { androidx.compose.material3.IconButton(onClick = { showQr = true }) { Icon(Icons.Rounded.QrCode2, stringResource(R.string.profile_share)) } }
                                } else {
                                    null
                                },
                            )
                        }
                        row == "bio" -> SettingsItem(bio, position, stringResource(R.string.profile_bio), Icons.Rounded.Info, pill = true, onLongClick = { copy(bio) })
                        row == "id" -> SettingsItem(userId.toString(), position, "ID", Icons.Rounded.Tag, pill = true, onLongClick = { copy(userId.toString()) })
                        row == "birthday" -> SettingsItem(
                            listOfNotNull(birthday!!.day.toString(), java.text.DateFormatSymbols.getInstance().months[birthday.month - 1], birthday.year.takeIf { it > 0 }?.toString()).joinToString(" "),
                            position,
                            stringResource(R.string.profile_birthday),
                            Icons.Rounded.Cake,
                            pill = true,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp + com.pixelgram.android.ui.components.LocalBottomBarPadding.current))
        }
    }
    collectible?.let { name -> UsernameSheet(graph, name) { collectible = null } }
    if (showQr && username != null) {
        val context = LocalContext.current
        val link = "https://t.me/$username"
        val qrSheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val qrScope = rememberCoroutineScope()
        ModalBottomSheet(onDismissRequest = { showQr = false }, sheetState = qrSheet) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExpressiveQrCode(link, Modifier.fillMaxWidth(0.85f).aspectRatio(1f))
                Text("@$username", style = MaterialTheme.typography.titleLargeEmphasized)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { qrScope.launch { qrSheet.hide() }.invokeOnCompletion { showQr = false } }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_close)) }
                    Button(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link)
                            context.startActivity(Intent.createChooser(send, null))
                        },
                        shapes = ButtonDefaults.shapes(),
                    ) { Text(stringResource(R.string.action_share)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.ProfileAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.weight(1f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null)
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileMusicCard(audio: TdApi.Audio, onClick: () -> Unit) {
    val graph = com.pixelgram.android.LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val covers by graph.covers.found.collectAsState()
    val coverId = audio.albumCoverThumbnail?.file?.id ?: audio.externalAlbumCovers?.maxByOrNull { it.width }?.file?.id ?: 0
    LaunchedEffect(coverId) { if (coverId != 0) graph.store.requestFileAsync(coverId) }
    val path = snapshot.files[coverId] ?: graph.covers.key(audio)?.let { covers[it] }
    val cover: Any? = path?.let { java.io.File(it) } ?: audio.albumCoverMinithumbnail?.data
    val dark = com.pixelgram.android.ui.theme.LocalDarkTheme.current
    val scheme by androidx.compose.runtime.produceState<androidx.compose.material3.ColorScheme?>(null, path, dark) {
        value = path?.let {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { com.pixelgram.android.feature.player.coverPalette(it) }.getOrNull()
                    ?.let { palette -> (palette.getOrNull(3) ?: palette.first()).let { seed -> com.pixelgram.android.feature.player.schemeFromSeed(seed.toArgb(), dark) } }
            }
        }
    }
    val container by androidx.compose.animation.animateColorAsState(scheme?.primaryContainer ?: MaterialTheme.colorScheme.primaryContainer, label = "card")
    val content by androidx.compose.animation.animateColorAsState(scheme?.onPrimaryContainer ?: MaterialTheme.colorScheme.onPrimaryContainer, label = "card-content")
    androidx.compose.material3.Card(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        androidx.compose.material3.ListItem(
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                headlineColor = content,
                supportingColor = content.copy(alpha = 0.8f),
                overlineColor = content.copy(alpha = 0.8f),
            ),
            leadingContent = {
                Box(
                    Modifier.size(64.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(content.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = content)
                    if (cover != null) coil.compose.AsyncImage(model = cover, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(64.dp))
                }
            },
            overlineContent = { Text(stringResource(R.string.profile_music)) },
            headlineContent = { Text(audio.title.ifBlank { audio.fileName }, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1) },
            supportingContent = if (audio.performer.isNotBlank()) {
                { Text(audio.performer, maxLines = 1) }
            } else {
                null
            },
        )
    }
}

internal fun playlistName(user: com.pixelgram.android.td.UserInfo?): String {
    if (user == null) return ""
    val words = (user.firstName.split(' ') + user.lastName.split(' ')).map { it.trim() }.filter { word -> word.any { it.isLetterOrDigit() } }
    return words.firstOrNull() ?: user.name
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileMoreMenu(graph: AppGraph, navigator: Navigator, userId: Long, username: String?, full: TdApi.UserFullInfo?, expanded: Boolean, onDismiss: () -> Unit, onQr: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val photo = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val target = java.io.File(java.io.File(context.filesDir, "local-photos").apply { mkdirs() }, "$userId.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                    target.absolutePath
                }.getOrNull()
            }
            graph.store.setLocalPhoto(userId, path)
        }
    }
    val blocked = full?.blockList != null
    val user = graph.store.snapshot.collectAsState().value.users[userId]
    var autoDelete by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf(false) }
    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_auto_delete)) },
            leadingIcon = { Icon(Icons.Rounded.Timer, null) },
            trailingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
            onClick = { onDismiss(); autoDelete = true },
        )
        androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.profile_edit)) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { onDismiss(); editing = true })
        if (user?.contact == true) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_delete_contact)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = {
                    onDismiss()
                    scope.launch { runCatching { graph.td.send(TdApi.RemoveContacts(longArrayOf(userId))) } }
                },
            )
        } else if (user != null && !user.bot) {
            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.profile_add_contact)) }, leadingIcon = { Icon(Icons.Rounded.PersonAdd, null) }, onClick = { onDismiss(); adding = true })
        }
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_secret_chat)) },
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            onClick = {
                onDismiss()
                scope.launch {
                    runCatching { graph.td.send(TdApi.CreateNewSecretChat(userId)) }.getOrNull()?.let { navigator.open(ChatKey(it.id)) }
                }
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_add_folder)) },
            leadingIcon = { Icon(Icons.Rounded.Folder, null) },
            trailingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
            onClick = { onDismiss(); folders = true },
        )
        if (username != null) androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.profile_share)) }, leadingIcon = { Icon(Icons.Rounded.QrCode2, null) }, onClick = { onDismiss(); onQr() })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(if (blocked) R.string.profile_unblock else R.string.profile_block), color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Rounded.Block, null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                scope.launch { runCatching { graph.td.send(TdApi.SetMessageSenderBlockList(TdApi.MessageSenderUser(userId), if (blocked) null else TdApi.BlockListMain())) } }
            },
        )
    }
    if (autoDelete) {
        val options = listOf(0 to R.string.auto_delete_off, 86_400 to R.string.auto_delete_day, 604_800 to R.string.auto_delete_week, 2_592_000 to R.string.auto_delete_month)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { autoDelete = false },
            title = { Text(stringResource(R.string.profile_auto_delete)) },
            text = {
                Column {
                    options.forEach { (seconds, label) ->
                        androidx.compose.material3.ListItem(
                            modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable {
                                autoDelete = false
                                scope.launch { runCatching { graph.td.send(TdApi.SetChatMessageAutoDeleteTime(userId, seconds)) } }
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            headlineContent = { Text(stringResource(label)) },
                        )
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { autoDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if ((editing || adding) && user != null) {
        val contactEdit = adding || user.contact
        val localName = graph.store.localName(userId)
        var first by remember { mutableStateOf(if (contactEdit) user.firstName else localName ?: user.firstName) }
        var last by remember { mutableStateOf(if (contactEdit) user.lastName else if (localName != null) "" else user.lastName) }
        val close = { editing = false; adding = false }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(if (adding) R.string.profile_add_contact else R.string.profile_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!adding) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            com.pixelgram.android.ui.components.Avatar(user.name, userId, user.localPhoto ?: graph.store.snapshot.value.files[user.photoFileId], user.minithumb, 56.dp)
                            Column {
                                androidx.compose.material3.TextButton(onClick = {
                                    photo.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) { Text(stringResource(R.string.profile_set_photo_for)) }
                                if (user.localPhoto != null) {
                                    androidx.compose.material3.TextButton(onClick = { graph.store.setLocalPhoto(userId, null) }) { Text(stringResource(R.string.profile_remove_photo), color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                    androidx.compose.material3.OutlinedTextField(first, { first = it.take(64) }, label = { Text(stringResource(R.string.contact_first_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    androidx.compose.material3.OutlinedTextField(last, { last = it.take(64) }, label = { Text(stringResource(R.string.contact_last_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(enabled = first.isNotBlank(), onClick = {
                    close()
                    if (contactEdit) {
                        scope.launch {
                            runCatching {
                                graph.td.send(TdApi.AddContact().apply {
                                    this.userId = userId
                                    contact = TdApi.ImportedContact().apply {
                                        phoneNumber = user.phone
                                        firstName = first.trim()
                                        lastName = last.trim()
                                    }
                                    sharePhoneNumber = false
                                })
                            }
                        }
                    } else {
                        val full = listOf(first.trim(), last.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                        graph.store.setLocalName(userId, full.takeIf { it != listOf(user.firstName, user.lastName).filter { p -> p.isNotBlank() }.joinToString(" ") })
                    }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = close) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (folders) {
        val list = graph.store.snapshot.collectAsState().value.folders
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { folders = false },
            title = { Text(stringResource(R.string.profile_add_folder)) },
            text = {
                Column {
                    list.forEach { folder ->
                        androidx.compose.material3.ListItem(
                            modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable {
                                folders = false
                                scope.launch {
                                    runCatching {
                                        val current = graph.td.send(TdApi.GetChatFolder(folder.id))
                                        if (userId !in current.includedChatIds) {
                                            current.includedChatIds = current.includedChatIds + userId
                                            current.excludedChatIds = current.excludedChatIds.filter { it != userId }.toLongArray()
                                            graph.td.send(TdApi.EditChatFolder(folder.id, current))
                                        }
                                    }
                                }
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            leadingContent = { Icon(Icons.Rounded.Folder, null) },
                            headlineContent = { Text(folder.title) },
                        )
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { folders = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RatingChip(rating: TdApi.UserRating, name: String) {
    var open by remember { mutableStateOf(false) }
    androidx.compose.material3.AssistChip(
        onClick = { open = true },
        label = { Text(stringResource(R.string.rating_level, rating.level)) },
        leadingIcon = { Icon(Icons.Rounded.Shield, null, Modifier.size(18.dp)) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.padding(top = 8.dp),
    )
    if (open) {
        val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetScope = rememberCoroutineScope()
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheet) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val span = (rating.nextLevelRating - rating.currentLevelRating).coerceAtLeast(1)
                val progress = if (rating.isMaximumLevelReached) 1f else ((rating.rating - rating.currentLevelRating).toFloat() / span).coerceIn(0f, 1f)
                Text("${rating.rating} / ${if (rating.isMaximumLevelReached) rating.rating else rating.nextLevelRating}", style = MaterialTheme.typography.titleLarge)
                androidx.compose.material3.LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.rating_level, rating.level), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    if (!rating.isMaximumLevelReached) Text(stringResource(R.string.rating_level, rating.level + 1), style = MaterialTheme.typography.labelLarge)
                }
                Text(stringResource(R.string.rating_title), style = MaterialTheme.typography.headlineSmallEmphasized)
                Text(stringResource(R.string.rating_text, name), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { sheetScope.launch { sheet.hide() }.invokeOnCompletion { open = false } }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_understood)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UsernameSheet(graph: AppGraph, name: String, onDismiss: () -> Unit) {
    val info by produceState<TdApi.CollectibleItemInfo?>(null, name) {
        value = runCatching { graph.td.send(TdApi.GetCollectibleItemInfo(TdApi.CollectibleItemTypeUsername(name))) }.getOrNull()
    }
    val state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.AlternateEmail, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text("@$name", style = MaterialTheme.typography.headlineSmallEmphasized)
            val item = info
            if (item != null) {
                val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG).format(java.util.Date(item.purchaseDate * 1000L))
                Text(stringResource(R.string.username_bought, date), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(
                    "${formatAmount(item.cryptocurrencyAmount, 9)} ${item.cryptocurrency} · ${formatAmount(item.amount, 2)} ${item.currency}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(item.url))) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.username_fragment))
                }
            }
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "https://t.me/$name")
                    context.startActivity(Intent.createChooser(send, null))
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_share)) }
            androidx.compose.material3.TextButton(onClick = close) { Text(stringResource(R.string.action_close)) }
        }
    }
}

private fun formatAmount(value: Long, decimals: Int): String {
    val scaled = java.math.BigDecimal.valueOf(value).movePointLeft(decimals).stripTrailingZeros()
    return java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits = decimals }.format(scaled)
}
