package com.pixelgram.android.feature.profile

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
import androidx.compose.material3.Icon
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
            val status = when {
                user == null -> null
                user.bot -> stringResource(R.string.chat_bot)
                user.online -> stringResource(R.string.chat_online)
                else -> stringResource(R.string.chat_last_seen_recently)
            }
            ProfileHeader(
                graph = graph,
                userId = if (userId != 0L) userId else chatId,
                title = title,
                subtitle = status,
                photoFileId = user?.bigPhotoFileId ?: chat?.photo?.big?.id ?: 0,
                minithumb = user?.minithumb ?: chat?.photo?.minithumbnail?.data,
                verified = user?.verified == true,
                showCamera = false,
                onAvatarClick = { if (userId != 0L) navigator.open(ProfilePhotosKey(userId)) },
                topStart = {
                    if (onOpenDrawer != null) IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) }
                    else IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                topEnd = {
                    if (username != null) IconButton(onClick = { showQr = true }) { Icon(Icons.Rounded.QrCode2, stringResource(R.string.profile_qr)) }
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
                    if (username != null) ProfileAction(Icons.Rounded.QrCode2, stringResource(R.string.profile_share)) { showQr = true }
                }
            }
            full?.firstProfileAudio?.let { audio ->
                Spacer(Modifier.height(16.dp))
                ProfileMusicCard(audio) {
                    scope.launch {
                        val all = runCatching { graph.td.send(TdApi.GetUserProfileAudios(userId, 0, 100)).audios.toList() }.getOrNull()
                        graph.music.play(all?.takeIf { it.isNotEmpty() } ?: listOf(audio), 0)
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
        ModalBottomSheet(onDismissRequest = { showQr = false }) {
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

@Composable
private fun RowScope.ProfileAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null) }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun ProfileMusicCard(audio: TdApi.Audio, onClick: () -> Unit) {
    val graph = com.pixelgram.android.LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val coverId = audio.albumCoverThumbnail?.file?.id ?: 0
    LaunchedEffect(coverId) { if (coverId != 0) graph.store.requestFileAsync(coverId) }
    val cover: Any? = snapshot.files[coverId]?.let { java.io.File(it) } ?: audio.albumCoverMinithumbnail?.data
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(72.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) coil.compose.AsyncImage(model = cover, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(72.dp))
                else Icon(Icons.Rounded.MusicNote, null)
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(audio.title.ifBlank { audio.fileName }, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1)
                if (audio.performer.isNotBlank()) Text(audio.performer, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(stringResource(R.string.profile_music), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(32.dp))
        }
    }
}
