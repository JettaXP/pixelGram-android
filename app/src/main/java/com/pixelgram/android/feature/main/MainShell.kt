package com.pixelgram.android.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.feature.chats.ChatsTab
import com.pixelgram.android.feature.contacts.ContactsTab
import com.pixelgram.android.feature.profile.ProfileScreen
import com.pixelgram.android.feature.settings.SettingsTab
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.PixelSettingsKey
import com.pixelgram.android.settings.ThemeMode
import com.pixelgram.android.td.LIST_ARCHIVE
import com.pixelgram.android.td.LIST_MAIN
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.HiddenPhone
import kotlinx.coroutines.launch

enum class MainTab { Chats, Contacts, Profile, Settings }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainShell(graph: AppGraph, navigator: Navigator) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Chats) }
    var list by rememberSaveable { mutableStateOf(LIST_MAIN) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(enabled = !drawer.isOpen && (tab != MainTab.Chats || list == LIST_ARCHIVE)) {
        if (list == LIST_ARCHIVE) list = LIST_MAIN else tab = MainTab.Chats
    }
    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            DrawerContent(
                graph = graph,
                onItem = { item ->
                    scope.launch { drawer.close() }
                    when (item) {
                        DrawerItem.Saved -> graph.store.snapshot.value.meId.takeIf { it != 0L }?.let { navigator.open(ChatKey(it)) }
                        DrawerItem.Archive -> {
                            tab = MainTab.Chats
                            list = LIST_ARCHIVE
                        }
                        DrawerItem.Contacts -> tab = MainTab.Contacts
                        DrawerItem.Pixel -> navigator.open(PixelSettingsKey())
                        DrawerItem.Settings -> tab = MainTab.Settings
                        DrawerItem.Theme -> graph.appearance.update {
                            it.copy(themeMode = if (it.themeMode == ThemeMode.Dark) ThemeMode.Light else ThemeMode.Dark)
                        }
                    }
                },
            )
        },
    ) {
        Scaffold(
            bottomBar = {
                ShortNavigationBar {
                    TabItem(tab == MainTab.Chats, Icons.Rounded.Chat, Icons.Outlined.Chat, stringResource(R.string.tab_chats)) { tab = MainTab.Chats }
                    TabItem(tab == MainTab.Contacts, Icons.Rounded.People, Icons.Outlined.People, stringResource(R.string.tab_contacts)) { tab = MainTab.Contacts }
                    TabItem(tab == MainTab.Profile, Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle, stringResource(R.string.tab_profile)) { tab = MainTab.Profile }
                    TabItem(tab == MainTab.Settings, Icons.Rounded.Settings, Icons.Outlined.Settings, stringResource(R.string.tab_settings)) { tab = MainTab.Settings }
                }
            },
        ) { padding ->
            val fade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
                label = "main-tab",
                modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
            ) { current ->
                val openDrawer: () -> Unit = { scope.launch { drawer.open() } }
                when (current) {
                    MainTab.Chats -> ChatsTab(graph, navigator, list, onOpenDrawer = openDrawer, onCloseArchive = { list = LIST_MAIN })
                    MainTab.Contacts -> ContactsTab(graph, navigator, onOpenDrawer = openDrawer)
                    MainTab.Profile -> {
                        val me by graph.store.snapshot.collectAsState()
                        ProfileScreen(graph, me.meId, 0L, navigator, onOpenDrawer = openDrawer)
                    }
                    MainTab.Settings -> SettingsTab(graph, navigator, onOpenDrawer = openDrawer)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TabItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ShortNavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(if (selected) selectedIcon else icon, contentDescription = null) },
        label = { Text(label) },
    )
}

private enum class DrawerItem { Saved, Archive, Contacts, Pixel, Settings, Theme }

@Composable
private fun DrawerContent(graph: AppGraph, onItem: (DrawerItem) -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val me = snapshot.users[snapshot.meId]
    ModalDrawerSheet {
        Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
            Avatar(
                title = me?.name.orEmpty(),
                seed = snapshot.meId,
                path = me?.photoFileId?.let { snapshot.files[it] },
                minithumb = null,
                size = 64.dp,
                onNeedFile = { me?.photoFileId?.let(graph.store::requestFileAsync) },
            )
            Spacer(Modifier.height(12.dp))
            Text(me?.name.orEmpty(), style = MaterialTheme.typography.titleLargeEmphasized)
            if (me != null) HiddenPhone("+" + me.phone, hidden = appearance.hidePhone)
        }
        val dark = appearance.themeMode == ThemeMode.Dark
        DrawerRow(Icons.Outlined.BookmarkBorder, stringResource(R.string.drawer_saved)) { onItem(DrawerItem.Saved) }
        DrawerRow(Icons.Outlined.Archive, stringResource(R.string.drawer_archive)) { onItem(DrawerItem.Archive) }
        DrawerRow(Icons.Outlined.People, stringResource(R.string.tab_contacts)) { onItem(DrawerItem.Contacts) }
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.pixel_settings)) },
            icon = { Icon(painterResource(R.drawable.ic_pixelgram), null) },
            selected = false,
            onClick = { onItem(DrawerItem.Pixel) },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        DrawerRow(Icons.Outlined.Settings, stringResource(R.string.tab_settings)) { onItem(DrawerItem.Settings) }
        DrawerRow(
            if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            stringResource(if (dark) R.string.drawer_day else R.string.drawer_night),
        ) { onItem(DrawerItem.Theme) }
    }
}

@Composable
private fun DrawerRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
