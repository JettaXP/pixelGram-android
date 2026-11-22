package com.pixelgram.android.ui.theme

import androidx.compose.ui.graphics.toArgb
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pixelgram.android.settings.AppearanceSettings
import com.pixelgram.android.settings.ThemeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixelTheme(settings: AppearanceSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val scheme = remember(dark, settings.dynamicColor, settings.monochrome) {
        when {
            settings.monochrome -> if (dark) MonochromeDarkScheme else MonochromeLightScheme
            settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dark -> PixelDarkScheme
            else -> PixelLightScheme
        }
    }
    androidx.compose.runtime.SideEffect { com.pixelgram.android.notify.NotificationAccent.save(context, scheme.primary.toArgb()) }
    val typography = remember(settings.appFont, settings.emojiStyle, settings.fontStamp) { pixelTypography(AppFonts.family(context, settings.appFont, settings.emojiStyle)) }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = typography,
        motionScheme = MotionScheme.expressive(),
    ) {
        CompositionLocalProvider(
            LocalAvatarShape provides animatedAvatarShape(settings.avatarShape, settings.avatarCorner),
            LocalDarkTheme provides dark,
            LocalAppearance provides settings,
            content = content,
        )
    }
}

@Composable
private fun animatedAvatarShape(shape: AvatarShape, corner: Int): androidx.compose.ui.graphics.Shape {
    val target = shape.shape(corner)
    val key = shape to corner
    var from by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(key) }
    val progress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(1f) }
    val morph = androidx.compose.runtime.remember(from, key) {
        if (from == key) null else runCatching { androidx.graphics.shapes.Morph(from.first.morphPolygon(from.second), shape.morphPolygon(corner)) }.getOrNull()
    }
    androidx.compose.runtime.LaunchedEffect(key) {
        if (from != key) {
            progress.snapTo(0f)
            progress.animateTo(1f, androidx.compose.animation.core.spring(0.6f, 300f))
            from = key
        }
    }
    return if (morph != null && progress.value < 1f) MorphShape(morph, progress.value) else target
}
