package com.pixelgram.android.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.RoundedPolygon

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
}
