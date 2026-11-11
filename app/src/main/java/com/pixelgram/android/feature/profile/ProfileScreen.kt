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
    val scope = rememberCoroutineScope()
    LaunchedEffect(userId) {
        if (userId != 0L) full = runCatching { graph.td.send(TdApi.GetUserFullInfo(userId)) }.getOrNull()
    }
    val bio = full?.bio?.text.orEmpty()
    val self = userId != 0L && userId == snapshot.meId
    val pickAvatar = rememberAvatarPicker(graph)
    val title = user?.name ?: chat?.title.orEmpty()
    val username = user?.username
    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            val status = user?.let { com.pixelgram.android.ui.components.statusLine(it) }
            ProfileHeader(
                graph = graph,
                userId = if (userId != 0L) userId else chatId,
                title = title,
                subtitle = status,
                photoFileId = user?.bigPhotoFileId ?: chat?.photo?.big?.id ?: 0,
                minithumb = user?.minithumb ?: chat?.photo?.minithumbnail?.data,
                verified = user?.verified == true,
                showCamera = false,
                onAvatarClick = { if (userId != 0L && user?.deleted != true) navigator.open(ProfilePhotosKey(userId)) },
                icon = if (user?.deleted == true) com.pixelgram.android.ui.icons.GhostIcon else null,
                localPhoto = user?.localPhoto,
                topStart = {
                    if (onOpenDrawer != null) IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) }
                    else IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                topEnd = {},
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
                    Box(Modifier.weight(1f)) {
                        var more by remember { mutableStateOf(false) }
                        Row { ProfileAction(Icons.Rounded.MoreHoriz, stringResource(R.string.profile_more)) { more = true } }
                        ProfileMoreMenu(graph, userId, username, full, more, onDismiss = { more = false }, onQr = { showQr = true })
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
                val rows = buildList {
                    if (bio.isNotBlank()) add("bio")
                    if (username != null) add("username")
                    if (phone.isNotBlank()) add("phone")
                    if (birthday != null) add("birthday")
                }
                rows.forEachIndexed { index, row ->
                    val position = com.pixelgram.android.ui.components.segmentPosition(index, rows.size)
                    when (row) {
                        "phone" -> SettingsItem(stringResource(R.string.profile_phone), position, subtitle = phoneDetails, icon = Icons.Rounded.Phone) {
                            HiddenPhone("+" + formatPhone(phone), hidden = appearance.hidePhone && self)
                        }
                        "username" -> SettingsItem("@$username", position, stringResource(R.string.profile_username), Icons.Rounded.AlternateEmail)
                        "bio" -> SettingsItem(bio, position, stringResource(R.string.profile_bio), Icons.Rounded.Info)
                        "birthday" -> SettingsItem(
                            listOfNotNull(birthday!!.day.toString(), java.text.DateFormatSymbols.getInstance().months[birthday.month - 1], birthday.year.takeIf { it > 0 }?.toString()).joinToString(" "),
                            position,
                            stringResource(R.string.profile_birthday),
                            Icons.Rounded.Cake,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp + com.pixelgram.android.ui.components.LocalBottomBarPadding.current))
        }
    }
    if (showQr && username != null) {
        val context = LocalContext.current
        val link = "https://t.me/$username"
        ModalBottomSheet(onDismissRequest = { showQr = false }, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExpressiveQrCode(link, Modifier.fillMaxWidth(0.85f).aspectRatio(1f))
                Text("@$username", style = MaterialTheme.typography.titleLargeEmphasized)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showQr = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_close)) }
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
private fun ProfileMoreMenu(graph: AppGraph, userId: Long, username: String?, full: TdApi.UserFullInfo?, expanded: Boolean, onDismiss: () -> Unit, onQr: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var renaming by remember { mutableStateOf(false) }
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
    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
        if (username != null) androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.profile_share)) }, leadingIcon = { Icon(Icons.Rounded.QrCode2, null) }, onClick = { onDismiss(); onQr() })
        androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.profile_rename)) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { onDismiss(); renaming = true })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_local_photo)) },
            leadingIcon = { Icon(Icons.Rounded.AddAPhoto, null) },
            onClick = {
                onDismiss()
                photo.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
        if (graph.store.localName(userId) != null || graph.store.localPhoto(userId) != null) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_reset_local)) },
                leadingIcon = { Icon(Icons.Rounded.RestartAlt, null) },
                onClick = {
                    onDismiss()
                    graph.store.setLocalName(userId, null)
                    graph.store.setLocalPhoto(userId, null)
                },
            )
        }
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(if (blocked) R.string.profile_unblock else R.string.profile_block), color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Rounded.Block, null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                scope.launch { runCatching { graph.td.send(TdApi.SetMessageSenderBlockList(TdApi.MessageSenderUser(userId), if (blocked) null else TdApi.BlockListMain())) } }
            },
        )
    }
    if (renaming) {
        var name by remember { mutableStateOf(graph.store.localName(userId) ?: graph.store.snapshot.value.users[userId]?.name.orEmpty()) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.profile_rename)) },
            text = { androidx.compose.material3.OutlinedTextField(value = name, onValueChange = { name = it.take(64) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { graph.store.setLocalName(userId, name); renaming = false }) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.action_cancel)) } },
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
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
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
                Button(onClick = { open = false }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_understood)) }
            }
        }
    }
}
