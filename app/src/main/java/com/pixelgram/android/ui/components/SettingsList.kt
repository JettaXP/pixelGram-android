package com.pixelgram.android.ui.components

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
    val linkDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (link != null && linkDialog.value) SettingLinkDialog(title, link) { linkDialog.value = false }
    @Suppress("NAME_SHADOWING")
    val onLongClick = onLongClick ?: if (link != null) ({ linkDialog.value = true }) else null
    val (index, count) = position.index()
    val shapes = ListItemDefaults.segmentedShapes(index, count)
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
            colors = segmentedColors(),
            content = headline,
        )
    } else {
        SegmentedListItem(
            shapes = shapes,
            leadingContent = leading,
            trailingContent = trailingSlot,
            supportingContent = supporting,
            colors = segmentedColors(),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingLinkDialog(title: String, link: String, onDismiss: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val copied = androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.settings_link_copied)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(androidx.compose.material.icons.Icons.Rounded.Link, null) },
        title = { Text(title) },
        text = {
            androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Text(link, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(link))
                    Notice.show(copied)
                    onDismiss()
                },
                shapes = androidx.compose.material3.ButtonDefaults.shapes(),
            ) { Text(androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.settings_copy_link)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.action_cancel)) }
        },
    )
}
