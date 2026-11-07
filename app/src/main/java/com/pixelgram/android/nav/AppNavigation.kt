package com.pixelgram.android.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
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
@Serializable data class ChatKey(val chatId: Long) : NavKey
@Serializable data class ProfileKey(val userId: Long, val chatId: Long = 0) : NavKey
@Serializable data class PixelSettingsKey(val page: String = "") : NavKey
@Serializable data object SessionsKey : NavKey
@Serializable data class ProfilePhotosKey(val userId: Long) : NavKey
@Serializable data class SettingsSectionKey(val section: String) : NavKey

class Navigator(private val stack: MutableList<NavKey>) {
    fun open(key: NavKey) {
        stack.add(key)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

@Composable
fun AppNavigation(graph: AppGraph) {
    val stack = rememberNavBackStack(MainKey)
    val navigator = Navigator(stack)
    NavDisplay(
        backStack = stack,
        onBack = { navigator.back() },
        transitionSpec = {
            slideInHorizontally(tween(320)) { it } togetherWith fadeOut(tween(200))
        },
        popTransitionSpec = {
            fadeIn(tween(200)) togetherWith slideOutHorizontally(tween(320)) { it }
        },
        predictivePopTransitionSpec = {
            fadeIn(tween(200)) togetherWith (scaleOut(targetScale = 0.9f) + slideOutHorizontally { it / 2 } + fadeOut())
        },
        entryProvider = entryProvider {
            entry<MainKey> { MainShell(graph, navigator) }
            entry<ChatKey> { key -> ChatScreen(graph, key.chatId, navigator) }
            entry<ProfileKey> { key -> ProfileScreen(graph, key.userId, key.chatId, navigator) }
            entry<PixelSettingsKey> { key -> PixelSettingsScreen(graph, key.page, navigator) }
            entry<SessionsKey> { SessionsScreen(graph, navigator) }
            entry<ProfilePhotosKey> { key -> com.pixelgram.android.feature.profile.ProfilePhotosScreen(graph, key.userId, navigator) }
            entry<SettingsSectionKey> { key -> com.pixelgram.android.feature.settings.SettingsSectionScreen(graph, key.section, navigator) }
        },
    )
}
