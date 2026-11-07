package com.pixelgram.android.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape

val LocalAvatarShape = staticCompositionLocalOf<Shape> { CircleShape }
val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalAppearance = androidx.compose.runtime.compositionLocalOf { com.pixelgram.android.settings.AppearanceSettings() }

fun countLabel(value: Int, exact: Boolean): String = if (!exact && value > 999) "999+" else value.toString()
