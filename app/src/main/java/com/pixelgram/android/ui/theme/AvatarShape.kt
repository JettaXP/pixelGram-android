package com.pixelgram.android.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath

enum class AvatarShape {
    Circle, Custom, Square, Slanted, Arch, Fan, Arrow, SemiCircle, Oval, Pill, Triangle, Diamond,
    ClamShell, Pentagon, Gem, Sunny, VerySunny, Cookie4Sided, Cookie6Sided, Cookie7Sided,
    Cookie9Sided, Cookie12Sided, Ghostish, Clover4Leaf, Clover8Leaf, Burst, SoftBurst, Boom,
    SoftBoom, Flower, Puffy, PuffyDiamond, PixelCircle, PixelTriangle, Bun, Heart;

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val polygon: RoundedPolygon?
        get() = when (this) {
            Circle, Custom -> null
            Square -> MaterialShapes.Square
            Slanted -> MaterialShapes.Slanted
            Arch -> MaterialShapes.Arch
            Fan -> MaterialShapes.Fan
            Arrow -> MaterialShapes.Arrow
            SemiCircle -> MaterialShapes.SemiCircle
            Oval -> MaterialShapes.Oval
            Pill -> MaterialShapes.Pill
            Triangle -> MaterialShapes.Triangle
            Diamond -> MaterialShapes.Diamond
            ClamShell -> MaterialShapes.ClamShell
            Pentagon -> MaterialShapes.Pentagon
            Gem -> MaterialShapes.Gem
            Sunny -> MaterialShapes.Sunny
            VerySunny -> MaterialShapes.VerySunny
            Cookie4Sided -> MaterialShapes.Cookie4Sided
            Cookie6Sided -> MaterialShapes.Cookie6Sided
            Cookie7Sided -> MaterialShapes.Cookie7Sided
            Cookie9Sided -> MaterialShapes.Cookie9Sided
            Cookie12Sided -> MaterialShapes.Cookie12Sided
            Ghostish -> MaterialShapes.Ghostish
            Clover4Leaf -> MaterialShapes.Clover4Leaf
            Clover8Leaf -> MaterialShapes.Clover8Leaf
            Burst -> MaterialShapes.Burst
            SoftBurst -> MaterialShapes.SoftBurst
            Boom -> MaterialShapes.Boom
            SoftBoom -> MaterialShapes.SoftBoom
            Flower -> MaterialShapes.Flower
            Puffy -> MaterialShapes.Puffy
            PuffyDiamond -> MaterialShapes.PuffyDiamond
            PixelCircle -> MaterialShapes.PixelCircle
            PixelTriangle -> MaterialShapes.PixelTriangle
            Bun -> MaterialShapes.Bun
            Heart -> MaterialShapes.Heart
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun shape(cornerPercent: Int): Shape = when (this) {
        Circle -> CircleShape
        Custom -> RoundedCornerShape(cornerPercent.coerceIn(0, 50))
        else -> polygon!!.toShape()
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun morphPolygon(cornerPercent: Int): RoundedPolygon = when (this) {
        Circle -> MaterialShapes.Circle
        Custom -> RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = androidx.graphics.shapes.CornerRounding(cornerPercent.coerceIn(0, 50) / 50f))
        else -> polygon!!
    }.normalized()
}

class MorphShape(private val morph: androidx.graphics.shapes.Morph, private val progress: Float) : Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val path = android.graphics.Path()
        morph.toPath(progress, path)
        val matrix = android.graphics.Matrix().apply { setScale(size.width, size.height) }
        path.transform(matrix)
        return androidx.compose.ui.graphics.Outline.Generic(path.asComposePath())
    }
}
