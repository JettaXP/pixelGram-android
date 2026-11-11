package com.pixelgram.android.feature.pixel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

object Credits {
    var open by mutableStateOf(false)
    val LinkPattern = Regex("tg://credits", RegexOption.IGNORE_CASE)

    fun handles(url: String): Boolean = url.trim().trimEnd('/').equals("tg://credits", ignoreCase = true)
}

private val Sections = listOf(
    "Developers" to listOf("homewatcha", "jettaxp", "Claude", "MrFaDev"),
    "Sponsors" to listOf("oodze"),
    "Testers" to listOf("homewatcha", "xtrvnn"),
    "Standing on the shoulders of" to listOf(
        "telegramdesktop/tdesktop",
        "tdlight-team/tdlight-java",
        "exteraSquad/exteraGram",
        "AyuGram/AyuGramDesktop",
        "MetrolistGroup/Metrolist",
        "LineageOS/android_frameworks_base",
        "MG-Devs-Team/Mategram",
        "YouRooni/plugins",
        "end-4/dots-hyprland",
        "kovidgoyal/kitty",
        "pytgcalls/ntgcalls",
        "hypfvieh/dbus-java",
        "google/material-design-icons",
        "Remix-Design/RemixIcon",
        "480 Design / Solar Icon Set",
    ),
)

@Composable
fun CreditsOverlay() {
    if (!Credits.open) return
    val scope = rememberCoroutineScope()
    val dim = remember { Animatable(0f) }
    val roll = remember { Animatable(0f) }
    var viewport by remember { mutableIntStateOf(0) }
    var content by remember { mutableIntStateOf(0) }
    var closing by remember { mutableStateOf(false) }
    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            dim.animateTo(0f, tween(CloseMs))
            Credits.open = false
        }
    }
    LaunchedEffect(Unit) { dim.animateTo(1f, tween(FadeMs)) }
    LaunchedEffect(viewport, content) {
        if (viewport == 0 || content == 0) return@LaunchedEffect
        roll.snapTo(0f)
        roll.animateTo(1f, tween(((viewport + content) * MsPerPx).toInt(), easing = LinearEasing))
        close()
    }
    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { viewport = it.height }
                .graphicsLayer { alpha = dim.value }
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .onSizeChanged { content = it.height }
                    .graphicsLayer { translationY = viewport - roll.value * (viewport + content) },
            ) {
                Text("pixelGram", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Spacer(Modifier.height(48.dp))
                Sections.forEach { (title, names) ->
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    names.forEach { name ->
                        Text(name, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(56.dp))
                }
                Text("Thank you for using pixelGram", style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(96.dp))
                Text("pixelGram © ${java.time.Year.now().value}", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
                Text("Free software under the GNU General Public License v3", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Text("Made by JettaX", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("with Kotlin, Jetpack Compose and TDLib", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center)
            }
        }
    }
}

private const val FadeMs = 700
private const val CloseMs = 220
private const val MsPerPx = 22f
