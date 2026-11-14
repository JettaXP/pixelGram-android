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
    val dispatcher = androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    androidx.compose.runtime.LaunchedEffect(dispatcher) {
        dispatcher?.transitionState?.collect { state ->
            PredictiveEdge.dragging = state is androidx.navigationevent.NavigationEventTransitionState.InProgress
        }
    }
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(backdrop)) {
    NavDisplay(
        backStack = stack,
        onBack = { navigator.back() },
        predictivePopTransitionSpec = { edge ->
            PredictiveEdge.fromLeft = edge != androidx.navigationevent.NavigationEvent.EDGE_RIGHT
            fadeIn(tween(PredictiveMs, easing = androidx.compose.animation.core.LinearEasing), initialAlpha = 0.999f) togetherWith
                fadeOut(tween(PredictiveMs, easing = androidx.compose.animation.core.LinearEasing), targetAlpha = 0.999f)
        },
        entryDecorators = listOf(
            androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator(),
            remember { PredictiveDecorator() },
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

object PredictiveEdge {
    var fromLeft = true
    var dragging by androidx.compose.runtime.mutableStateOf(false)
    var commit by androidx.compose.runtime.mutableFloatStateOf(0f)
}

private class PredictiveRun {
    var root: Any? = null
    var start: Any? = null
    var closing = false
    var seekEnd = -1f
}

private val DragEasing = androidx.compose.animation.core.CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private val CommitEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

private class PredictiveDecorator : androidx.navigation3.runtime.NavEntryDecorator<NavKey>(
    decorate = { entry ->
        val child = androidx.navigation3.ui.LocalNavAnimatedContentScope.current.transition
        val root = child.parentTransition ?: child
        val run = remember { PredictiveRun() }
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier.graphicsLayer {
                val total = child.totalDurationNanos
                val f = if (total > 0) (child.playTimeNanos.toFloat() / total).coerceIn(0f, 1f) else 0f
                val moving = child.currentState != child.targetState
                val drag = PredictiveEdge.dragging
                if (drag && moving) {
                    if (run.root == null) {
                        run.root = root
                        run.start = root.currentState
                        run.closing = child.targetState == androidx.compose.animation.EnterExitState.PostExit
                    }
                    run.seekEnd = f
                } else if (!moving && !drag) {
                    if (run.closing && run.root != null) PredictiveEdge.commit = 0f
                    run.root = null
                    run.seekEnd = -1f
                }
                if (run.root == null) return@graphicsLayer
                val w = size.width
                val cancelled = !drag && root.targetState == run.start
                val dragging = drag || cancelled
                val p = DragEasing.transform(if (dragging) f else run.seekEnd.coerceAtLeast(0f))
                val c = if (dragging) 0f else {
                    val window = minOf(1f - run.seekEnd, CommitShare).coerceAtLeast(0.01f)
                    CommitEasing.transform(((f - run.seekEnd) / window).coerceIn(0f, 1f))
                }
                val corner = 28.dp.toPx()
                if (run.closing) {
                    if (PredictiveEdge.commit != c) PredictiveEdge.commit = c
                    val scale = mix(1f, 0.9f, p) * mix(1f, 0.94f, c)
                    scaleX = scale
                    scaleY = scale
                    translationX = (if (PredictiveEdge.fromLeft) 1f else -1f) * (w * 0.04f * p + w * 0.06f * c)
                    alpha = (1f - c * 1.6f).coerceIn(0f, 1f)
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(corner * (p * 4f).coerceAtMost(1f))
                    clip = true
                } else {
                    val shared = if (dragging) 0f else PredictiveEdge.commit
                    alpha = shared
                    val scale = mix(0.92f, 1f, shared)
                    scaleX = scale
                    scaleY = scale
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(corner * (1f - shared))
                    clip = shared < 1f
                }
            },
        ) { entry.Content() }
    },
)

private const val PredictiveMs = 1000
private const val CommitShare = 0.4f
