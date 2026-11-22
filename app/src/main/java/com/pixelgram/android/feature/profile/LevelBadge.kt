package com.pixelgram.android.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.android.R

private val LevelRes = listOf(
    R.drawable.profile_level1_inner to R.drawable.profile_level1_outer,
    R.drawable.profile_level2_inner to R.drawable.profile_level2_outer,
    R.drawable.profile_level3_inner to R.drawable.profile_level3_outer,
    R.drawable.profile_level4_inner to R.drawable.profile_level4_outer,
    R.drawable.profile_level5_inner to R.drawable.profile_level5_outer,
    R.drawable.profile_level6_inner to R.drawable.profile_level6_outer,
    R.drawable.profile_level7_inner to R.drawable.profile_level7_outer,
    R.drawable.profile_level8_inner to R.drawable.profile_level8_outer,
    R.drawable.profile_level9_inner to R.drawable.profile_level9_outer,
    R.drawable.profile_level10_inner to R.drawable.profile_level10_outer,
    R.drawable.profile_level20_inner to R.drawable.profile_level20_outer,
    R.drawable.profile_level30_inner to R.drawable.profile_level30_outer,
    R.drawable.profile_level40_inner to R.drawable.profile_level40_outer,
    R.drawable.profile_level50_inner to R.drawable.profile_level50_outer,
    R.drawable.profile_level60_inner to R.drawable.profile_level60_outer,
    R.drawable.profile_level70_inner to R.drawable.profile_level70_outer,
    R.drawable.profile_level80_inner to R.drawable.profile_level80_outer,
    R.drawable.profile_level90_inner to R.drawable.profile_level90_outer,
)

private fun levelIndex(level: Int): Int = (if (level <= 10) level - 1 else 8 + level / 10).coerceIn(0, LevelRes.lastIndex)

@Composable
fun LevelBadge(level: Int, size: Dp = 26.dp, onClick: (() -> Unit)? = null) {
    val (inner, outer) = if (level < 0) R.drawable.profile_level_minus_inner to R.drawable.profile_level_minus_outer else LevelRes[levelIndex(level)]
    val ink = MaterialTheme.colorScheme.onSurface
    Box(
        Modifier.size(size).then(if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(outer), null, colorFilter = ColorFilter.tint(ink.copy(alpha = 0.35f)), modifier = Modifier.fillMaxSize())
        Image(painterResource(inner), null, colorFilter = ColorFilter.tint(ink), modifier = Modifier.fillMaxSize())
        Text(
            if (level >= 0) level.toString() else "!",
            color = MaterialTheme.colorScheme.surface,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
