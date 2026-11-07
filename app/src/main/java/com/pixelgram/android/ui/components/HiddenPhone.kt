package com.pixelgram.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
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
    val masked = hidden && !revealed
    val shapes = listOf(
        MaterialShapes.Circle.toShape(),
        MaterialShapes.Cookie6Sided.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
    )
    val mask by animateFloatAsState(if (masked) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "phone-mask")
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(if (masked) R.string.phone_hidden else R.string.phone_shown)
    val text = androidx.compose.ui.text.buildAnnotatedString {
        for (char in phone) {
            if (char.isDigit()) {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = color.copy(alpha = color.alpha * (1f - mask))))
                append(char)
                pop()
            } else {
                append(char)
            }
        }
    }
    Text(
        text,
        style = style,
        color = color,
        onTextLayout = { layout = it },
        modifier = modifier
            .clickable(enabled = hidden) { revealed = !revealed }
            .padding(vertical = 4.dp)
            .semantics { contentDescription = description }
            .drawWithContent {
                drawContent()
                val measured = layout
                if (measured == null || mask <= 0f) return@drawWithContent
                var digit = 0
                phone.forEachIndexed { index, char ->
                    if (!char.isDigit()) return@forEachIndexed
                    val box = measured.getBoundingBox(index)
                    val side = minOf(box.width, box.height * 0.62f) * (0.6f + 0.4f * mask)
                    val shape = shapes[(digit * 5 + char.code) % shapes.size]
                    digit++
                    val outline = shape.createOutline(Size(side, side), layoutDirection, this)
                    translate(box.center.x - side / 2, box.center.y - side / 2) {
                        drawOutline(outline, color.copy(alpha = color.alpha * mask))
                    }
                }
            },
    )
}
