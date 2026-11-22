package com.pixelgram.android.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

class MenuEntry(val icon: ImageVector, val label: String, val error: Boolean = false, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveMenu(expanded: Boolean, onDismiss: () -> Unit, groups: List<List<MenuEntry>>) {
    val shown = groups.filter { it.isNotEmpty() }
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismiss) {
        shown.forEachIndexed { g, entries ->
            if (g > 0) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(g, shown.size)) {
                entries.forEachIndexed { i, entry ->
                    val tint = if (entry.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    DropdownMenuItem(
                        text = { Text(entry.label, color = tint) },
                        leadingIcon = { Icon(entry.icon, null, tint = tint) },
                        shape = MenuDefaults.itemShape(i, entries.size).shape,
                        onClick = {
                            onDismiss()
                            entry.onClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveMenu(expanded: Boolean, onDismiss: () -> Unit, vararg entries: MenuEntry) = ExpressiveMenu(expanded, onDismiss, listOf(entries.toList()))
