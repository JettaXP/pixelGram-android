package com.pixelgram.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val LocalBottomBarPadding = androidx.compose.runtime.compositionLocalOf { 0.dp }

data class CapsuleItem<K>(val key: K, val selectedIcon: ImageVector, val icon: ImageVector, val label: String, val badge: Int = 0)

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
    val colors = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val bounds = remember { mutableStateMapOf<K, Pair<Float, Float>>() }
    var previous by remember { mutableStateOf(selected) }
    val travel = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (previous != selected) {
            travel.snapTo(0f)
            travel.animateTo(1f, spatial)
        }
        previous = selected
    }
    Box(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = colors.surfaceContainerHighest, shadowElevation = 3.dp) {
            Box(Modifier.padding(inset)) {
                Box(
                    Modifier
                        .layout { measurable, _ ->
                            val from = bounds[previous]
                            val to = bounds[selected] ?: from
                            val t = travel.value
                            val left = androidx.compose.ui.util.lerp(from?.first ?: to?.first ?: 0f, to?.first ?: 0f, t)
                            val width = androidx.compose.ui.util.lerp(from?.second ?: to?.second ?: 0f, to?.second ?: 0f, t)
                            val placeable = measurable.measure(Constraints.fixed(width.roundToInt().coerceAtLeast(0), itemHeight.roundToPx()))
                            layout(0, 0) { placeable.place(left.roundToInt(), 0) }
                        }
                        .clip(CircleShape)
                        .background(colors.primary),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
                    items.forEach { item ->
                        val active = item.key == selected
                        val progress by animateFloatAsState(if (active) 1f else 0f, effects, label = "capsule-tint")
                        val content = lerp(colors.onSurfaceVariant, colors.onPrimary, progress)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .onPlaced {
                                    val value = it.positionInParent().x to it.size.width.toFloat()
                                    if (bounds[item.key] != value) bounds[item.key] = value
                                }
                                .height(itemHeight)
                                .clip(CircleShape)
                                .selectable(selected = active, role = Role.Tab) { onSelect(item.key) }
                                .padding(horizontal = 18.dp),
                        ) {
                            BadgedBox(badge = {
                                if (item.badge > 0) Badge { Text(if (item.badge > 999) "999+" else item.badge.toString()) }
                            }) {
                                Icon(if (active) item.selectedIcon else item.icon, contentDescription = item.label, tint = content)
                            }
                            AnimatedVisibility(
                                visible = active,
                                enter = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                                exit = shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                            ) {
                                Text(
                                    item.label,
                                    color = content,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

