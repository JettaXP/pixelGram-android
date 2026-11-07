package com.pixelgram.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Devices
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
import androidx.compose.material3.TopAppBar
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
import com.pixelgram.android.nav.SessionsKey
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.HiddenPhone
import com.pixelgram.android.ui.components.SegmentPosition
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.util.formatPhone
import org.drinkless.tdlib.TdApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsTab(graph: AppGraph, navigator: Navigator, onOpenDrawer: () -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val me = snapshot.users[snapshot.meId]
    var confirmLogout by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.titleLargeEmphasized) },
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsGroup {
                SettingsItem(
                    title = me?.name.orEmpty(),
                    position = SegmentPosition.Single,
                    onClick = { navigator.open(ProfileKey(snapshot.meId)) },
                    trailing = { Icon(Icons.Rounded.QrCode2, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    subtitle = me?.username?.let { "@$it" },
                )
            }
            if (me != null) {
                Row(Modifier.padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
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
            SettingsGroup(stringResource(R.string.settings_account)) {
                SettingsItem(
                    title = stringResource(R.string.settings_devices),
                    subtitle = stringResource(R.string.settings_devices_sub),
                    icon = Icons.Rounded.Devices,
                    position = SegmentPosition.Top,
                    onClick = { navigator.open(SessionsKey) },
                )
                SettingsItem(
                    title = stringResource(R.string.settings_logout),
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    iconBackground = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    position = SegmentPosition.Bottom,
                    onClick = { confirmLogout = true },
                )
            }
            Spacer(Modifier.height(24.dp))
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
