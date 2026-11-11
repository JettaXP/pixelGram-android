package com.pixelgram.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import org.drinkless.tdlib.TdApi
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.SessionsKey
import com.pixelgram.android.ui.components.SegmentPosition
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem

enum class Tone { Primary, Secondary, Tertiary, Error }

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@androidx.compose.runtime.Composable
fun SettingsSection.shape(): androidx.compose.ui.graphics.Shape = when (this) {
    SettingsSection.Account -> androidx.compose.material3.MaterialShapes.Cookie9Sided
    SettingsSection.Chats -> androidx.compose.material3.MaterialShapes.Clover4Leaf
    SettingsSection.Stickers -> androidx.compose.material3.MaterialShapes.Sunny
    SettingsSection.Privacy -> androidx.compose.material3.MaterialShapes.Pentagon
    SettingsSection.Notifications -> androidx.compose.material3.MaterialShapes.Burst
    SettingsSection.Data -> androidx.compose.material3.MaterialShapes.Pill
    SettingsSection.Folders -> androidx.compose.material3.MaterialShapes.Arch
    SettingsSection.Power -> androidx.compose.material3.MaterialShapes.Cookie6Sided
    else -> androidx.compose.material3.MaterialShapes.Circle
}.toShape()

fun Tone.container(scheme: ColorScheme): Color = when (this) {
    Tone.Primary -> scheme.primaryContainer
    Tone.Secondary -> scheme.secondaryContainer
    Tone.Tertiary -> scheme.tertiaryContainer
    Tone.Error -> scheme.errorContainer
}

fun Tone.content(scheme: ColorScheme): Color = when (this) {
    Tone.Primary -> scheme.onPrimaryContainer
    Tone.Secondary -> scheme.onSecondaryContainer
    Tone.Tertiary -> scheme.onTertiaryContainer
    Tone.Error -> scheme.onErrorContainer
}

enum class SettingsSection(val title: Int, val subtitle: Int, val icon: ImageVector, val tone: Tone) {
    Account(R.string.section_account, R.string.section_account_sub, Icons.Rounded.AccountCircle, Tone.Primary),
    Chats(R.string.section_chats, R.string.section_chats_sub, Icons.Rounded.ChatBubble, Tone.Tertiary),
    Stickers(R.string.section_stickers, R.string.section_stickers_sub, Icons.Rounded.EmojiEmotions, Tone.Secondary),
    Privacy(R.string.section_privacy, R.string.section_privacy_sub, Icons.Rounded.Key, Tone.Primary),
    Notifications(R.string.section_notifications, R.string.section_notifications_sub, Icons.Rounded.Notifications, Tone.Error),
    Data(R.string.section_data, R.string.section_data_sub, Icons.Rounded.DataUsage, Tone.Secondary),
    Folders(R.string.section_folders, R.string.section_folders_sub, Icons.Rounded.Folder, Tone.Tertiary),
    Power(R.string.section_power, R.string.section_power_sub, Icons.Rounded.BatterySaver, Tone.Primary),
    Language(R.string.section_language, R.string.section_language_sub, Icons.Rounded.Language, Tone.Secondary),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSectionScreen(graph: AppGraph, section: String, navigator: Navigator) {
    val current = SettingsSection.entries.firstOrNull { it.name == section } ?: SettingsSection.Account
    val collapse = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(collapse.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.MediumFlexibleTopAppBar(
                scrollBehavior = collapse,
                title = { Text(stringResource(current.title)) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (current == SettingsSection.Account) {
                AccountEditor(graph)
                return@Column
            }
            if (current == SettingsSection.Privacy) {
                SettingsGroup(stringResource(R.string.section_security)) {
                    SettingsItem(
                        title = stringResource(R.string.settings_devices),
                        subtitle = stringResource(R.string.settings_devices_sub),
                        icon = Icons.Rounded.Devices,
                        position = SegmentPosition.Single,
                        onClick = { navigator.open(SessionsKey) },
                    )
                }
            }
            ComingSoon(current.icon, current.tone)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ComingSoon(icon: ImageVector, tone: Tone, title: String = stringResource(R.string.section_soon_title)) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(88.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(tone.container(scheme)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tone.content(scheme), modifier = Modifier.size(40.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.pixel_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountEditor(graph: AppGraph) {
    val snapshot by graph.store.snapshot.collectAsState()
    val me = snapshot.users[snapshot.meId]
    var first by rememberSaveable(me?.id) { mutableStateOf(me?.firstName.orEmpty()) }
    var last by rememberSaveable(me?.id) { mutableStateOf(me?.lastName.orEmpty()) }
    var username by rememberSaveable(me?.id) { mutableStateOf(me?.username.orEmpty()) }
    var bio by rememberSaveable(me?.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(snapshot.meId) {
        if (snapshot.meId != 0L) bio = runCatching { graph.td.send(TdApi.GetUserFullInfo(snapshot.meId)).bio?.text.orEmpty() }.getOrDefault(bio)
    }
    val saved = stringResource(R.string.account_saved)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(first, { first = it.take(64) }, label = { Text(stringResource(R.string.account_first_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(last, { last = it.take(64) }, label = { Text(stringResource(R.string.account_last_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            username,
            { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(32) },
            label = { Text(stringResource(R.string.profile_username)) },
            prefix = { Text("@") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            bio,
            { bio = it.take(70) },
            label = { Text(stringResource(R.string.profile_bio)) },
            supportingText = { Text("${bio.length} / 70") },
            modifier = Modifier.fillMaxWidth(),
        )
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
        Button(
            onClick = {
                busy = true
                scope.launch {
                    val errors = listOfNotNull(
                        runCatching { graph.td.send(TdApi.SetName(first.trim(), last.trim())) }.exceptionOrNull(),
                        runCatching { graph.td.send(TdApi.SetBio(bio.trim())) }.exceptionOrNull(),
                        if (username != me?.username.orEmpty()) runCatching { graph.td.send(TdApi.SetUsername(username)) }.exceptionOrNull() else null,
                    )
                    busy = false
                    status = errors.firstOrNull()?.let { (it as? com.pixelgram.android.td.TdException)?.error?.message ?: it.message } ?: saved
                }
            },
            enabled = !busy && first.isNotBlank(),
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) LoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(R.string.account_save))
        }
    }
}
