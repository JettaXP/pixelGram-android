package com.pixelgram.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalBottomBarPadding = compositionLocalOf { 0.dp }

data class CapsuleItem<K>(val key: K, val selectedIcon: ImageVector, val icon: ImageVector, val label: String, val badge: Int = 0)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <K> CapsuleBar(
    items: List<CapsuleItem<K>>,
    selected: K,
    onSelect: (K) -> Unit,
    modifier: Modifier = Modifier,
    inset: Dp = 12.dp,
    spacing: Dp = 4.dp,
    height: Dp = 64.dp,
) {
    val itemHeight = (height - inset * 2).coerceAtLeast(32.dp)
    val shapes = ToggleButtonShapes(CircleShape, RoundedCornerShape(itemHeight / 4), CircleShape)
    Box(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            contentPadding = PaddingValues(inset),
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.width(spacing))
                val active = item.key == selected
                ToggleButton(
                    checked = active,
                    onCheckedChange = { onSelect(item.key) },
                    shapes = shapes,
                    colors = ToggleButtonDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    modifier = Modifier.height(itemHeight),
                ) {
                    BadgedBox(badge = {
                        if (item.badge > 0) Badge { Text(com.pixelgram.android.ui.theme.countLabel(item.badge, com.pixelgram.android.ui.theme.LocalAppearance.current.exactNumbers)) }
                    }) {
                        Icon(if (active) item.selectedIcon else item.icon, contentDescription = item.label)
                    }
                    AnimatedVisibility(
                        visible = active,
                        enter = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                        exit = shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                    ) {
                        Text(item.label, maxLines = 1, modifier = Modifier.padding(start = ToggleButtonDefaults.IconSpacing))
                    }
                }
            }
        }
    }
}
