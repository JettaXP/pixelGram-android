package com.pixelgram.android.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape

val LocalAvatarShape = staticCompositionLocalOf<Shape> { CircleShape }
val LocalDarkTheme = staticCompositionLocalOf { false }
