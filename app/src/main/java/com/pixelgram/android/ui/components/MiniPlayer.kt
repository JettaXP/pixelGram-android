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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
            ElevatedCard(onClick = { com.pixelgram.android.feature.player.FullPlayer.open = true }, shape = RoundedCornerShape(28.dp)) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    leadingContent = {
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
                    },
                    headlineContent = { Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = if (track.performer.isNotBlank()) {
                        { Text(track.performer, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    } else {
                        null
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = music::previous) { Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous)) }
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                if (state.loading) {
                                    LoadingIndicator(Modifier.size(48.dp))
                                } else {
                                    val progress = if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f
                                    val amplitude by androidx.compose.animation.core.animateFloatAsState(if (state.playing) 1f else 0f, MaterialTheme.motionScheme.slowEffectsSpec(), label = "wave")
                                    CircularWavyProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(48.dp),
                                        amplitude = { amplitude },
                                        waveSpeed = if (state.playing) androidx.compose.material3.WavyProgressIndicatorDefaults.CircularWavelength else 0.dp,
                                    )
                                    FilledTonalIconButton(onClick = music::toggle, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(38.dp)) {
                                        Icon(
                                            if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            stringResource(if (state.playing) R.string.player_pause else R.string.player_play),
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = music::next, enabled = state.hasNext) { Icon(Icons.Rounded.SkipNext, stringResource(R.string.player_next)) }
                        }
                    },
                )
            }
        }
    }
}
