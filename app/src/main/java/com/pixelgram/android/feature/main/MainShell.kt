package com.pixelgram.android.feature.main

import androidx.compose.material.icons.rounded.Lock
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateContentSize
import com.pixelgram.android.nav.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
import com.pixelgram.android.settings.NavBarStyle
import com.pixelgram.android.settings.ThemeMode
import com.pixelgram.android.td.LIST_ARCHIVE
import com.pixelgram.android.td.LIST_MAIN
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.CapsuleBar
import com.pixelgram.android.ui.components.CapsuleItem
import com.pixelgram.android.ui.components.LocalBottomBarPadding
import com.pixelgram.android.ui.components.HiddenPhone
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val darkNow = com.pixelgram.android.ui.theme.LocalDarkTheme.current
    val shellPrefs by graph.appearance.state.collectAsState()
    ModalNavigationDrawer(
        gesturesEnabled = shellPrefs.showDrawer || drawer.isOpen,
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
                        DrawerItem.Contacts -> navigator.open(com.pixelgram.android.nav.ContactsKey)
                        DrawerItem.Pixel -> navigator.open(PixelSettingsKey())
                        DrawerItem.Settings -> navigator.open(com.pixelgram.android.nav.SettingsKey)
                        DrawerItem.Theme -> graph.appearance.update {
                            it.copy(themeMode = if (darkNow) ThemeMode.Light else ThemeMode.Dark)
                        }
                    }
                },
            )
        },
    ) {
        val appearance by graph.appearance.state.collectAsState()
        val unread by remember { graph.store.snapshot.map { it.unread[LIST_MAIN]?.chats ?: 0 }.distinctUntilChanged() }.collectAsState(0)
        val labels = mapOf(
            MainTab.Chats to stringResource(R.string.tab_chats),
            MainTab.Contacts to stringResource(R.string.tab_contacts),
            MainTab.Profile to stringResource(R.string.tab_profile),
            MainTab.Settings to stringResource(R.string.tab_settings),
        )
        val tabs = appearance.navTabs.map { item ->
            when (item) {
                MainTab.Chats -> MainTabItem(item, Icons.Rounded.Chat, Icons.Outlined.Chat, labels.getValue(item), unread)
                MainTab.Contacts -> MainTabItem(item, Icons.Rounded.People, Icons.Outlined.People, labels.getValue(item))
                MainTab.Profile -> MainTabItem(item, Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle, labels.getValue(item))
                MainTab.Settings -> MainTabItem(item, Icons.Rounded.Settings, Icons.Outlined.Settings, labels.getValue(item))
            }
        }
        LaunchedEffect(appearance.navTabs) { if (tab !in appearance.navTabs) tab = MainTab.Chats }
        val density = androidx.compose.ui.platform.LocalDensity.current
        var barHeight by remember { mutableStateOf(0.dp) }
        val floating = appearance.navBarStyle == NavBarStyle.Expressive
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            CompositionLocalProvider(LocalBottomBarPadding provides if (floating) barHeight else 0.dp) {
                val order = appearance.navTabs
                val pager = androidx.compose.foundation.pager.rememberPagerState(order.indexOf(tab).coerceAtLeast(0)) { order.size }
                var jumping by remember { mutableStateOf(false) }
                LaunchedEffect(pager.settledPage, order) { if (!jumping) order.getOrNull(pager.settledPage)?.let { if (it != tab) tab = it } }
                LaunchedEffect(tab, order) {
                    val index = order.indexOf(tab)
                    if (index >= 0 && index != pager.targetPage) {
                        jumping = true
                        try {
                            pager.animateScrollToPage(index, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 700f))
                        } finally {
                            jumping = false
                        }
                    }
                }
                val openDrawer: (() -> Unit)? = if (appearance.showDrawer) ({ scope.launch { drawer.open() } }) else null
                val drawerPull = remember(openDrawer) {
                    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                        var pulled = 0f
                        var locked = false
                        override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                            if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) return androidx.compose.ui.geometry.Offset(available.x, 0f)
                            if (consumed.x != 0f) locked = true
                            if (locked) return androidx.compose.ui.geometry.Offset(available.x, 0f)
                            if (openDrawer == null || available.x <= 0f) {
                                pulled = 0f
                                return androidx.compose.ui.geometry.Offset.Zero
                            }
                            pulled += available.x
                            if (pulled > with(density) { 48.dp.toPx() }) {
                                pulled = 0f
                                openDrawer()
                            }
                            return androidx.compose.ui.geometry.Offset(available.x, 0f)
                        }

                        override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                            if (locked) {
                                locked = false
                                pulled = 0f
                            }
                            return androidx.compose.ui.unit.Velocity.Zero
                        }

                        override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                            if (consumed.x != 0f) return androidx.compose.ui.unit.Velocity(available.x, 0f)
                            pulled = 0f
                            return if (openDrawer != null && available.x > 0f) androidx.compose.ui.unit.Velocity(available.x, 0f) else androidx.compose.ui.unit.Velocity.Zero
                        }
                    }
                }
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pager,
                    key = { order[it].name },
                    beyondViewportPageCount = 0,
                    modifier = Modifier.fillMaxSize().padding(bottom = if (floating) 0.dp else barHeight),
                ) { page ->
                    val current = order[page]
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).let { kotlin.math.abs(it) }.coerceIn(0f, 1f)
                            alpha = 1f - offset * 0.35f
                        },
                    ) {
                    when (current) {
                        MainTab.Chats -> Box(Modifier.nestedScroll(drawerPull)) { ChatsTab(graph, navigator, list, onOpenDrawer = openDrawer, onCloseArchive = { list = LIST_MAIN }, onOpenArchive = { list = LIST_ARCHIVE }) }
                        MainTab.Contacts -> ContactsTab(graph, navigator, onOpenDrawer = openDrawer)
                        MainTab.Profile -> {
                            val me by graph.store.snapshot.collectAsState()
                            ProfileScreen(graph, me.meId, 0L, navigator, onOpenDrawer = openDrawer, onOpenSettings = { navigator.open(com.pixelgram.android.nav.SettingsKey) })
                        }
                        MainTab.Settings -> SettingsTab(graph, navigator, onOpenDrawer = openDrawer)
                    }
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { with(density) { barHeight = it.height.toDp() } },
            ) {
                if (tabs.size > 1 && appearance.showBottomBar) {
                    MainNavBar(appearance.navBarStyle, tabs, tab, { tab = it }, appearance.capsuleInset, appearance.capsuleSpacing, appearance.capsuleHeight)
                }
            }
        }
    }
}

internal data class MainTabItem(val tab: MainTab, val selectedIcon: ImageVector, val icon: ImageVector, val label: String, val badge: Int = 0)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MainNavBar(style: NavBarStyle, tabs: List<MainTabItem>, selected: MainTab, onSelect: (MainTab) -> Unit, inset: Int = 12, spacing: Int = 4, height: Int = 64) {
    val icon: @Composable (MainTabItem) -> Unit = { item ->
        BadgedBox(badge = { if (item.badge > 0) Badge { com.pixelgram.android.ui.components.AnimatedCounter(item.badge, androidx.compose.material3.LocalTextStyle.current, format = { n -> com.pixelgram.android.ui.theme.countLabel(n, com.pixelgram.android.ui.theme.LocalAppearance.current.exactNumbers) }) } }) {
            Icon(if (item.tab == selected) item.selectedIcon else item.icon, contentDescription = null)
        }
    }
    when (style) {
        NavBarStyle.Expressive -> CapsuleBar(
            items = tabs.map { CapsuleItem(it.tab, it.selectedIcon, it.icon, it.label, it.badge) },
            selected = selected,
            onSelect = onSelect,
            inset = inset.dp,
            spacing = spacing.dp,
            height = height.dp,
        )
        NavBarStyle.Classic -> NavigationBar {
            tabs.forEach { item ->
                NavigationBarItem(
                    selected = item.tab == selected,
                    onClick = { onSelect(item.tab) },
                    icon = { icon(item) },
                    label = { Text(item.label) },
                )
            }
        }
    }
}

private typealias DrawerItem = com.pixelgram.android.settings.DrawerEntry

@Composable
private fun DrawerContent(graph: AppGraph, onItem: (DrawerItem) -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val me = snapshot.users[snapshot.meId]
    val banners by graph.remote.banners.collectAsState()
    LaunchedEffect(snapshot.meId) { if (snapshot.meId != 0L) graph.remote.want(snapshot.meId) }
    val banner = banners[snapshot.meId]
    val accountsContext = androidx.compose.ui.platform.LocalContext.current
    var accountsOpen by remember { mutableStateOf(com.pixelgram.android.account.Accounts.drawerOpen(accountsContext)) }
    ModalDrawerSheet(modifier = Modifier.width(DrawerWidth)) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp, bottom = 12.dp),
        ) {
            Column {
                if (banner != null) {
                    var ratio by remember(banner) { mutableStateOf(3f) }
                    val fade = MaterialTheme.colorScheme.surfaceContainerHigh
                    Box(Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))) {
                        coil.compose.AsyncImage(
                            model = banner,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            onSuccess = { state ->
                                val size = state.painter.intrinsicSize
                                if (size.width > 0f && size.height > 0f) ratio = (size.width / size.height).coerceIn(1f, 4f)
                            },
                            modifier = Modifier.matchParentSize(),
                        )
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.55f).background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, fade.copy(alpha = 0.6f), fade)),
                            ),
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
                ) {
                    Avatar(
                        title = me?.name.orEmpty(),
                        seed = snapshot.meId,
                        path = me?.photoFileId?.let { snapshot.files[it] },
                        minithumb = null,
                        size = 52.dp,
                        onNeedFile = { me?.photoFileId?.let(graph.store::requestFileAsync) },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(me?.name.orEmpty(), style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (me != null) HiddenPhone("+" + me.phone, hidden = appearance.hidePhone)
                    }
                    val turn by androidx.compose.animation.core.animateFloatAsState(if (accountsOpen) 180f else 0f, label = "accounts-arrow")
                    androidx.compose.material3.FilledTonalIconButton(onClick = { accountsOpen = !accountsOpen; com.pixelgram.android.account.Accounts.setDrawerOpen(accountsContext, accountsOpen) }) {
                        Icon(Icons.Outlined.ExpandMore, stringResource(R.string.accounts_toggle), Modifier.graphicsLayer { rotationZ = turn })
                    }
                }
            }
        }
        val context = androidx.compose.ui.platform.LocalContext.current
        val myPhoto = me?.photoFileId?.let { snapshot.files[it] }
        LaunchedEffect(me?.id, me?.name, me?.phone, myPhoto) {
            val current = me ?: return@LaunchedEffect
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.pixelgram.android.account.Accounts.remember(context, current.name, current.phone, current.id, myPhoto) }
        }
        androidx.compose.animation.AnimatedVisibility(accountsOpen) {
            val active = com.pixelgram.android.account.Accounts.active(context)
            val others = remember(accountsOpen) { com.pixelgram.android.account.Accounts.all(context).filter { it.id != active && it.userId != 0L } }
            Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
                val count = others.size + 1
                others.forEachIndexed { index, account ->
                    androidx.compose.material3.SegmentedListItem(
                        onClick = { com.pixelgram.android.account.Accounts.switchTo(context, account.id) },
                        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, count),
                        colors = com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                        leadingContent = { Avatar(title = account.name, seed = account.userId, path = account.photo, minithumb = null, size = 36.dp) },
                    ) { Text(account.name.ifBlank { stringResource(R.string.accounts_unnamed) }) }
                }
                androidx.compose.material3.SegmentedListItem(
                    onClick = { com.pixelgram.android.account.Accounts.add(context) },
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(others.size, count),
                    colors = com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                    leadingContent = { Icon(Icons.Outlined.PersonAdd, null) },
                ) { Text(stringResource(R.string.accounts_add)) }
            }
        }
        val dark = com.pixelgram.android.ui.theme.LocalDarkTheme.current
        val entries = appearance.drawerOrder.filter { it in appearance.drawerItems }
        val lockContext = androidx.compose.ui.platform.LocalContext.current
        val lockShown = com.pixelgram.android.feature.security.PasscodeLock.enabled(lockContext)
        val total = entries.size + if (lockShown) 1 else 0
        Column(
            Modifier.padding(horizontal = 12.dp).animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
            verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap),
        ) {
            entries.forEachIndexed { index, item ->
                val (icon, label) = when (item) {
                    DrawerItem.Saved -> Icons.Outlined.BookmarkBorder to stringResource(R.string.drawer_saved)
                    DrawerItem.Archive -> Icons.Outlined.Archive to stringResource(R.string.drawer_archive)
                    DrawerItem.Contacts -> Icons.Outlined.People to stringResource(R.string.tab_contacts)
                    DrawerItem.Pixel -> null to stringResource(R.string.pixel_settings)
                    DrawerItem.Settings -> Icons.Outlined.Settings to stringResource(R.string.tab_settings)
                    DrawerItem.Theme -> (if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode) to stringResource(if (dark) R.string.drawer_day else R.string.drawer_night)
                }
                androidx.compose.runtime.key(item) {
                    androidx.compose.material3.SegmentedListItem(
                        onClick = { onItem(item) },
                        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, total),
                        colors = com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                        leadingContent = {
                            if (icon != null) Icon(icon, null) else Icon(painterResource(R.drawable.ic_pixelgram), null)
                        },
                    ) { Text(label) }
                }
            }
            if (lockShown) {
                androidx.compose.material3.SegmentedListItem(
                    onClick = { com.pixelgram.android.feature.security.PasscodeLock.lockNow(lockContext) },
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(entries.size, total),
                    colors = com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                    leadingContent = { Icon(androidx.compose.material.icons.Icons.Rounded.Lock, null) },
                ) { Text(stringResource(R.string.passcode_lock_now)) }
            }
        }
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

private val DrawerWidth = 300.dp
