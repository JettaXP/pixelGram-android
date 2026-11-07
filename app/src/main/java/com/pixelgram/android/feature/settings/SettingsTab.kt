package com.pixelgram.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.PixelSettingsKey
import com.pixelgram.android.nav.ProfileKey
import com.pixelgram.android.nav.SettingsSectionKey
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.HiddenPhone
import com.pixelgram.android.ui.components.SegmentPosition
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.segmentPosition
import com.pixelgram.android.util.formatPhone
import org.drinkless.tdlib.TdApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsTab(graph: AppGraph, navigator: Navigator, onOpenDrawer: () -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val me = snapshot.users[snapshot.meId]
    var confirmLogout by remember { mutableStateOf(false) }
    val pickAvatar = com.pixelgram.android.feature.profile.rememberAvatarPicker(graph)
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            com.pixelgram.android.feature.profile.ProfileHeader(
                graph = graph,
                userId = snapshot.meId,
                title = me?.name.orEmpty(),
                subtitle = me?.username?.let { "@$it" },
                photoFileId = me?.bigPhotoFileId ?: 0,
                minithumb = me?.minithumb,
                verified = me?.verified == true,
                showCamera = true,
                onAvatarClick = pickAvatar,
                topStart = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) }
                },
                topEnd = {
                    if (me?.username != null) IconButton(onClick = { navigator.open(ProfileKey(snapshot.meId)) }) { Icon(Icons.Rounded.QrCode2, stringResource(R.string.profile_qr)) }
                },
            )
            if (me != null) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                    HiddenPhone("+" + formatPhone(me.phone), hidden = appearance.hidePhone)
                }
            }
            SettingsGroup {
                SettingsItem(
                    title = stringResource(R.string.pixel_settings),
                    subtitle = stringResource(R.string.pixel_settings_sub),
                    painter = painterResource(R.drawable.ic_pixelgram),
                    iconBackground = MaterialTheme.colorScheme.primary,
                    iconTint = MaterialTheme.colorScheme.onPrimary,
                    position = SegmentPosition.Single,
                    onClick = { navigator.open(PixelSettingsKey()) },
                )
            }
            SettingsGroup {
                val sections = SettingsSection.entries
                sections.forEachIndexed { index, section ->
                    SettingsItem(
                        title = stringResource(section.title),
                        subtitle = stringResource(section.subtitle),
                        icon = section.icon,
                        iconBackground = section.tone.container(MaterialTheme.colorScheme),
                        iconTint = section.tone.content(MaterialTheme.colorScheme),
                        position = segmentPosition(index, sections.size),
                        onClick = { navigator.open(SettingsSectionKey(section.name)) },
                    )
                }
            }
            SettingsGroup {
                SettingsItem(
                    title = stringResource(R.string.settings_logout),
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    iconBackground = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    position = SegmentPosition.Single,
                    onClick = { confirmLogout = true },
                )
            }
            Spacer(Modifier.height(24.dp + com.pixelgram.android.ui.components.LocalBottomBarPadding.current))
        }
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.settings_logout)) },
            text = { Text(stringResource(R.string.settings_logout_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmLogout = false
                        graph.td.fire(TdApi.LogOut())
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.settings_logout)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmLogout = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
