package com.pixelgram.android.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

fun gramAmount(value: Long): String = java.math.BigDecimal.valueOf(value, 9).stripTrailingZeros().let {
    if (it.scale() > 3) it.setScale(3, java.math.RoundingMode.DOWN).stripTrailingZeros() else it
}.toPlainString()

@Composable
fun BalanceChip(card: TdApi.TonWalletCard, owner: String, self: Boolean) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var open by remember { mutableStateOf(false) }
    Surface(
        onClick = { open = true },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.onGloballyPositioned { bounds = it.boundsInRoot() }.graphicsLayer { alpha = if (open) 0f else 1f },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
            Text(stringResource(R.string.profile_balance) + ": ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Image(painterResource(R.drawable.mini_gram_16), null, Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text(gramAmount(card.gramAmount), style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(16.dp))
        }
    }
    if (open) BalanceCard(card, owner, self, bounds) { open = false }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BalanceCard(card: TdApi.TonWalletCard, owner: String, self: Boolean, from: Rect, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var qr by remember { mutableStateOf(false) }
    val close: () -> Unit = {
        scope.launch {
            progress.animateTo(0f, androidx.compose.animation.core.spring(1f, 500f))
            onDismiss()
        }
    }
    LaunchedEffect(Unit) { progress.animateTo(1f, androidx.compose.animation.core.spring(0.72f, 260f)) }
    BackHandler(onBack = close)
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val density = LocalDensity.current
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val cardWidth = width - with(density) { 32.dp.toPx() }
            val cardHeight = cardWidth / 1.6f
            val targetTop = (height - cardHeight) / 2f
            val p = progress.value
            val startScaleX = if (from.width > 0f) from.width / cardWidth else 0.3f
            val startScaleY = if (from.height > 0f) from.height / cardHeight else 0.1f
            val dx = from.center.x - width / 2f
            val dy = from.center.y - (targetTop + cardHeight / 2f)
            Text(
                stringResource(R.string.balance_public),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 48.dp, start = 40.dp, end = 40.dp).graphicsLayer { alpha = p.coerceIn(0f, 1f) },
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = with(density) { targetTop.toDp() })
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin.Center
                        scaleX = startScaleX + (1f - startScaleX) * p
                        scaleY = startScaleY + (1f - startScaleY) * p
                        translationX = dx * (1f - p)
                        translationY = dy * (1f - p)
                        rotationX = (1f - p) * -18f
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2AA3FF), Color(0xFF0A7BEF), Color(0xFF1E8CF5))))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Column(Modifier.align(Alignment.CenterStart).padding(start = 24.dp).graphicsLayer { alpha = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.mini_gram_72_white), null, Modifier.size(30.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(gramAmount(card.gramAmount), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Text("GRAM", color = Color.White.copy(alpha = 0.75f), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    owner,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(24.dp).graphicsLayer { alpha = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f) },
                )
                Surface(
                    onClick = { qr = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 52.dp).size(56.dp, 42.dp),
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.QrCode2, null, tint = Color(0xFF555B66)) } }
                Text(
                    card.address.chunked(4).joinToString(" "),
                    color = Color(0xFF0A4E9E).copy(alpha = 0.75f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(with(density) { cardHeight.toDp() } - 32.dp)
                        .graphicsLayer { rotationZ = 90f; translationX = size.width / 2f - 14.dp.toPx() },
                )
            }
            Button(
                onClick = {
                    com.pixelgram.android.nav.PendingNav.external = "https://tonviewer.com/${card.address}"
                    close()
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp).height(52.dp).graphicsLayer { alpha = p.coerceIn(0f, 1f) },
            ) { Text(stringResource(if (self) R.string.balance_open_wallet else R.string.balance_open_explorer)) }
        }
    }
    if (qr) ProfileQrSheet(card.address, link = "ton://transfer/${card.address}") { qr = false }
}
