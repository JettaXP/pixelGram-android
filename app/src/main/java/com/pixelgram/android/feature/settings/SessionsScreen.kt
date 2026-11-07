package com.pixelgram.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.TabletMac
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.segmentPosition
import com.pixelgram.android.util.fullDate
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

private fun TdApi.Session.isPixelgram() = applicationName.contains("pixelgram", ignoreCase = true)

private fun TdApi.Session.icon(): ImageVector = when (deviceType) {
    is TdApi.SessionDeviceTypeAndroid -> Icons.Rounded.PhoneAndroid
    is TdApi.SessionDeviceTypeIphone -> Icons.Rounded.PhoneIphone
    is TdApi.SessionDeviceTypeIpad -> Icons.Rounded.TabletMac
    is TdApi.SessionDeviceTypeWindows, is TdApi.SessionDeviceTypeMac, is TdApi.SessionDeviceTypeLinux,
    is TdApi.SessionDeviceTypeUbuntu, is TdApi.SessionDeviceTypeApple -> Icons.Rounded.Computer
    is TdApi.SessionDeviceTypeChrome, is TdApi.SessionDeviceTypeFirefox, is TdApi.SessionDeviceTypeSafari,
    is TdApi.SessionDeviceTypeEdge, is TdApi.SessionDeviceTypeOpera, is TdApi.SessionDeviceTypeBrave,
    is TdApi.SessionDeviceTypeVivaldi -> Icons.Rounded.Language
    else -> Icons.Rounded.Public
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SessionsScreen(graph: AppGraph, navigator: Navigator) {
    var sessions by remember { mutableStateOf<List<TdApi.Session>?>(null) }
    var opened by remember { mutableStateOf<TdApi.Session?>(null) }
    val scope = rememberCoroutineScope()
    val reload: () -> Unit = {
        scope.launch { sessions = runCatching { graph.td.send(TdApi.GetActiveSessions()).sessions.toList() }.getOrDefault(emptyList()) }
    }
    LaunchedEffect(Unit) { reload() }
    val collapse = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(collapse.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.MediumFlexibleTopAppBar(
                scrollBehavior = collapse,
                title = { Text(stringResource(R.string.settings_devices)) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        val list = sessions
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        val current = list.filter { it.isCurrent }
        val others = list.filterNot { it.isCurrent }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsGroup(stringResource(R.string.sessions_this_device)) {
                current.forEachIndexed { i, s -> SessionRow(s, segmentPosition(i, current.size)) { opened = s } }
            }
            if (others.isNotEmpty()) {
                SettingsGroup(stringResource(R.string.sessions_active)) {
                    others.forEachIndexed { i, s -> SessionRow(s, segmentPosition(i, others.size)) { opened = s } }
                }
            }
        }
    }
    opened?.let { session ->
        ModalBottomSheet(onDismissRequest = { opened = null }) {
            SessionDetails(session) {
                scope.launch {
                    runCatching { graph.td.send(TdApi.TerminateSession(session.id)) }
                    opened = null
                    reload()
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: TdApi.Session, position: com.pixelgram.android.ui.components.SegmentPosition, onClick: () -> Unit) {
    val pixel = session.isPixelgram()
    SettingsItem(
        title = session.deviceModel.ifBlank { session.platform },
        subtitle = "${session.applicationName} ${session.applicationVersion} · ${session.location.ifBlank { session.ipAddress }}",
        icon = if (pixel) null else session.icon(),
        painter = if (pixel) painterResource(R.drawable.ic_pixelgram) else null,
        iconBackground = if (pixel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        iconTint = if (pixel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        position = position,
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SessionDetails(session: TdApi.Session, onTerminate: () -> Unit) {
    val pixel = session.isPixelgram()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(88.dp)
                .clip(if (pixel) MaterialShapes.Cookie12Sided.toShape() else MaterialShapes.Cookie9Sided.toShape())
                .background(if (pixel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (pixel) Icon(painterResource(R.drawable.ic_pixelgram), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
            else Icon(session.icon(), null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(session.deviceModel.ifBlank { session.platform }, style = MaterialTheme.typography.headlineSmallEmphasized)
        Text("${session.applicationName} ${session.applicationVersion}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Detail(stringResource(R.string.session_system), listOf(session.platform, session.systemVersion).filter { it.isNotBlank() }.joinToString(" "))
        Detail(stringResource(R.string.session_location), session.location)
        Detail(stringResource(R.string.session_ip), session.ipAddress)
        Detail(stringResource(R.string.session_logged_in), fullDate(session.logInDate))
        Detail(stringResource(R.string.session_last_active), fullDate(session.lastActiveDate))
        Detail(stringResource(R.string.session_official), stringResource(if (session.isOfficialApplication) R.string.yes else R.string.no))
        if (!session.isCurrent) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onTerminate,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.session_terminate)) }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    if (value.isBlank()) return
    androidx.compose.material3.ListItem(
        headlineContent = { Text(value) },
        overlineContent = { Text(label) },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
    )
}
