package com.pixelgram.android.ui.components

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.rounded.Link
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

enum class SegmentPosition { Single, Top, Middle, Bottom }

private val Outer = 24.dp
private val Inner = 4.dp

fun segmentShape(position: SegmentPosition) = when (position) {
    SegmentPosition.Single -> RoundedCornerShape(Outer)
    SegmentPosition.Top -> RoundedCornerShape(Outer, Outer, Inner, Inner)
    SegmentPosition.Middle -> RoundedCornerShape(Inner)
    SegmentPosition.Bottom -> RoundedCornerShape(Inner, Inner, Outer, Outer)
}

fun segmentPosition(index: Int, count: Int) = when {
    count == 1 -> SegmentPosition.Single
    index == 0 -> SegmentPosition.Top
    index == count - 1 -> SegmentPosition.Bottom
    else -> SegmentPosition.Middle
}

@Composable
fun segmentedColors(container: Color = MaterialTheme.colorScheme.surfaceContainer) = ListItemDefaults.segmentedColors(containerColor = container)

fun SegmentPosition.index(): Pair<Int, Int> = when (this) {
    SegmentPosition.Single -> 0 to 1
    SegmentPosition.Top -> 0 to 3
    SegmentPosition.Middle -> 1 to 3
    SegmentPosition.Bottom -> 2 to 3
}

@Composable
fun SettingsGroup(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 8.dp),
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsItem(
    title: String,
    position: SegmentPosition,
    subtitle: String? = null,
    icon: ImageVector? = null,
    painter: Painter? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    iconBackground: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconShape: androidx.compose.ui.graphics.Shape = CircleShape,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    pill: Boolean = false,
    headlineContent: (@Composable () -> Unit)? = null,
    link: String? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val linkContext = androidx.compose.ui.platform.LocalContext.current
    @Suppress("NAME_SHADOWING")
    val link = androidx.compose.runtime.remember(title, link) { link?.takeUnless { it.startsWith("pg://") } ?: com.pixelgram.android.nav.SettingsLinks.forTitle(linkContext, title) ?: link }
    val linkDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (link != null && linkDialog.value) SettingLinkDialog(title, link) { linkDialog.value = false }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    @Suppress("NAME_SHADOWING")
    val onLongClick = onLongClick ?: if (link != null) ({
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        linkDialog.value = true
    }) else null
    val (index, count) = position.index()
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val highlightId = com.pixelgram.android.nav.SettingsLinks.highlight
    val highlighted = highlightId != null && androidx.compose.runtime.remember(highlightId, title) { runCatching { linkContext.getString(highlightId as Int) == title }.getOrDefault(false) }
    val glow = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(highlighted) {
        if (!highlighted) return@LaunchedEffect
        kotlinx.coroutines.delay(350)
        repeat(2) {
            glow.animateTo(1f, androidx.compose.animation.core.tween(280))
            glow.animateTo(0f, androidx.compose.animation.core.tween(420))
        }
        com.pixelgram.android.nav.SettingsLinks.highlight = null
    }
    val requester = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    androidx.compose.runtime.LaunchedEffect(highlighted) { if (highlighted) requester.bringIntoView() }
    val leading: (@Composable () -> Unit)? = if (icon != null || painter != null) {
        {
            Box(Modifier.size(40.dp).clip(iconShape).background(iconBackground), contentAlignment = Alignment.Center) {
                if (icon != null) Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                else if (painter != null) Icon(painter, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
        }
    } else {
        null
    }
    val trailingSlot: (@Composable () -> Unit)? = trailing?.let { content -> { Row(verticalAlignment = Alignment.CenterVertically) { content() } } }
    val supporting: (@Composable () -> Unit)? = subtitle?.let { text ->
        if (pill) {
            {
                androidx.compose.material3.Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        } else {
            { Text(text) }
        }
    }
    val headline: @Composable () -> Unit = headlineContent ?: {
        Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
    }
    if (onClick != null || onLongClick != null) {
        SegmentedListItem(
            onClick = onClick ?: {},
            onLongClick = onLongClick,
            shapes = shapes,
            enabled = enabled,
            leadingContent = leading,
            trailingContent = trailingSlot,
            supportingContent = supporting,
            colors = segmentedColors(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primaryContainer, glow.value)),
            modifier = Modifier.bringIntoViewRequester(requester),
            content = headline,
        )
    } else {
        SegmentedListItem(
            shapes = shapes,
            leadingContent = leading,
            trailingContent = trailingSlot,
            supportingContent = supporting,
            colors = segmentedColors(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primaryContainer, glow.value)),
            modifier = Modifier.bringIntoViewRequester(requester),
            content = headline,
        )
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: SegmentPosition,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        position = position,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
    ) {
        PixelSwitch(checked, onCheckedChange, enabled)
    }
}

@Composable
fun PixelSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        thumbContent = {
            Icon(
                if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

@Composable
fun Modifier.settingLink(title: String): Modifier {
    val context = androidx.compose.ui.platform.LocalContext.current
    val link = androidx.compose.runtime.remember(title) { com.pixelgram.android.nav.SettingsLinks.forTitle(context, title) } ?: return this
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    if (open) SettingLinkDialog(title, link) { open = false }
    return this.pointerInput(link) {
        detectTapGestures(onLongPress = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            open = true
        })
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingLinkDialog(title: String, link: String, onDismiss: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val copied = androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.settings_link_copied)
    androidx.compose.material3.BasicAlertDialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            androidx.compose.foundation.layout.Column(Modifier.padding(24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.fillMaxWidth()) {
                    Text(link, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, link)
                            runCatching { context.startActivity(android.content.Intent.createChooser(intent, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            onDismiss()
                        },
                        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text(androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.shot_share)) }
                    androidx.compose.material3.Button(
                        onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(link))
                            Notice.show(copied)
                            onDismiss()
                        },
                        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text(androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.settings_copy_link), maxLines = 1) }
                }
            }
        }
    }
}
