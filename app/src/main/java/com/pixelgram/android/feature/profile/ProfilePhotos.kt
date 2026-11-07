package com.pixelgram.android.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.util.fullDate
import org.drinkless.tdlib.TdApi

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfilePhotosScreen(graph: AppGraph, userId: Long, navigator: Navigator) {
    val snapshot by graph.store.snapshot.collectAsState()
    val photos by produceState<List<TdApi.ChatPhoto>?>(null, userId) {
        value = runCatching { graph.td.send(TdApi.GetUserProfilePhotos(userId, 0, 100)).photos.toList() }.getOrDefault(emptyList())
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val list = photos
        when {
            list == null -> LoadingIndicator(Modifier.align(Alignment.Center))
            list.isEmpty() -> Text(stringResource(R.string.profile_no_photos), color = Color.White, modifier = Modifier.align(Alignment.Center))
            else -> {
                val pager = rememberPagerState { list.size }
                HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                    val photo = list[page]
                    val best = photo.sizes.maxByOrNull { it.width * it.height }
                    val fileId = best?.photo?.id ?: 0
                    LaunchedEffect(fileId) { if (fileId != 0) graph.store.requestFileAsync(fileId) }
                    val path = snapshot.files[fileId]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = path?.let { java.io.File(it) } ?: photo.minithumbnail?.data,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (path == null) LoadingIndicator()
                    }
                }
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = Color.White) }
                    Text(
                        "${pager.currentPage + 1} / ${list.size} · ${fullDate(list[pager.currentPage].addedDate)}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
