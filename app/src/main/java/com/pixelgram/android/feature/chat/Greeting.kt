package com.pixelgram.android.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import org.drinkless.tdlib.TdApi

@Composable
fun EmptyChatGreeting(graph: AppGraph, userId: Long, name: String, onSticker: (TdApi.Sticker) -> Unit, modifier: Modifier = Modifier) {
    var page by remember(userId) { mutableStateOf<TdApi.BusinessStartPage?>(null) }
    var loaded by remember(userId) { mutableStateOf(false) }
    var fallback by remember(userId) { mutableStateOf<TdApi.Sticker?>(null) }
    var how by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        page = runCatching { graph.td.send(TdApi.GetUserFullInfo(userId)).businessInfo?.startPage }.getOrNull()
        loaded = true
        if (page?.sticker == null) fallback = runCatching { graph.td.send(TdApi.GetGreetingStickers()).stickers.toList() }.getOrNull().orEmpty().randomOrNull()
    }
    val sticker = page?.sticker ?: fallback
    val snapshot by graph.store.snapshot.collectAsState()
    AnimatedVisibility(
        visible = loaded,
        enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.85f) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (page != null) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.greeting_start_page, name) + " ", style = MaterialTheme.typography.labelLarge)
                        Text(
                            stringResource(R.string.greeting_how),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(CircleShape).clickable { how = true },
                        )
                    }
                }
            }
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.widthIn(max = 280.dp)) {
                Column(
                    Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(page?.title?.ifBlank { null } ?: stringResource(R.string.greeting_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(page?.message?.ifBlank { null } ?: stringResource(R.string.greeting_text), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Box(Modifier.padding(top = 8.dp).size(GreetingSticker), contentAlignment = Alignment.Center) {
                        if (sticker != null) {
                            StickerView(
                                sticker,
                                snapshot.files,
                                { graph.store.requestFileAsync(it) },
                                Modifier.size(GreetingSticker).clip(RoundedCornerShape(20.dp)).clickable { onSticker(sticker) },
                            )
                        }
                    }
                }
            }
        }
    }
    if (how) {
        AlertDialog(
            onDismissRequest = { how = false },
            title = { Text(stringResource(R.string.greeting_how_title)) },
            text = { Text(stringResource(R.string.greeting_how_text)) },
            confirmButton = { TextButton(onClick = { how = false }) { Text(stringResource(R.string.action_done)) } },
        )
    }
}

private val GreetingSticker = 140.dp
