package com.pixelgram.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixelgram.android.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun pixelShapes(): List<Shape> = listOf(
    MaterialShapes.Cookie9Sided.toShape(),
    MaterialShapes.Sunny.toShape(),
    MaterialShapes.Clover4Leaf.toShape(),
    MaterialShapes.Pill.toShape(),
    MaterialShapes.Cookie12Sided.toShape(),
    MaterialShapes.Gem.toShape(),
    MaterialShapes.SoftBurst.toShape(),
    MaterialShapes.Pentagon.toShape(),
    MaterialShapes.Flower.toShape(),
    MaterialShapes.Cookie6Sided.toShape(),
)

@Composable
fun ShapeCodeField(code: String, length: Int, error: Boolean, modifier: Modifier = Modifier) {
    val shapes = pixelShapes()
    val shake = rememberShake(error)
    val slots = length.coerceIn(4, 8).coerceAtLeast(code.length)
    Row(
        modifier = modifier
            .offset { IntOffset(shake.value.roundToInt(), 0) }
            .semantics { contentDescription = code },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (index in 0 until slots) {
            val digit = code.getOrNull(index)
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (digit == null) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                } else {
                    key(index, digit) {
                        PopIn {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(shapes[(index + digit.code) % shapes.size])
                                    .background(
                                        if (error) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    digit.toString(),
                                    style = MaterialTheme.typography.titleLargeEmphasized,
                                    color = if (error) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopIn(content: @Composable () -> Unit) {
    val grow = remember { Animatable(0f) }
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(Unit) { grow.animateTo(1f, spatial) }
    Box(
        Modifier.graphicsLayer {
            val s = 0.4f + 0.6f * grow.value
            scaleX = s
            scaleY = s
            alpha = (grow.value * 3f).coerceAtMost(1f)
            rotationZ = (1f - grow.value) * -30f
        },
    ) { content() }
}

@Composable
private fun rememberShake(trigger: Boolean): Animatable<Float, *> {
    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        val step = with(density) { 10.dp.toPx() }
        for (k in floatArrayOf(-1f, 1f, -0.8f, 0.8f, -0.4f, 0.4f, 0f)) shake.animateTo(k * step, tween(50))
    }
    return shake
}
