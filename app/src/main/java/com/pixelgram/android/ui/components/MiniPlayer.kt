package com.pixelgram.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.theme.LocalAvatarShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayer(modifier: Modifier = Modifier) {
    val music = LocalGraph.current.music
    val state by music.state.collectAsState()
    val track = state.track
    AnimatedVisibility(
        visible = track != null,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier,
    ) {
        if (track == null) return@AnimatedVisibility
        val dismiss = rememberSwipeToDismissBoxState()
        LaunchedEffect(dismiss.currentValue) { if (dismiss.currentValue != SwipeToDismissBoxValue.Settled) music.stop() }
        SwipeToDismissBox(state = dismiss, backgroundContent = {}, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(LocalAvatarShape.current).background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        val image: Any? = track.cover?.let { java.io.File(it) } ?: track.minithumb
                        if (image != null) {
                            AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp))
                        } else {
                            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (track.performer.isNotBlank()) {
                            Text(track.performer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = music::previous) { Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous), tint = MaterialTheme.colorScheme.primary) }
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        if (state.loading) {
                            LoadingIndicator(Modifier.size(48.dp))
                        } else {
                            val progress = if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f
                            CircularWavyProgressIndicator(progress = { progress }, modifier = Modifier.size(48.dp))
                            IconButton(onClick = music::toggle) {
                                Icon(
                                    if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    stringResource(if (state.playing) R.string.player_pause else R.string.player_play),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    IconButton(onClick = music::next, enabled = state.hasNext) { Icon(Icons.Rounded.SkipNext, stringResource(R.string.player_next), tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}
