package com.pixelgram.android.feature.player

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot

@SuppressLint("RestrictedApi")
internal fun schemeFromSeed(seed: Int, dark: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed), dark, 0.0)
    val roles = MaterialDynamicColors()
    fun c(color: DynamicColor) = Color(color.getArgb(scheme))
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c(roles.primary()),
        onPrimary = c(roles.onPrimary()),
        primaryContainer = c(roles.primaryContainer()),
        onPrimaryContainer = c(roles.onPrimaryContainer()),
        inversePrimary = c(roles.inversePrimary()),
        secondary = c(roles.secondary()),
        onSecondary = c(roles.onSecondary()),
        secondaryContainer = c(roles.secondaryContainer()),
        onSecondaryContainer = c(roles.onSecondaryContainer()),
        tertiary = c(roles.tertiary()),
        onTertiary = c(roles.onTertiary()),
        tertiaryContainer = c(roles.tertiaryContainer()),
        onTertiaryContainer = c(roles.onTertiaryContainer()),
        background = c(roles.background()),
        onBackground = c(roles.onBackground()),
        surface = c(roles.surface()),
        onSurface = c(roles.onSurface()),
        surfaceVariant = c(roles.surfaceVariant()),
        onSurfaceVariant = c(roles.onSurfaceVariant()),
        inverseSurface = c(roles.inverseSurface()),
        inverseOnSurface = c(roles.inverseOnSurface()),
        outline = c(roles.outline()),
        outlineVariant = c(roles.outlineVariant()),
        surfaceBright = c(roles.surfaceBright()),
        surfaceDim = c(roles.surfaceDim()),
        surfaceContainerLowest = c(roles.surfaceContainerLowest()),
        surfaceContainerLow = c(roles.surfaceContainerLow()),
        surfaceContainer = c(roles.surfaceContainer()),
        surfaceContainerHigh = c(roles.surfaceContainerHigh()),
        surfaceContainerHighest = c(roles.surfaceContainerHighest()),
        surfaceTint = c(roles.primary()),
    )
}

internal fun coverPalette(path: String): List<Color>? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= PaletteSize) sample *= 2
    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    bitmap.recycle()
    val all = vividColor(pixels, 0 until height, width) ?: return null
    val bands = listOf(0 until height / 3, height / 3 until height * 2 / 3, height * 2 / 3 until height)
    return bands.map { rows -> vividColor(pixels, rows, width) ?: all } + all
}

private fun vividColor(pixels: IntArray, rows: IntRange, width: Int): Color? {
    val weight = FloatArray(HueBuckets)
    val r = FloatArray(HueBuckets)
    val g = FloatArray(HueBuckets)
    val b = FloatArray(HueBuckets)
    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var count = 0
    val hsv = FloatArray(3)
    for (y in rows) for (x in 0 until width) {
        val p = pixels[y * width + x]
        val pr = (p shr 16) and 0xFF
        val pg = (p shr 8) and 0xFF
        val pb = p and 0xFF
        sumR += pr
        sumG += pg
        sumB += pb
        count++
        android.graphics.Color.RGBToHSV(pr, pg, pb, hsv)
        val w = hsv[1] * hsv[1] * hsv[2]
        if (w < MinVivid) continue
        val bucket = (hsv[0] / 360f * HueBuckets).toInt().coerceIn(0, HueBuckets - 1)
        weight[bucket] += w
        r[bucket] += pr * w
        g[bucket] += pg * w
        b[bucket] += pb * w
    }
    if (count == 0) return null
    val best = weight.indices.maxByOrNull { weight[it] } ?: return null
    val (cr, cg, cb) = if (weight[best] > count * MinShare) {
        Triple(r[best] / weight[best], g[best] / weight[best], b[best] / weight[best])
    } else {
        Triple(sumR.toFloat() / count, sumG.toFloat() / count, sumB.toFloat() / count)
    }
    android.graphics.Color.RGBToHSV(cr.toInt(), cg.toInt(), cb.toInt(), hsv)
    hsv[1] = (hsv[1] * 1.2f).coerceIn(0.35f, 1f)
    hsv[2] = hsv[2].coerceIn(0.5f, 0.95f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private const val HueBuckets = 24
private const val MinVivid = 0.04f
private const val MinShare = 0.002f
private const val PaletteSize = 96
