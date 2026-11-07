package com.pixelgram.android.ui.components

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
import androidx.compose.material3.Icon
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
fun SettingsGroup(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Composable
fun SettingsItem(
    title: String,
    position: SegmentPosition,
    subtitle: String? = null,
    icon: ImageVector? = null,
    painter: Painter? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    iconBackground: Color = MaterialTheme.colorScheme.secondaryContainer,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (icon != null || painter != null) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(iconBackground), contentAlignment = Alignment.Center) {
                    if (icon != null) Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                    else if (painter != null) Icon(painter, null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing?.invoke(this)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, enabled = enabled, shape = segmentShape(position), color = MaterialTheme.colorScheme.surfaceContainer, content = content)
    } else {
        Surface(shape = segmentShape(position), color = MaterialTheme.colorScheme.surfaceContainer, content = content)
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
        thumbContent = {
            Icon(
                if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}
