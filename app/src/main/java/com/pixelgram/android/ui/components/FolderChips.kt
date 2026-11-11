package com.pixelgram.android.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.pixelgram.android.settings.FolderTitle

@Immutable
data class FolderTab(val id: Int, val title: String, val icon: String, val unreadChats: Int = 0, val unreadUnmuted: Int = 0)

@Composable
fun FolderChips(
    tabs: List<FolderTab>,
    selected: Int,
    mode: FolderTitle,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.size < 2) return
    val scroll = rememberScrollState()
    val bounds = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    val spec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    LaunchedEffect(selected, bounds[selected], scroll.viewportSize) {
        val (left, width) = bounds[selected] ?: return@LaunchedEffect
        if (scroll.maxValue == 0) return@LaunchedEffect
        val target = (left + width / 2 - scroll.viewportSize / 2).coerceIn(0, scroll.maxValue)
        scroll.animateScrollTo(target, spec)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        tabs.forEach { tab ->
            FolderPill(
                tab = tab,
                selected = tab.id == selected,
                mode = mode,
                onClick = { onSelect(tab.id) },
                modifier = Modifier.onPlaced {
                    val value = it.positionInParent().x.toInt() to it.size.width
                    if (bounds[tab.id] != value) bounds[tab.id] = value
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderPill(tab: FolderTab, selected: Boolean, mode: FolderTitle, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val fade = tween<Color>(SwitchMs, easing = FastOutSlowInEasing)
    val container by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerHigh, fade, label = "pill-container")
    val content by animateColorAsState(if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant, fade, label = "pill-content")
    val dip = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        dip.animateTo(DipAlpha, tween(SwitchMs / 3, easing = FastOutSlowInEasing))
        dip.animateTo(1f, tween(SwitchMs * 2 / 3, easing = FastOutSlowInEasing))
    }
    val showIcon = mode != FolderTitle.Names
    val showName = mode == FolderTitle.NamesAndIcons || mode == FolderTitle.Names || (mode == FolderTitle.ActiveName && selected)
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonShapes(CircleShape, RoundedCornerShape(10.dp), CircleShape),
        colors = ToggleButtonDefaults.colors(
            containerColor = container,
            contentColor = content,
            checkedContainerColor = container,
            checkedContentColor = content,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.height(PillHeight),
    ) {
        Row(
            Modifier
                .graphicsLayer { alpha = dip.value }
                .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showIcon) Icon(folderIcon(tab.icon, selected), contentDescription = tab.title, modifier = Modifier.size(18.dp))
            if (showName) {
                Text(tab.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            if (tab.unreadChats > 0) {
                val badgeColor by animateColorAsState(
                    when {
                        selected -> colors.onPrimaryContainer
                        tab.unreadUnmuted > 0 -> colors.primary
                        else -> colors.outline
                    },
                    fade,
                    label = "pill-badge",
                )
                val badgeContent by animateColorAsState(
                    when {
                        selected -> colors.primaryContainer
                        tab.unreadUnmuted > 0 -> colors.onPrimary
                        else -> colors.surface
                    },
                    fade,
                    label = "pill-badge-content",
                )
                Badge(containerColor = badgeColor, contentColor = badgeContent) {
                    Text(com.pixelgram.android.ui.theme.countLabel(tab.unreadChats, com.pixelgram.android.ui.theme.LocalAppearance.current.exactNumbers), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val PillHeight = 36.dp
private val ChipGap = 6.dp
private const val SwitchMs = 450
private const val DipAlpha = 0.45f

fun folderIcon(name: String, filled: Boolean): ImageVector {
    val pair = FolderIcons[name] ?: FolderIcons.getValue("Custom")
    return if (filled) pair.first else pair.second
}

private val FolderIcons: Map<String, Pair<ImageVector, ImageVector>> = mapOf(
    "All" to (Icons.Rounded.Forum to Icons.Outlined.Forum),
    "Unread" to (Icons.Rounded.MarkChatUnread to Icons.Outlined.MarkChatUnread),
    "Mention" to (Icons.Rounded.AlternateEmail to Icons.Outlined.AlternateEmail),
    "Unmuted" to (Icons.Rounded.NotificationsActive to Icons.Outlined.NotificationsActive),
    "Private" to (Icons.Rounded.Person to Icons.Outlined.Person),
    "Groups" to (Icons.Rounded.Groups to Icons.Outlined.Groups),
    "Channels" to (Icons.Rounded.Campaign to Icons.Outlined.Campaign),
    "Bots" to (Icons.Rounded.SmartToy to Icons.Outlined.SmartToy),
    "Flag" to (Icons.Rounded.Flag to Icons.Outlined.Flag),
    "Favorite" to (Icons.Rounded.Star to Icons.Outlined.StarOutline),
    "Love" to (Icons.Rounded.Favorite to Icons.Outlined.FavoriteBorder),
    "Home" to (Icons.Rounded.Home to Icons.Outlined.Home),
    "Work" to (Icons.Rounded.Work to Icons.Outlined.Work),
    "Study" to (Icons.Rounded.School to Icons.Outlined.School),
    "Book" to (Icons.AutoMirrored.Rounded.MenuBook to Icons.AutoMirrored.Outlined.MenuBook),
    "Trade" to (Icons.AutoMirrored.Rounded.TrendingUp to Icons.AutoMirrored.Outlined.TrendingUp),
    "Money" to (Icons.Rounded.Payments to Icons.Outlined.Payments),
    "Travel" to (Icons.Rounded.Luggage to Icons.Outlined.Luggage),
    "Airplane" to (Icons.Rounded.Flight to Icons.Outlined.Flight),
    "Game" to (Icons.Rounded.SportsEsports to Icons.Outlined.SportsEsports),
    "Sport" to (Icons.Rounded.SportsSoccer to Icons.Outlined.SportsSoccer),
    "Party" to (Icons.Rounded.Celebration to Icons.Outlined.Celebration),
    "Mask" to (Icons.Rounded.TheaterComedy to Icons.Outlined.TheaterComedy),
    "Crown" to (Icons.Rounded.WorkspacePremium to Icons.Outlined.WorkspacePremium),
    "Cat" to (Icons.Rounded.Pets to Icons.Outlined.Pets),
    "Flower" to (Icons.Rounded.LocalFlorist to Icons.Outlined.LocalFlorist),
    "Note" to (Icons.Rounded.MusicNote to Icons.Outlined.MusicNote),
    "Palette" to (Icons.Rounded.Palette to Icons.Outlined.Palette),
    "Light" to (Icons.Rounded.Lightbulb to Icons.Outlined.Lightbulb),
    "Like" to (Icons.Rounded.ThumbUp to Icons.Outlined.ThumbUp),
    "Setup" to (Icons.Rounded.Settings to Icons.Outlined.Settings),
    "Custom" to (Icons.Rounded.Folder to Icons.Outlined.Folder),
)
