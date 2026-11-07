package com.pixelgram.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.pixelgram.android.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HiddenPhone(
    phone: String,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    var revealed by remember(phone) { mutableStateOf(false) }
    val shapes = listOf(
        MaterialShapes.Cookie7Sided.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
        MaterialShapes.Sunny.toShape(),
        MaterialShapes.Pill.toShape(),
        MaterialShapes.Gem.toShape(),
        MaterialShapes.Cookie12Sided.toShape(),
        MaterialShapes.SoftBurst.toShape(),
        MaterialShapes.Diamond.toShape(),
    )
    val description = stringResource(if (revealed || !hidden) R.string.phone_shown else R.string.phone_hidden)
    val fast = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val fastSpatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    AnimatedContent(
        targetState = hidden && !revealed,
        transitionSpec = {
            (fadeIn(fast) + scaleIn(fastSpatial, initialScale = 0.9f)).togetherWith(fadeOut(fast))
        },
        label = "hidden-phone",
        modifier = modifier
            .clickable(enabled = hidden) { revealed = !revealed }
            .semantics { contentDescription = description },
    ) { masked ->
        if (!masked) {
            Text(phone, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                var digitIndex = 0
                for (char in phone) {
                    when {
                        char.isDigit() -> {
                            val shape = shapes[(digitIndex * 3 + char.code) % shapes.size]
                            digitIndex++
                            Box(
                                Modifier
                                    .padding(horizontal = 1.dp)
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant, shape),
                            )
                        }
                        char == ' ' -> Spacer(Modifier.width(6.dp))
                        else -> Text(char.toString(), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
