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
    val gesture = rememberBackGesture(stack)
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest)) {
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
            androidx.compose.animation.EnterTransition.None togetherWith fadeOut(tween(CommitMs + 200), targetAlpha = 0.99f)
        },
        entryDecorators = listOf(
            androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator(),
            remember(gesture) { BackFollowDecorator(gesture) },
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

object BackBlockers {
    var count by androidx.compose.runtime.mutableIntStateOf(0)
}

@Composable
fun BackBlock(enabled: Boolean) {
    androidx.compose.runtime.DisposableEffect(enabled) {
        if (enabled) BackBlockers.count++
        onDispose { if (enabled) BackBlockers.count-- }
    }
}

class BackGesture {
    var progress by androidx.compose.runtime.mutableFloatStateOf(0f)
    var commit by androidx.compose.runtime.mutableFloatStateOf(0f)
    var startY by androidx.compose.runtime.mutableFloatStateOf(Float.NaN)
    var touchY by androidx.compose.runtime.mutableFloatStateOf(Float.NaN)
    var fromLeft by androidx.compose.runtime.mutableStateOf(true)
    var closingKey by androidx.compose.runtime.mutableStateOf<String?>(null)
    var enteringKey by androidx.compose.runtime.mutableStateOf<String?>(null)
    var dragging = false
    var job: kotlinx.coroutines.Job? = null

    fun reset() {
        progress = 0f
        commit = 0f
        startY = Float.NaN
        touchY = Float.NaN
        closingKey = null
        enteringKey = null
        dragging = false
    }
}

private val GestureEasing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val CommitEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
private fun rememberBackGesture(stack: List<NavKey>): BackGesture {
    val gesture = remember { BackGesture() }
    val dispatcher = androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val currentStack = androidx.compose.runtime.rememberUpdatedState(stack)
    androidx.compose.runtime.LaunchedEffect(dispatcher) {
        val scope = this
        dispatcher?.transitionState?.collect { state ->
            if (state is androidx.navigationevent.NavigationEventTransitionState.InProgress) {
                val event = state.latestEvent
                if (!gesture.dragging) {
                    val keys = currentStack.value
                    if (keys.size < 2 || BackBlockers.count > 0 || com.pixelgram.android.feature.player.FullPlayer.open) return@collect
                    gesture.job?.cancel()
                    gesture.reset()
                    gesture.dragging = true
                    gesture.closingKey = keys[keys.lastIndex].toString()
                    gesture.enteringKey = keys[keys.lastIndex - 1].toString()
                    gesture.fromLeft = event.swipeEdge != androidx.navigationevent.NavigationEvent.EDGE_RIGHT
                    gesture.startY = event.touchY
                }
                gesture.touchY = event.touchY
                val raw = ((event.progress - 0.015f) / 0.985f).coerceIn(0f, 1f)
                gesture.progress = GestureEasing.transform(raw)
            } else if (gesture.dragging) {
                gesture.dragging = false
                gesture.job = scope.launch {
                    repeat(2) { androidx.compose.runtime.withFrameNanos { } }
                    val committed = currentStack.value.lastOrNull()?.toString() != gesture.closingKey
                    if (committed) {
                        androidx.compose.animation.core.animate(0f, 1f, animationSpec = tween(CommitMs, easing = CommitEasing)) { v, _ -> gesture.commit = v }
                    } else {
                        androidx.compose.animation.core.animate(gesture.progress, 0f, animationSpec = tween(250, easing = CommitEasing)) { v, _ -> gesture.progress = v }
                    }
                    gesture.reset()
                }
            }
        }
    }
    return gesture
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private class BackFollowDecorator(private val gesture: BackGesture) : androidx.navigation3.runtime.NavEntryDecorator<NavKey>(
    decorate = { entry ->
        val key = entry.contentKey.toString()
        val scrim = androidx.compose.ui.graphics.Color.Black
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier
                .graphicsLayer {
                    val closing = gesture.closingKey?.let { key == it || key.startsWith("$it:") } == true
                    val entering = gesture.enteringKey?.let { key == it || key.startsWith("$it:") } == true
                    if (!closing && !entering) return@graphicsLayer
                    val p = gesture.progress
                    val c = gesture.commit
                    val w = size.width
                    val edge = 8.dp.toPx()
                    val corner = lerp(0f, 40.dp.toPx(), p) * (1f - c)
                    val shift = if (gesture.startY.isNaN()) 0f else (gesture.touchY - gesture.startY) * FollowShare
                    val limit = size.height * FollowLimit
                    val follow = shift.coerceIn(-limit, limit) * (p * 4f).coerceAtMost(1f)
                    if (closing) {
                        val s = lerp(1f, ClosingScale, p)
                        val scale = lerp(s, 1f, c)
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (gesture.fromLeft) 1f else 0f, 0.5f)
                        val base = (if (gesture.fromLeft) -edge else edge) * p
                        translationX = base + (if (gesture.fromLeft) 1f else -1f) * (w * (1f - s) + 96.dp.toPx()) * c
                        translationY = follow * (1f - c)
                        alpha = (1f - 5f * c).coerceAtLeast(0f)
                        shadowElevation = 12.dp.toPx() * p * (1f - c)
                    } else {
                        val s = lerp(EnteringScale, EnteringScale * ClosingScale, p)
                        val scale = lerp(s, 1f, c)
                        scaleX = scale
                        scaleY = scale
                        val offset = -kotlin.math.max(w * 0.15f, 96.dp.toPx()) * (if (gesture.fromLeft) 1f else -1f)
                        translationX = lerp(offset, 0f, c)
                        translationY = follow * (1f - c)
                    }
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
                    clip = true
                }
                .drawWithContent {
                    drawContent()
                    if (gesture.closingKey != null && gesture.enteringKey?.let { key == it || key.startsWith("$it:") } == true) {
                        val a = ScrimAlpha * (1f - gesture.commit) * (gesture.progress * 4f).coerceAtMost(1f).coerceAtLeast(if (gesture.commit > 0f) 1f else 0f)
                        if (a > 0f) drawRect(scrim.copy(alpha = a))
                    }
                },
        ) { entry.Content() }
    },
)

private const val ClosingScale = 0.85f
private const val EnteringScale = 0.95f
private const val ScrimAlpha = 0.35f
private const val CommitMs = 320
private const val FollowShare = 0.35f
private const val FollowLimit = 0.08f
