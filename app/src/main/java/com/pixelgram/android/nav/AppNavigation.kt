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
    val backScope = androidx.compose.runtime.rememberCoroutineScope()
    val dispatcher = androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val currentStack = androidx.compose.runtime.rememberUpdatedState(stack.toList())
    androidx.compose.runtime.LaunchedEffect(dispatcher) {
        dispatcher?.transitionState?.collect { state ->
            if (state is androidx.navigationevent.NavigationEventTransitionState.InProgress) {
                EtgBack.progress(state.latestEvent, currentStack.value)
            } else {
                EtgBack.released(backScope)
            }
        }
    }
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest)) {
    NavDisplay(
        backStack = stack,
        onBack = {
            if (EtgBack.phase == EtgBack.Drag || EtgBack.phase == EtgBack.Cancel) EtgBack.commit(backScope)
            navigator.back()
        },
        transitionSpec = {
            if (EtgBack.phase != EtgBack.Idle) androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
            else androidx.navigation3.ui.defaultTransitionSpec<NavKey>()(this)
        },
        popTransitionSpec = {
            if (EtgBack.phase != EtgBack.Idle) androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
            else androidx.navigation3.ui.defaultPopTransitionSpec<NavKey>()(this)
        },
        predictivePopTransitionSpec = {
            fadeIn(tween(HoldMs), initialAlpha = 0.999f) togetherWith fadeOut(tween(HoldMs), targetAlpha = 0.999f)
        },
        entryDecorators = listOf(
            androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator(),
            remember { EtgBackDecorator() },
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

private const val HoldMs = 1_500

object EtgBack {
    const val Idle = 0
    const val Drag = 1
    const val Commit = 2
    const val Cancel = 3

    private val gesture = android.view.animation.PathInterpolator(0.1f, 0.1f, 0f, 1f)
    private val vertical = android.view.animation.DecelerateInterpolator()
    private val emphasized = android.view.animation.PathInterpolator(
        android.graphics.Path().apply {
            moveTo(0f, 0f)
            cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
            cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        },
    )
    private val easeOutQuint = android.view.animation.PathInterpolator(0.23f, 1f, 0.32f, 1f)

    var phase by androidx.compose.runtime.mutableIntStateOf(Idle)
    var progress by androidx.compose.runtime.mutableFloatStateOf(0f)
    var commitRaw by androidx.compose.runtime.mutableFloatStateOf(0f)
    var scrim by androidx.compose.runtime.mutableFloatStateOf(1f)
    var touchY by androidx.compose.runtime.mutableFloatStateOf(0f)
    var startY = 0f
    var fromLeft = true
    var closingKey: String? = null
    var enteringKey: String? = null
    private var job: kotlinx.coroutines.Job? = null

    fun matches(key: String, target: String?) = target != null && (key == target || key.startsWith("$target:"))

    fun progress(event: androidx.navigationevent.NavigationEvent, stack: List<NavKey>) {
        if (phase == Commit) return
        if (phase != Drag) {
            if (stack.size < 2) return
            job?.cancel()
            hidden = null
            closingKey = stack[stack.lastIndex].toString()
            enteringKey = stack[stack.lastIndex - 1].toString()
            fromLeft = event.swipeEdge != androidx.navigationevent.NavigationEvent.EDGE_RIGHT
            startY = event.touchY
            scrim = 1f
            commitRaw = 0f
            phase = Drag
        }
        touchY = event.touchY
        progress = gesture.getInterpolation(event.progress.coerceIn(0f, 1f))
    }

    fun released(scope: kotlinx.coroutines.CoroutineScope) {
        if (phase != Drag) return
        job = scope.launch {
            androidx.compose.runtime.withFrameNanos { }
            if (phase != Drag) return@launch
            phase = Cancel
            val from = progress
            val duration = maxOf(120, (from * 220).toInt())
            val start = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                val f = (((now - start) / 1_000_000f) / duration).coerceIn(0f, 1f)
                val value = from * (1f - easeOutQuint.getInterpolation(f))
                progress = value
                scrim = if (from != 0f) value / from else 0f
                if (f >= 1f) break
            }
            reset()
        }
    }

    var hidden by androidx.compose.runtime.mutableStateOf<String?>(null)

    fun commit(scope: kotlinx.coroutines.CoroutineScope) {
        phase = Commit
        job?.cancel()
        job = scope.launch {
            val start = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                val f = (((now - start) / 1_000_000f) / CommitMs).coerceIn(0f, 1f)
                commitRaw = f
                scrim = 1f - f
                if (f >= 1f) break
            }
            hidden = closingKey
            reset()
        }
    }

    private fun reset() {
        phase = Idle
        progress = 0f
        commitRaw = 0f
        scrim = 1f
        closingKey = null
        enteringKey = null
    }

    fun commitCurve(raw: Float) = emphasized.getInterpolation(raw)
    fun verticalCurve(x: Float) = vertical.getInterpolation(x)

    private const val CommitMs = 375
}

private class EtgBackDecorator : androidx.navigation3.runtime.NavEntryDecorator<NavKey>(
    decorate = { entry ->
        val key = entry.contentKey.toString()
        val dark = com.pixelgram.android.ui.theme.LocalDarkTheme.current
        androidx.compose.runtime.DisposableEffect(key) {
            onDispose { if (EtgBack.matches(key, EtgBack.hidden)) EtgBack.hidden = null }
        }
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier
                .graphicsLayer {
                    if (EtgBack.matches(key, EtgBack.hidden)) {
                        alpha = 0f
                        return@graphicsLayer
                    }
                    val phase = EtgBack.phase
                    if (phase == EtgBack.Idle) return@graphicsLayer
                    val closing = EtgBack.matches(key, EtgBack.closingKey)
                    val entering = EtgBack.matches(key, EtgBack.enteringKey)
                    if (!closing && !entering) return@graphicsLayer
                    val w = size.width
                    val h = size.height
                    val p = EtgBack.progress
                    val margin = 8.dp.toPx()
                    val shift = 96.dp.toPx()
                    val dy = EtgBack.touchY - EtgBack.startY
                    fun yOffset(height: Float): Float {
                        val half = h / 2f
                        return (if (dy < 0) -1f else 1f) * EtgBack.verticalCurve(minOf(half, kotlin.math.abs(dy)) / half) * maxOf(0f, (h - height) / 2f - margin)
                    }
                    val closeScale = 1f + (0.85f - 1f) * p
                    val targetLeft = if (EtgBack.fromLeft) w - margin - w * 0.85f else (w - w * 0.85f) / 2f
                    val closeLeft = targetLeft * p
                    val closeTop = (h - h * 0.85f) / 2f * p + yOffset(h * closeScale)
                    val corner = 40.dp.toPx() * p
                    val enterBase = 0.95f
                    val enterOffset = maxOf(w * enterBase * 0.15f, shift)
                    val enterScale = enterBase * (1f + (0.85f - 1f) * p)
                    val enterCx = w / 2f - enterOffset
                    val enterCy = h / 2f + yOffset(h * closeScale)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    if (phase == EtgBack.Commit) {
                        val raw = EtgBack.commitRaw
                        val c = EtgBack.commitCurve(raw)
                        if (closing) {
                            val scale = closeScale + (1f - closeScale) * c
                            scaleX = scale
                            scaleY = scale
                            translationX = closeLeft + shift * c
                            translationY = closeTop * (1f - c)
                            alpha = maxOf(1f - 5f * raw, 0f)
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(corner * (1f - c))
                            clip = true
                        } else {
                            val scale = enterScale + (1f - enterScale) * c
                            scaleX = scale
                            scaleY = scale
                            translationX = (enterCx - w * enterScale / 2f) * (1f - c)
                            translationY = (enterCy - h * enterScale / 2f) * (1f - c)
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(corner * (1f - c))
                            clip = c < 1f
                        }
                    } else if (closing) {
                        scaleX = closeScale
                        scaleY = closeScale
                        translationX = closeLeft
                        translationY = closeTop
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
                        clip = true
                    } else {
                        scaleX = enterScale
                        scaleY = enterScale
                        translationX = enterCx - w * enterScale / 2f
                        translationY = enterCy - h * enterScale / 2f
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
                        clip = true
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (EtgBack.phase != EtgBack.Idle && EtgBack.matches(key, EtgBack.enteringKey)) {
                        val a = (if (dark) 0.8f else 0.2f) * EtgBack.scrim
                        if (a > 0f) drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = a))
                    }
                },
        ) { entry.Content() }
    },
)
