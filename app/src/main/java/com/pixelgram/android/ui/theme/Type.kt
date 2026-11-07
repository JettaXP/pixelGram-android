package com.pixelgram.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pixelgram.android.R

val GoogleSans = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold),
)

private fun TextStyle.sans() = copy(fontFamily = GoogleSans)

internal fun pixelTypography(base: Typography = Typography()): Typography = base.copy(
    displayLarge = base.displayLarge.sans(),
    displayMedium = base.displayMedium.sans(),
    displaySmall = base.displaySmall.sans(),
    headlineLarge = base.headlineLarge.sans(),
    headlineMedium = base.headlineMedium.sans(),
    headlineSmall = base.headlineSmall.sans(),
    titleLarge = base.titleLarge.sans(),
    titleMedium = base.titleMedium.sans(),
    titleSmall = base.titleSmall.sans(),
    bodyLarge = base.bodyLarge.sans(),
    bodyMedium = base.bodyMedium.sans(),
    bodySmall = base.bodySmall.sans(),
    labelLarge = base.labelLarge.sans(),
    labelMedium = base.labelMedium.sans(),
    labelSmall = base.labelSmall.sans(),
    displayLargeEmphasized = base.displayLargeEmphasized.sans(),
    displayMediumEmphasized = base.displayMediumEmphasized.sans(),
    displaySmallEmphasized = base.displaySmallEmphasized.sans(),
    headlineLargeEmphasized = base.headlineLargeEmphasized.sans(),
    headlineMediumEmphasized = base.headlineMediumEmphasized.sans(),
    headlineSmallEmphasized = base.headlineSmallEmphasized.sans(),
    titleLargeEmphasized = base.titleLargeEmphasized.sans(),
    titleMediumEmphasized = base.titleMediumEmphasized.sans(),
    titleSmallEmphasized = base.titleSmallEmphasized.sans(),
    bodyLargeEmphasized = base.bodyLargeEmphasized.sans(),
    bodyMediumEmphasized = base.bodyMediumEmphasized.sans(),
    bodySmallEmphasized = base.bodySmallEmphasized.sans(),
    labelLargeEmphasized = base.labelLargeEmphasized.sans(),
    labelMediumEmphasized = base.labelMediumEmphasized.sans(),
    labelSmallEmphasized = base.labelSmallEmphasized.sans(),
)
