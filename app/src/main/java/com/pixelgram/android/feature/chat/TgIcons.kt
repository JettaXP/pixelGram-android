package com.pixelgram.android.feature.chat

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.pixelgram.android.R

@Composable
private fun tint(color: Color) = rememberLottieDynamicProperties(
    rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, PorterDuffColorFilter(color.toArgb(), PorterDuff.Mode.SRC_ATOP), "**"),
)

@Composable
fun TgEmojiIcon(keyboard: Boolean, color: Color, modifier: Modifier) {
    val toKeyboard by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.tg_smile_to_keyboard))
    val toSmile by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.tg_keyboard_to_smile))
    val progress = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(keyboard) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(IconMs, easing = LinearEasing))
    }
    LottieAnimation(if (keyboard) toKeyboard else toSmile, { progress.value }, modifier, dynamicProperties = tint(color))
}

@Composable
fun TgVoiceVideoIcon(video: Boolean, color: Color, modifier: Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.tg_voice_and_video))
    val progress = remember { Animatable(if (video) 0f else 0.5f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(video) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        if (video) {
            progress.snapTo(0.5f)
            progress.animateTo(1f, tween(IconMs, easing = LinearEasing))
        } else {
            progress.snapTo(0f)
            progress.animateTo(0.5f, tween(IconMs, easing = LinearEasing))
        }
    }
    LottieAnimation(composition, { progress.value }, modifier, dynamicProperties = tint(color))
}

private const val IconMs = 500
