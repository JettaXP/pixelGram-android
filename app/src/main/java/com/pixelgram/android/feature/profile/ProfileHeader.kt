package com.pixelgram.android.feature.profile

import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelgram.android.AppGraph
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.NameBadges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File

@Composable
fun ProfileHeader(
    graph: AppGraph,
    userId: Long,
    title: String,
    subtitle: String?,
    photoFileId: Int,
    minithumb: ByteArray?,
    verified: Boolean,
    official: Boolean = false,
    showCamera: Boolean,
    onAvatarClick: () -> Unit,
    topStart: @Composable RowScope.() -> Unit = {},
    topEnd: @Composable RowScope.() -> Unit = {},
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    localPhoto: String? = null,
    expand: () -> Float = { 0f },
    loader: () -> Float = { 0f },
    refreshing: Boolean = false,
    onTitleClick: (() -> Unit)? = null,
    subtitleLeading: (@Composable () -> Unit)? = null,
    belowSubtitle: (@Composable () -> Unit)? = null,
    orbit: List<org.drinkless.tdlib.TdApi.UpgradedGift> = emptyList(),
) {
    val snapshot by graph.store.snapshot.collectAsState()
    val banners by graph.remote.banners.collectAsState()
    LaunchedEffect(userId) { graph.remote.want(userId) }
    val banner = banners[userId]
    val surface = MaterialTheme.colorScheme.surface
    val shown by animateFloatAsState(if (banner != null) 1f else 0f, MaterialTheme.motionScheme.slowEffectsSpec(), label = "banner")
    val density = androidx.compose.ui.platform.LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    var infoHeight by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.dp) }
    val path = localPhoto ?: snapshot.files[photoFileId]
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val e = expand().coerceIn(0f, 1f)
        val full = maxWidth
        val avatarTop = statusTop + 64.dp
        val collapsedHeight = avatarTop + AvatarSize + 14.dp + infoHeight
        val height = collapsedHeight + (maxOf(full, collapsedHeight) - collapsedHeight) * e
        val size = AvatarSize + (full - AvatarSize) * e
        val top = avatarTop * (1f - e)
        Box(Modifier.fillMaxWidth().height(height + 12.dp * e)) {
            val dark = com.pixelgram.android.ui.theme.LocalDarkTheme.current
            val profileUser = snapshot.users[userId]
            val profileChat = if (profileUser == null) graph.store.chat(userId) else null
            val colorId = profileUser?.profileAccentColorId ?: profileChat?.profileAccentColorId ?: -1
            val patternId = profileUser?.profileEmojiId ?: profileChat?.profileBackgroundCustomEmojiId ?: 0L
            val profileColors = if (colorId >= 0) com.pixelgram.android.td.ProfileAccentColors.background(colorId, dark) else null
            if (banner == null && (profileColors != null || patternId != 0L)) {
                val center = avatarTop + AvatarSize / 2
                Box(Modifier.fillMaxWidth().height(avatarTop + AvatarSize + 14.dp + infoHeight + 24.dp).graphicsLayer { alpha = 1f - e }) {
                    if (profileColors != null) {
                        Box(
                            Modifier.matchParentSize().background(
                                Brush.radialGradient(
                                    listOf(profileColors.first(), profileColors.last()),
                                    center = androidx.compose.ui.geometry.Offset.Unspecified.let { androidx.compose.ui.geometry.Offset(with(density) { (full / 2).toPx() }, with(density) { center.toPx() }) },
                                    radius = with(density) { (full * 1.25f).toPx() },
                                ),
                            ),
                        )
                        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.75f to Color.Transparent, 1f to surface)))
                    }
                    if (patternId != 0L) {
                        val tint = if (profileColors != null) Color.White else MaterialTheme.colorScheme.primary
                        ProfilePatternRings.forEach { ring ->
                            ring.angles.forEach { a ->
                                val rad = Math.toRadians(a.toDouble())
                                val r = AvatarSize / 2 + ring.distance
                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(x = r * kotlin.math.cos(rad).toFloat(), y = center + r * kotlin.math.sin(rad).toFloat() - ring.size / 2)
                                        .graphicsLayer { alpha = ring.alpha },
                                ) { com.pixelgram.android.ui.components.CustomEmojiView(patternId, "", size = ring.size, tint = tint, animate = false, forceTint = true) }
                            }
                        }
                    }
                }
            }
            if (banner != null) {
                Box(Modifier.fillMaxWidth().aspectRatio(BannerRatio).graphicsLayer { alpha = shown * (1f - e) }) {
                    AsyncImage(model = banner, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to surface)))
                }
            }
            if (e > 0f) {
                androidx.compose.runtime.CompositionLocalProvider(com.pixelgram.android.ui.theme.LocalAvatarShape provides RoundedCornerShape((AvatarSize / 2) * (1f - e))) {
                    Box(Modifier.align(Alignment.TopCenter).offset(y = top).graphicsLayer { alpha = (e * 2.5f).coerceAtMost(1f) }) {
                        Avatar(
                            title = title,
                            seed = userId,
                            path = path,
                            minithumb = if (icon != null) null else minithumb,
                            size = size,
                            icon = icon,
                            modifier = Modifier.clickable(onClick = onAvatarClick),
                            onNeedFile = { graph.store.requestFileAsync(photoFileId) },
                        )
                    }
                }
            }
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = avatarTop * (1f - e)).graphicsLayer {
                    alpha = (1f - e * 2.5f).coerceIn(0f, 1f)
                    val grow = 1f + e * 0.4f
                    scaleX = grow
                    scaleY = grow
                },
            ) {
                Avatar(
                    title = title,
                    seed = userId,
                    path = path,
                    minithumb = if (icon != null) null else minithumb,
                    size = AvatarSize,
                    icon = icon,
                    modifier = Modifier.clickable(enabled = e < 0.3f, onClick = onAvatarClick),
                    onNeedFile = { graph.store.requestFileAsync(photoFileId) },
                    official = official,
                )
                if (showCamera && e < 0.05f) {
                    androidx.compose.material3.FilledIconButton(
                        onClick = onAvatarClick,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(40.dp),
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(20.dp))
                    }
                }
            }
            if (orbit.isNotEmpty() && e < 0.4f) {
                val box = maxOf(AvatarSize, 96.dp)
                val acx = full / 2
                val ay = avatarTop - (box - AvatarSize) / 2
                val acy = ay + box / 2
                val spots = listOf(
                    (acx * 2f / 3f - 6.dp) to (ay - 4.dp),
                    (acx * 4f / 3f + 12.dp) to (ay - 4.dp),
                    (acx / 2f - 20.dp) to (acy - 13.dp),
                    (acx * 1.5f + 20.dp) to (acy - 13.dp),
                    (acx * 2f / 3f - 12.dp) to (ay + box - 16.dp),
                    (acx * 4f / 3f + 12.dp) to (ay + box - 16.dp),
                )
                val appear by androidx.compose.animation.core.animateFloatAsState(1f - e * 2.5f, label = "orbit")
                orbit.take(6).forEachIndexed { i, gift ->
                    val (cx, cy) = spots[i]
                    val glow = gift.backdrop?.colors?.let { com.pixelgram.android.feature.chat.giftColor(it.centerColor) } ?: MaterialTheme.colorScheme.primary
                    val enter = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(gift.name) {
                        kotlinx.coroutines.delay(i * 60L)
                        enter.animateTo(1f, androidx.compose.animation.core.spring(0.55f, 300f))
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(x = cx - 22.5.dp, y = cy - 22.5.dp)
                            .size(45.dp)
                            .graphicsLayer {
                                val v = enter.value * appear.coerceIn(0f, 1f)
                                alpha = v.coerceIn(0f, 1f)
                                val sc = 0.5f + 0.5f * v
                                scaleX = sc
                                scaleY = sc
                            }
                            .background(Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f))), androidx.compose.foundation.shape.CircleShape)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable { com.pixelgram.android.feature.chat.GiftSheets.current = com.pixelgram.android.feature.chat.GiftTarget.Unique(gift.name) },
                        contentAlignment = Alignment.Center,
                    ) {
                        gift.model?.sticker?.let { sticker ->
                            com.pixelgram.android.feature.chat.StickerView(sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.size(24.dp), video = false)
                        }
                    }
                }
            }
            if (e > 0f) {
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = top).size(size).graphicsLayer { alpha = e }.background(
                        Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)),
                    ),
                )
                Column(Modifier.align(Alignment.TopStart).offset(y = top + size - 76.dp).padding(start = 20.dp, end = 20.dp).graphicsLayer { alpha = ((e - 0.5f) * 2f).coerceIn(0f, 1f) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.headlineSmallEmphasized, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        NameBadges(userId, verified, 24.dp, snapshot.users[userId]?.scam == true, snapshot.users[userId]?.fake == true, name = title, label = false, official = official)
                    }
                    if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f))
                }
            }
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = avatarTop + AvatarSize + 14.dp)
                    .fillMaxWidth()
                    .onSizeChanged { infoHeight = with(density) { it.height.toDp() } }
                    .graphicsLayer { alpha = (1f - e * 2.5f).coerceIn(0f, 1f) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.animation.AnimatedContent(
                        title,
                        transitionSpec = {
                            (androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 }) togetherWith
                                (androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it / 2 })
                        },
                        label = "profile-title",
                        modifier = Modifier.weight(1f, fill = false),
                    ) { value ->
                        Text(
                            value,
                            style = MaterialTheme.typography.headlineSmallEmphasized,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = if (onTitleClick != null) Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onTitleClick).padding(horizontal = 6.dp) else Modifier,
                        )
                    }
                    NameBadges(userId, verified, 28.dp, snapshot.users[userId]?.scam == true, snapshot.users[userId]?.fake == true, name = title, label = true, official = official)
                }
                if (!subtitle.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                        subtitleLeading?.invoke()
                        androidx.compose.animation.AnimatedContent(subtitle, label = "profile-subtitle") { value ->
                            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                belowSubtitle?.let {
                    Spacer(Modifier.height(10.dp))
                    it()
                }
                Spacer(Modifier.height(16.dp))
            }
            val tint = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.onSurface, Color.White, e)
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides tint) {
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(64.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    topStart()
                    Spacer(Modifier.weight(1f))
                    topEnd()
                }
            }
            val pullLoader = loader().coerceIn(0f, 1f)
            if (pullLoader > 0f || refreshing) {
                val shift = if (refreshing) 1f else pullLoader
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = (-64).dp + (statusTop + 76.dp) * shift),
                ) {
                    @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
                    if (refreshing) androidx.compose.material3.ContainedLoadingIndicator(Modifier.size(52.dp))
                    else androidx.compose.material3.ContainedLoadingIndicator(progress = { pullLoader }, modifier = Modifier.size(52.dp))
                }
            }
        }
    }
}

private val AvatarSize = 120.dp
private const val BannerRatio = 3f

@Composable
fun rememberAvatarPicker(graph: AppGraph): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) graph.scope.launch { uploadAvatar(context, graph, uri) }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}

private suspend fun uploadAvatar(context: Context, graph: AppGraph, uri: Uri) {
    val file = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "avatar-${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: return@withContext null
        target
    } ?: return
    runCatching {
        graph.td.send(TdApi.SetProfilePhoto(TdApi.InputChatPhotoStatic(TdApi.InputFileLocal(file.absolutePath)), false))
    }
}

private class PatternRing(val distance: androidx.compose.ui.unit.Dp, val size: androidx.compose.ui.unit.Dp, val alpha: Float, val angles: List<Float>)

private val ProfilePatternRings = listOf(
    PatternRing(30.dp, 20.dp, 0.5f, listOf(0f, 60f, 120f, 180f, 240f, 300f)),
    PatternRing(64.dp, 16.dp, 0.32f, listOf(30f, 75f, 105f, 150f, 210f, 255f, 285f, 330f)),
    PatternRing(100.dp, 13.dp, 0.18f, listOf(0f, 40f, 140f, 180f, 220f, 320f)),
)
