package com.pixelgram.android.feature.profile

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
import androidx.compose.material3.Icon
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
    showCamera: Boolean,
    onAvatarClick: () -> Unit,
    topStart: @Composable RowScope.() -> Unit = {},
    topEnd: @Composable RowScope.() -> Unit = {},
) {
    val snapshot by graph.store.snapshot.collectAsState()
    val banners by graph.remote.banners.collectAsState()
    LaunchedEffect(userId) { graph.remote.want(userId) }
    val banner = banners[userId]
    val surface = MaterialTheme.colorScheme.surface
    val shown by animateFloatAsState(if (banner != null) 1f else 0f, MaterialTheme.motionScheme.slowEffectsSpec(), label = "banner")
    Box(Modifier.fillMaxWidth()) {
        if (banner != null) {
            Box(Modifier.fillMaxWidth().height(BannerHeight)) {
                AsyncImage(
                    model = banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().graphicsLayer { alpha = shown },
                )
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to surface)))
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                topStart()
                Spacer(Modifier.weight(1f))
                topEnd()
            }
            Box {
                Avatar(
                    title = title,
                    seed = userId,
                    path = snapshot.files[photoFileId],
                    minithumb = minithumb,
                    size = 120.dp,
                    modifier = Modifier.clickable(onClick = onAvatarClick),
                    onNeedFile = { graph.store.requestFileAsync(photoFileId) },
                )
                if (showCamera) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(36.dp),
                        onClick = onAvatarClick,
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(20.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false),
                )
                NameBadges(userId, verified, 22.dp)
            }
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private val BannerHeight = 240.dp

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
