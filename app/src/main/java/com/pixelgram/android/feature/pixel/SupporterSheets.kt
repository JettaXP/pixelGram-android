package com.pixelgram.android.feature.pixel

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.ProfileKey
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

object SupporterWindows {
    var perks by mutableStateOf(false)
    var other by mutableStateOf<Long?>(null)
}

@Composable
private fun animationsOn(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
}

@Composable
fun SupporterSheetHost(graph: AppGraph, navigator: Navigator) {
    val snapshot by graph.store.snapshot.collectAsState()
    val supporters by graph.remote.supporters.collectAsState()
    if (SupporterWindows.perks) {
        val number = supporters.numbers[snapshot.meId]
        if (number != null) PerksSheet(number, navigator) { SupporterWindows.perks = false }
        else SupporterWindows.perks = false
    }
    SupporterWindows.other?.let { userId ->
        val number = supporters.numbers[userId]
        val user = snapshot.users[userId]
        val name = listOfNotNull(user?.firstName, user?.lastName).filter { it.isNotBlank() }.joinToString(" ")
        if (number != null) OtherSupporterSheet(number, name.ifBlank { stringResource(R.string.badge_supporter_someone) }) { SupporterWindows.other = null }
        else SupporterWindows.other = null
    }
}

@Composable
private fun logoTint(): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (dark) androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primary, Color.White, 0.65f) else MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerksSheet(number: Int, navigator: Navigator, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    val logo = painterResource(R.drawable.ic_pixelgram)
    val myId = com.pixelgram.android.LocalGraph.current.store.snapshot.collectAsState().value.meId
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainer, scrimColor = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(logo, null, tint = logoTint(), modifier = Modifier.size(120.dp))
            Text(stringResource(R.string.supporter_perks_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
            Text(stringResource(R.string.supporter_perks_you, number), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PerkRow(
                    Icons.Rounded.Verified,
                    stringResource(R.string.supporter_perk_badge_title),
                    stringResource(R.string.supporter_perk_badge_text),
                    chevron = true,
                    onClick = { close(); navigator.open(ProfileKey(myId)) },
                )
                PerkRow(
                    Icons.Rounded.Headphones,
                    stringResource(R.string.supporter_perk_now_title),
                    stringResource(R.string.supporter_perk_now_text),
                    chevron = true,
                    onClick = { close(); navigator.open(com.pixelgram.android.nav.NowPlayingKey) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherSupporterSheet(number: Int, name: String, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    val logo = painterResource(R.drawable.ic_pixelgram)
    val nickColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.15f)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainer, scrimColor = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(logo, null, tint = logoTint(), modifier = Modifier.size(120.dp))
            Box(Modifier.padding(top = 24.dp)) { BadgePill(stringResource(R.string.supporter_other_pill, number)) }
            Text(
                stringResource(R.string.supporter_other_title, name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(stringResource(R.string.supporter_other_sub), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PerkRow(Icons.Rounded.Verified, stringResource(R.string.supporter_perk_badge_title), stringResource(R.string.supporter_other_badge_text), chevron = false, onClick = {})
                PerkRow(Icons.Rounded.Headphones, stringResource(R.string.supporter_perk_now_title), stringResource(R.string.supporter_other_now_text), chevron = false, onClick = {})
            }
            Button(
                onClick = { close() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(48.dp),
            ) { Text(stringResource(R.string.supporter_other_got_it), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PerkRow(icon: ImageVector, title: String, desc: String, chevron: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 42.dp).weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
private fun BadgePill(text: String) {
    val animate = animationsOn()
    val a = MaterialTheme.colorScheme.primaryContainer
    val b = MaterialTheme.colorScheme.tertiaryContainer
    val shift = if (animate) rememberInfiniteTransition(label = "pill-shift").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "shift",
    ).value else 0.5f
    val sweep = if (animate) rememberInfiniteTransition(label = "pill-sweep").animateFloat(
        initialValue = -0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(keyframes { durationMillis = 3600; -0.6f at 0; 1.6f at 1600 }),
        label = "sweep",
    ).value else 2f
    Box(
        Modifier
            .clip(CircleShape)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(a, b, a),
                        start = Offset(size.width * (shift * 1.5f - 0.75f), 0f),
                        end = Offset(size.width * (shift * 1.5f + 0.75f), size.height),
                    ),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                )
                if (sweep in -0.5f..1.5f) {
                    val band = size.width * 0.4f
                    val x = size.width * sweep
                    rotate(degrees = 20f, pivot = Offset(x, size.height / 2f)) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.4f),
                            topLeft = Offset(x - band / 2f, -size.height),
                            size = Size(band, size.height * 3f),
                        )
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun ThankYouBanner(number: Int, onClick: () -> Unit) {
    val bg = Color(0xFFC5EFAB)
    val fg = Color(0xFF2D4F1C)
    val logo = painterResource(R.drawable.ic_pixelgram)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, top = 20.dp, bottom = 20.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.supporter_banner_title), color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, maxLines = 2)
            Text(stringResource(R.string.supporter_banner_subtitle, number), color = fg.copy(alpha = 0.8f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Box(
                Modifier
                    .padding(top = 16.dp)
                    .clip(CircleShape)
                    .background(fg)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 24.dp)
                    .height(36.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.supporter_banner_button), color = bg, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        Icon(logo, null, tint = fg, modifier = Modifier.size(85.dp).padding(end = 15.dp))
    }
}

