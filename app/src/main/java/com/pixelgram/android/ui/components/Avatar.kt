package com.pixelgram.android.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pixelgram.android.ui.theme.LocalAvatarShape
import com.pixelgram.android.ui.theme.LocalDarkTheme
import com.pixelgram.android.util.initials

private val AvatarHues = floatArrayOf(4f, 28f, 48f, 140f, 190f, 215f, 265f, 320f)

@Composable
fun avatarColor(seed: Long): Color {
    val hue = AvatarHues[((seed % AvatarHues.size + AvatarHues.size) % AvatarHues.size).toInt()]
    val dark = LocalDarkTheme.current
    return Color.hsl(hue, if (dark) 0.45f else 0.55f, if (dark) 0.35f else 0.62f)
}

@Composable
fun Avatar(
    title: String,
    seed: Long,
    path: String?,
    minithumb: ByteArray?,
    size: Dp,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onNeedFile: (() -> Unit)? = null,
) {
    if (path == null && onNeedFile != null) LaunchedEffect(seed) { onNeedFile() }
    val color = avatarColor(seed)
    Box(
        modifier
            .size(size)
            .clip(LocalAvatarShape.current)
            .background(if (icon != null) MaterialTheme.colorScheme.primary else color),
        contentAlignment = Alignment.Center,
    ) {
        when {
            icon != null -> Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(size * 0.5f))
            path != null -> AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(java.io.File(path)).crossfade(false).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            minithumb != null -> {
                val bitmap = remember(minithumb) { BitmapFactory.decodeByteArray(minithumb, 0, minithumb.size)?.asImageBitmap() }
                if (bitmap != null) {
                    Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(2.dp))
                }
            }
            else -> Text(
                initials(title),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }
}
