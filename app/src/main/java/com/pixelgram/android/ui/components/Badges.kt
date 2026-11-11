@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pixelgram.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import coil.compose.AsyncImage
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import androidx.compose.ui.res.painterResource
import com.pixelgram.android.pixel.CustomBadge
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

enum class UserBadge(val userId: Long, val notice: Int) {
    MainDeveloper(1780288121L, R.string.badge_main_developer),
    Developer(1061520526L, R.string.badge_developer),
    SecondDeveloper(1170147841L, R.string.badge_developer),
    Sponsor(1602207467L, R.string.badge_sponsor),
    Tester(7820526891L, R.string.badge_tester),
    SecondTester(1635698970L, R.string.badge_tester),
    Menorah(8315838128L, R.string.badge_friend),
    Poop(7826361017L, R.string.badge_poop);

    val icon: ImageVector
        get() = when (this) {
            MainDeveloper -> CodeSlash
            Developer, SecondDeveloper -> Icons.Rounded.Build
            Sponsor -> Icons.Rounded.AttachMoney
            Tester, SecondTester -> BugNet
            Menorah -> MenorahIcon
            Poop -> PoopIcon
        }

    companion object {
        fun of(userId: Long): UserBadge? = entries.firstOrNull { it.userId == userId }
    }
}

@Composable
fun NameBadges(userId: Long, verified: Boolean, size: Dp = 18.dp, scam: Boolean = false, fake: Boolean = false, name: String = "", label: Boolean = false) {
    if (verified) VerifiedBadge(size)
    if (scam || fake) {
        Box(
            Modifier.padding(start = 4.dp).border(1.dp, MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
        ) {
            Text(
                stringResource(if (scam) R.string.badge_scam else R.string.badge_fake),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            )
        }
    }
    val official = UserBadge.of(userId)
    if (official != null) UserBadgeView(official, size) else if (userId > 0) SupporterBadge(userId, name, size, label)
}

@Composable
fun SupporterBadge(userId: Long, name: String, size: Dp = 18.dp, label: Boolean = false) {
    val graph = LocalGraph.current
    val scheme = MaterialTheme.colorScheme
    val supporters by remember { graph.remote.supporters }.collectAsState()
    val badges by remember { graph.remote.badges }.collectAsState()
    val number = supporters.numbers[userId]
    val custom = badges[userId]?.takeIf { userId in supporters.custom }
    if (number == null && custom == null) return
    val notice = custom?.text?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.badge_supporter, name.ifBlank { stringResource(R.string.badge_supporter_someone) }, number?.toString().orEmpty())
    var showLabel by remember(userId) { androidx.compose.runtime.mutableStateOf(label && number != null) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 4.dp)
            .size(size)
            .then(if (custom == null) Modifier.clip(MaterialShapes.Cookie12Sided.toShape()).background(scheme.tertiary) else Modifier)
            .clickable(interactionSource = null, indication = null) { Notice.show(notice) },
    ) {
        if (custom != null) {
            CustomEmoji(custom, size)
        } else {
            Icon(painterResource(R.drawable.ic_pixelgram), contentDescription = notice, tint = scheme.onTertiary, modifier = Modifier.size(size * LogoScale))
        }
        if (showLabel && number != null) BadgeLabel(stringResource(R.string.badge_supporter_label, number)) { showLabel = false }
    }
}

@Composable
private fun BadgeLabel(text: String, onDone: () -> Unit) {
    val shown = remember(text) { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    androidx.compose.runtime.LaunchedEffect(text) {
        kotlinx.coroutines.delay(LabelMs)
        shown.targetState = false
    }
    androidx.compose.runtime.LaunchedEffect(shown.currentState, shown.isIdle) {
        if (shown.isIdle && !shown.currentState) onDone()
    }
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 6.dp.roundToPx() }
    androidx.compose.ui.window.Popup(
        popupPositionProvider = remember {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize,
                ) = androidx.compose.ui.unit.IntOffset(
                    (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                    (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0),
                )
            }
        },
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visibleState = shown,
            enter = androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) +
                androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)),
            exit = androidx.compose.animation.fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)),
        ) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 4.dp,
            ) {
                Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

private const val LabelMs = 5_000L

@Composable
fun VerifiedBadge(size: Dp = 18.dp) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(start = 4.dp).size(size).clip(MaterialShapes.Cookie12Sided.toShape()).background(scheme.primary),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(size * LogoScale))
    }
}

@Composable
fun UserBadgeView(badge: UserBadge, size: Dp = 18.dp) {
    val graph = LocalGraph.current
    val scheme = MaterialTheme.colorScheme
    val custom by remember { graph.remote.badges }.collectAsState()
    val own = custom[badge.userId]
    val notice = stringResource(badge.notice)
    val text = own?.text?.takeIf { it.isNotBlank() } ?: notice
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 4.dp)
            .size(size)
            .then(if (own == null) Modifier.clip(MaterialShapes.Cookie12Sided.toShape()).background(scheme.primary) else Modifier)
            .clickable(interactionSource = null, indication = null) { Notice.show(text) },
    ) {
        if (own != null) {
            CustomEmoji(own, size)
        } else {
            Icon(badge.icon, contentDescription = notice, tint = scheme.onPrimary, modifier = Modifier.size(size * LogoScale))
        }
    }
}

object Notice {
    var text by androidx.compose.runtime.mutableStateOf<String?>(null)
    var stamp by androidx.compose.runtime.mutableLongStateOf(0L)

    fun show(value: String) {
        text = value
        stamp = System.currentTimeMillis()
    }
}

@Composable
fun NoticeHost(modifier: Modifier = Modifier) {
    val shown = Notice.text
    androidx.compose.runtime.LaunchedEffect(Notice.stamp) {
        if (shown != null) {
            kotlinx.coroutines.delay(NoticeMs)
            Notice.text = null
        }
    }
    var last by remember { androidx.compose.runtime.mutableStateOf("") }
    if (shown != null) last = shown
    androidx.compose.animation.AnimatedVisibility(
        visible = shown != null,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.85f) + androidx.compose.animation.slideInVertically { it / 2 },
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.9f),
        modifier = modifier,
    ) {
        androidx.compose.material3.Surface(
            onClick = { Notice.text = null },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp,
        ) {
            Text(last, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
        }
    }
}

private const val NoticeMs = 3_000L

@Composable
fun CustomEmoji(badge: CustomBadge, size: Dp) {
    CustomEmojiView(badge.customEmojiId, badge.emoji, size = size)
}

private const val LogoScale = 0.66f

private val CodeSlash: ImageVector by lazy {
    ImageVector.Builder("CodeSlash", 24.dp, 24.dp, 960f, 960f)
        .addPath(
            addPathNodes("M320 300L140 480L320 660 M640 300L820 480L640 660 M550 220L410 740"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 84f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}

private val PoopIcon: ImageVector by lazy {
    ImageVector.Builder("Poop", 24.dp, 24.dp, 960f, 960f)
        .addPath(
            addPathNodes("M230.0 600H730.0A100.0 100.0 0 0 1 730.0 800H230.0A100.0 100.0 0 0 1 230.0 600ZM315.0 430H645.0A95.0 95.0 0 0 1 645.0 620H315.0A95.0 95.0 0 0 1 315.0 430ZM395.0 280H565.0A85.0 85.0 0 0 1 565.0 450H395.0A85.0 85.0 0 0 1 395.0 280ZM440 290Q430 190 540 140Q520 210 590 290Z"),
            fill = SolidColor(Color.Black),
        )
        .build()
}

private val MenorahIcon: ImageVector by lazy {
    ImageVector.Builder("Menorah", 24.dp, 24.dp, 960f, 960f)
        .addPath(
            addPathNodes("M480 330L480 760 M390 330A90 90 0 0 0 570 330 M300 330A180 180 0 0 0 660 330 M210 330A270 270 0 0 0 750 330 M380 790L580 790"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 52f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .addPath(
            addPathNodes(
                (0 until 7).joinToString(" ") { i ->
                    val x = 210 + i * 90
                    "M${x - 38} 250L${x + 38} 250L${x + 22} 292L${x - 22} 292Z M$x 150Q${x + 30} 196 $x 228Q${x - 30} 196 $x 150Z"
                },
            ),
            fill = SolidColor(Color.Black),
        )
        .build()
}

private val BugNet: ImageVector by lazy {
    ImageVector.Builder("BugNet", 24.dp, 24.dp, 960f, 960f)
        .addPath(
            addPathNodes("M590 120a250 250 0 1 0 0.1 0Z M590 130L590 610 M350 370L830 370 M410 550L140 840"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 96f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}
