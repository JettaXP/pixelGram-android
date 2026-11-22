package com.pixelgram.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedCounter(value: Int, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified, format: @Composable (Int) -> String = { it.toString() }) {
    var previous by remember { mutableIntStateOf(value) }
    val up = value >= previous
    val text = format(value)
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    Row(modifier) {
        val padded = text.padStart(format(previous).length)
        padded.forEachIndexed { index, char ->
            if (char == ' ') return@forEachIndexed
            AnimatedContent(
                char,
                transitionSpec = {
                    if (up) (slideInVertically(spatial) { it } + fadeIn(effects)) togetherWith (slideOutVertically(spatial) { -it } + fadeOut(effects))
                    else (slideInVertically(spatial) { -it } + fadeIn(effects)) togetherWith (slideOutVertically(spatial) { it } + fadeOut(effects))
                },
                label = "digit$index",
            ) { c -> Text(c.toString(), style = style, color = color) }
        }
    }
    previous = value
}
