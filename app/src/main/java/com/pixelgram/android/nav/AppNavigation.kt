package com.pixelgram.android.nav

import androidx.compose.animation.core.animateDp
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
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
@Serializable data object BusinessKey : NavKey
@Serializable data object SettingsKey : NavKey

object PendingNav {
    var chat by androidx.compose.runtime.mutableStateOf<Long?>(null)
    var link by androidx.compose.runtime.mutableStateOf<String?>(null)
}

object PixelLinks {
    val Pattern = Regex("pg://(settings|premium|stars|business|pixel)(/[A-Za-z_]+)?", RegexOption.IGNORE_CASE)

    fun key(link: String): NavKey? {
        val match = Pattern.matchEntire(link.trim()) ?: return null
        val sub = match.groupValues[2].removePrefix("/")
        return when (match.groupValues[1].lowercase()) {
            "settings" -> if (sub.isEmpty()) SettingsKey else com.pixelgram.android.feature.settings.SettingsSection.entries.firstOrNull { it.name.equals(sub, true) }?.let { SettingsSectionKey(it.name) } ?: SettingsKey
            "premium" -> PremiumKey
            "stars" -> StarsKey
            "business" -> BusinessKey
            "pixel" -> PixelSettingsKey(sub.lowercase())
            else -> null
        }
    }
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
    val backdrop = androidx.compose.ui.graphics.lerp(androidx.compose.material3.MaterialTheme.colorScheme.surface, androidx.compose.ui.graphics.Color.Black, 0.7f)
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(backdrop)) {
    NavDisplay(
        backStack = stack,
        onBack = { navigator.back() },
        predictivePopTransitionSpec = { edge ->
            val toward = if (edge == androidx.navigationevent.NavigationEvent.EDGE_RIGHT) -1 else 1
            val enter = fadeIn(androidx.compose.animation.core.keyframes { durationMillis = PredictiveMs; 0f at PredictiveMs * 6 / 10; 1f at PredictiveMs using androidx.compose.animation.core.LinearOutSlowInEasing }) +
                androidx.compose.animation.scaleIn(androidx.compose.animation.core.keyframes { durationMillis = PredictiveMs; 0.92f at PredictiveMs * 6 / 10; 1f at PredictiveMs using androidx.compose.animation.core.LinearOutSlowInEasing }, initialScale = 0.92f)
            val exit = androidx.compose.animation.scaleOut(androidx.compose.animation.core.keyframes { durationMillis = PredictiveMs; 0.9f at PredictiveMs / 2 using androidx.compose.animation.core.LinearOutSlowInEasing; 0.9f at PredictiveMs }, targetScale = 0.9f) +
                slideOutHorizontally(tween(PredictiveMs, easing = androidx.compose.animation.core.LinearEasing)) { (it * 0.08f * toward).toInt() } +
                fadeOut(androidx.compose.animation.core.keyframes { durationMillis = PredictiveMs; 1f at PredictiveMs / 2; 0f at PredictiveMs * 65 / 100 using androidx.compose.animation.core.FastOutLinearInEasing; 0f at PredictiveMs })
            enter togetherWith exit
        },
        entryDecorators = listOf(
            androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator(),
            remember { RoundedTransitionDecorator() },
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
            entry<SettingsKey> { com.pixelgram.android.feature.settings.SettingsTab(graph, navigator, onOpenDrawer = null, onBack = { navigator.back() }) }
            entry<BusinessKey> { com.pixelgram.android.feature.premium.PremiumScreen(graph, navigator, business = true) }
            entry<StarsKey> { com.pixelgram.android.feature.premium.StarsScreen(graph, navigator) }
        },
    )
    com.pixelgram.android.feature.player.FullPlayerOverlay(graph)
    com.pixelgram.android.feature.stories.StoryOverlays(graph)
    com.pixelgram.android.feature.pixel.SupportSheet()
    com.pixelgram.android.feature.premium.PaymentHost()
    com.pixelgram.android.feature.premium.EmojiStatusHost { navigator.open(PremiumKey) }
    androidx.compose.runtime.LaunchedEffect(PendingNav.link) {
        PendingNav.link?.let { link ->
            PendingNav.link = null
            PixelLinks.key(link)?.let { navigator.open(it) }
        }
    }
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

private class RoundedTransitionDecorator : androidx.navigation3.runtime.NavEntryDecorator<NavKey>(
    decorate = { entry ->
        val transition = androidx.navigation3.ui.LocalNavAnimatedContentScope.current.transition
        val corner by transition.animateDp(transitionSpec = { tween(PredictiveMs / 3) }, label = "entry-corner") { state ->
            if (state == androidx.compose.animation.EnterExitState.Visible) 0.dp else 28.dp
        }
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier.graphicsLayer {
                if (corner > 0.dp) {
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
                    clip = true
                }
            },
        ) { entry.Content() }
    },
)

private const val PredictiveMs = 500
