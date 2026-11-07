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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(graph: AppGraph, userId: Long, chatId: Long, navigator: Navigator, onOpenDrawer: (() -> Unit)? = null) {
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val user = snapshot.users[userId]
    val chat = remember(chatId, snapshot) { if (chatId != 0L) graph.store.chat(chatId) else null }
    var bio by remember { mutableStateOf("") }
    var showQr by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(userId) {
        if (userId != 0L) bio = runCatching { graph.td.send(TdApi.GetUserFullInfo(userId)).bio?.text.orEmpty() }.getOrDefault("")
    }
    val title = user?.name ?: chat?.title.orEmpty()
    val username = user?.username
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (onOpenDrawer != null) IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) }
                    else IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    if (username != null) IconButton(onClick = { showQr = true }) { Icon(Icons.Rounded.QrCode2, stringResource(R.string.profile_qr)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            val photoId = user?.photoFileId ?: chat?.photo?.small?.id ?: 0
            Avatar(
                title = title,
                seed = if (userId != 0L) userId else chatId,
                path = snapshot.files[photoId],
                minithumb = chat?.photo?.minithumbnail?.data,
                size = 128.dp,
                onNeedFile = { graph.store.requestFileAsync(photoId) },
            )
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
            if (user != null) {
                Text(
                    stringResource(if (user.online) R.string.chat_online else R.string.chat_last_seen_recently),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            if (userId != 0L && userId != snapshot.meId) {
                Button(
                    onClick = {
                        scope.launch {
                            val created = runCatching { graph.td.send(TdApi.CreatePrivateChat(userId, false)) }.getOrNull()
                            created?.let { navigator.open(ChatKey(it.id)) }
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, null)
                    Text(stringResource(R.string.profile_message), Modifier.padding(start = 8.dp))
                }
            }
            val phone = user?.phone.orEmpty()
            val showDetails = appearance.phoneDetails && userId != snapshot.meId && phone.isNotBlank()
            val phoneDetails by produceState<String?>(null, phone, showDetails) {
                value = if (!showDetails) null else withContext(Dispatchers.Default) {
                    runCatching { Phones.details(phone) }.getOrNull()?.let { d ->
                        listOfNotNull("${d.flag} ${d.country}", d.carrier).joinToString(" · ")
                    }
                }
            }
            SettingsGroup {
                val rows = buildList {
                    if (user != null && user.phone.isNotBlank()) add("phone")
                    if (username != null) add("username")
                    if (bio.isNotBlank()) add("bio")
                }
                rows.forEachIndexed { index, row ->
                    val position = com.pixelgram.android.ui.components.segmentPosition(index, rows.size)
                    when (row) {
                        "phone" -> SettingsItem(stringResource(R.string.profile_phone), position, subtitle = phoneDetails, icon = Icons.Rounded.Phone) {
                            HiddenPhone("+" + formatPhone(user!!.phone), hidden = appearance.hidePhone && userId == snapshot.meId)
                        }
                        "username" -> SettingsItem("@$username", position, stringResource(R.string.profile_username), Icons.Rounded.AlternateEmail)
                        "bio" -> SettingsItem(bio, position, stringResource(R.string.profile_bio), Icons.Rounded.Info)
                    }
                }
            }
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
