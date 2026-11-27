package com.pixelgram.android.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

val WheelItem = 48.dp

@Composable
fun WheelPicker(items: List<String>, initial: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState(initial)
    val fling = rememberSnapFlingBehavior(state)
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > 60) 1 else 0 }
            .distinctUntilChanged()
            .collect { onSelect(it.coerceIn(0, items.lastIndex)) }
    }
    val itemPx = with(LocalDensity.current) { WheelItem.toPx() }
    val center by remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / itemPx } }
    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = WheelItem * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(WheelItem * 5),
    ) {
        items(items.size) { index ->
            val delta = index - center
            val distance = kotlin.math.abs(delta).toInt()
            val exit = ((kotlin.math.abs(delta) - 0.5f) / 1.5f).coerceIn(0f, 1f)
            Box(Modifier.height(WheelItem).fillMaxWidth(), contentAlignment = Alignment.Center) {
                var fit by remember(index) { mutableStateOf(1f) }
                Text(
                    items[index],
                    fontSize = ((if (distance == 0) 22f else 19f) * fit).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = when (distance) { 0 -> 1f; 1 -> 0.6f; else -> 0.3f }),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = 1f - exit * exit }
                        .blur((exit * 10).dp, BlurredEdgeTreatment.Unbounded),
                    onTextLayout = { if (it.hasVisualOverflow && fit > 0.6f) fit *= 0.9f },
                )
            }
        }
    }
}
