package com.pixelgram.android.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.pixelgram.android.AppGraph
import com.pixelgram.android.feature.chat.ChatScreen
import com.pixelgram.android.feature.main.MainShell
import com.pixelgram.android.feature.pixel.PixelSettingsScreen
import com.pixelgram.android.feature.profile.ProfileScreen
import com.pixelgram.android.feature.settings.SessionsScreen
import kotlinx.serialization.Serializable

@Serializable data object MainKey : NavKey
@Serializable data class ChatKey(val chatId: Long, val topicId: Int = 0) : NavKey
@Serializable data class ForumKey(val chatId: Long) : NavKey
@Serializable data class ProfileKey(val userId: Long, val chatId: Long = 0) : NavKey
@Serializable data class PixelSettingsKey(val page: String = "") : NavKey
@Serializable data object SessionsKey : NavKey
@Serializable data object SearchKey : NavKey
@Serializable data class ProfilePhotosKey(val userId: Long) : NavKey
@Serializable data class SettingsSectionKey(val section: String) : NavKey
@Serializable data object PremiumKey : NavKey
@Serializable data object StarsKey : NavKey

object PendingNav {
    var chat by androidx.compose.runtime.mutableStateOf<Long?>(null)
}

class Navigator(private val stack: MutableList<NavKey>) {
    fun open(key: NavKey) {
        stack.add(key)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

@Composable
fun AppNavigation(graph: AppGraph, start: List<NavKey> = listOf(MainKey)) {
    val stack = rememberNavBackStack(*start.toTypedArray())
    val navigator = Navigator(stack)
    val gesture = rememberBackGesture()
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
    NavDisplay(
        backStack = stack,
        onBack = { navigator.back() },
        transitionSpec = {
            slideInHorizontally(tween(320)) { it } togetherWith fadeOut(tween(200))
        },
        popTransitionSpec = {
            fadeIn(tween(200)) togetherWith slideOutHorizontally(tween(320)) { it }
        },
        predictivePopTransitionSpec = { edge ->
            val sign = if (edge == androidx.navigationevent.NavigationEvent.EDGE_RIGHT) -1 else 1
            fadeIn(tween(200)) togetherWith (scaleOut(targetScale = BackScale) + slideOutHorizontally { it / 5 * sign } + fadeOut(tween(220, delayMillis = 80)))
        },
        entryDecorators = listOf(
            androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator(),
            remember(gesture) { BackFollowDecorator(gesture) { stack.lastOrNull() } },
        ),
        entryProvider = entryProvider {
            entry<MainKey> { MainShell(graph, navigator) }
            entry<ChatKey> { key -> ChatScreen(graph, key.chatId, navigator, key.topicId) }
            entry<ForumKey> { key -> com.pixelgram.android.feature.chat.ForumScreen(graph, key.chatId, navigator) }
            entry<ProfileKey> { key -> ProfileScreen(graph, key.userId, key.chatId, navigator) }
            entry<PixelSettingsKey> { key -> PixelSettingsScreen(graph, key.page, navigator) }
            entry<SessionsKey> { SessionsScreen(graph, navigator) }
            entry<SearchKey> { com.pixelgram.android.feature.search.SearchScreen(graph, navigator) }
            entry<ProfilePhotosKey> { key -> com.pixelgram.android.feature.profile.ProfilePhotosScreen(graph, key.userId, navigator) }
            entry<SettingsSectionKey> { key -> com.pixelgram.android.feature.settings.SettingsSectionScreen(graph, key.section, navigator) }
            entry<PremiumKey> { com.pixelgram.android.feature.premium.PremiumScreen(graph, navigator) }
            entry<StarsKey> { com.pixelgram.android.feature.premium.StarsScreen(graph, navigator) }
        },
    )
    com.pixelgram.android.feature.player.FullPlayerOverlay(graph)
    com.pixelgram.android.feature.stories.StoryOverlays(graph)
    com.pixelgram.android.feature.pixel.SupportSheet()
    com.pixelgram.android.feature.premium.PaymentHost()
    com.pixelgram.android.feature.premium.EmojiStatusHost { navigator.open(PremiumKey) }
    androidx.compose.runtime.LaunchedEffect(PendingNav.chat) {
        PendingNav.chat?.let { id ->
            PendingNav.chat = null
            navigator.open(ChatKey(id))
        }
    }
    com.pixelgram.android.feature.pixel.CreditsOverlay()
    com.pixelgram.android.ui.components.NoticeHost(
        androidx.compose.ui.Modifier
            .align(androidx.compose.ui.Alignment.BottomCenter)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
            .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
            .zIndex(30f),
    )
    }
}

class BackGesture {
    var progress by androidx.compose.runtime.mutableFloatStateOf(0f)
    var startY by androidx.compose.runtime.mutableFloatStateOf(Float.NaN)
    var touchY by androidx.compose.runtime.mutableFloatStateOf(Float.NaN)
    val active: Boolean get() = progress > 0f && !startY.isNaN()
}

@Composable
private fun rememberBackGesture(): BackGesture {
    val gesture = remember { BackGesture() }
    val dispatcher = androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    androidx.compose.runtime.LaunchedEffect(dispatcher) {
        dispatcher?.transitionState?.collect { state ->
            if (state is androidx.navigationevent.NavigationEventTransitionState.InProgress) {
                val event = state.latestEvent
                if (gesture.startY.isNaN()) gesture.startY = event.touchY
                gesture.touchY = event.touchY
                gesture.progress = event.progress
            } else {
                gesture.progress = 0f
                gesture.startY = Float.NaN
                gesture.touchY = Float.NaN
            }
        }
    }
    return gesture
}

private class BackFollowDecorator(private val gesture: BackGesture, private val top: () -> NavKey?) : androidx.navigation3.runtime.NavEntryDecorator<NavKey>(
    decorate = { entry ->
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier.graphicsLayer {
                if (entry.contentKey == top()?.toString() && gesture.active) {
                    val shift = (gesture.touchY - gesture.startY) * FollowShare
                    val limit = size.height * FollowLimit
                    translationY = shift.coerceIn(-limit, limit) * (gesture.progress * 4f).coerceAtMost(1f)
                    shape = androidx.compose.foundation.shape.RoundedCornerShape((CornerMax * (gesture.progress * 3f).coerceAtMost(1f)).dp)
                    clip = true
                }
            },
        ) { entry.Content() }
    },
)

private const val BackScale = 0.88f
private const val FollowShare = 0.35f
private const val FollowLimit = 0.08f
private const val CornerMax = 28f
