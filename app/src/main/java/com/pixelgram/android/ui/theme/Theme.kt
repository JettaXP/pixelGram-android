package com.pixelgram.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
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
    val typography = remember { pixelTypography() }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = typography,
        motionScheme = MotionScheme.expressive(),
    ) {
        CompositionLocalProvider(
            LocalAvatarShape provides settings.avatarShape.shape(settings.avatarCorner),
            LocalDarkTheme provides dark,
            LocalAppearance provides settings,
            content = content,
        )
    }
}
