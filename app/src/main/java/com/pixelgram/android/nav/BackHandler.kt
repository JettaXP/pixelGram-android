package com.pixelgram.android.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val latest = rememberUpdatedState(onBack)
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = enabled,
        onBackCompleted = { latest.value() },
    )
}
