package com.pixelgram.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import org.drinkless.tdlib.TdApi
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCalendar(chatId: Long, onDismiss: () -> Unit, onDay: (LocalDate) -> Unit) {
    val graph = LocalGraph.current
    val zone = ZoneId.systemDefault()
    val today = remember { LocalDate.now(zone) }
    val snapshot by graph.store.snapshot.collectAsState()
    val days by produceState(emptyMap<LocalDate, TdApi.Message>(), chatId) {
        val found = HashMap<LocalDate, TdApi.Message>()
        var from = 0L
        repeat(MaxPages) {
            val calendar = runCatching { graph.td.send(TdApi.GetChatMessageCalendar(chatId, null, TdApi.SearchMessagesFilterPhotoAndVideo(), from)) }.getOrNull() ?: return@produceState
            if (calendar.days.isEmpty()) return@produceState
            var oldest = Long.MAX_VALUE
            calendar.days.forEach { day ->
                val date = Instant.ofEpochSecond(day.message.date.toLong()).atZone(zone).toLocalDate()
                found.putIfAbsent(date, day.message)
                oldest = minOf(oldest, day.message.id)
            }
            value = HashMap(found)
            if (oldest == Long.MAX_VALUE || oldest == from) return@produceState
            from = oldest
        }
    }
    val first = days.keys.minOrNull()?.let { YearMonth.from(it) } ?: YearMonth.from(today).minusMonths(5)
    val months = remember(first) {
        val list = ArrayList<YearMonth>()
        var current = first
        val last = YearMonth.from(today)
        while (!current.isAfter(last)) {
            list += current
            current = current.plusMonths(1)
        }
        list
    }
    val state = rememberLazyListState()
    LaunchedEffect(months.size) { if (months.isNotEmpty()) state.scrollToItem(months.lastIndex) }
    val firstDay = DayOfWeek.MONDAY
    val weekdays = remember { (0 until 7).map { firstDay.plus(it.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()) } }
    val monthFormat = remember { DateTimeFormatter.ofPattern("LLLL yyyy") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(stringResource(R.string.calendar_title)) },
                        navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                        weekdays.forEach { Text(it.uppercase(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                    }
                    HorizontalDivider()
                }
            },
        ) { padding ->
            LazyColumn(state = state, contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize().padding(padding)) {
                items(months, key = { it.toString() }) { month ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text(
                            month.format(monthFormat).replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        )
                        val offset = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
                        val cells = offset + month.lengthOfMonth()
                        val rows = (cells + 6) / 7
                        repeat(rows) { row ->
                            Row(Modifier.fillMaxWidth()) {
                                repeat(7) { col ->
                                    val dayNumber = row * 7 + col - offset + 1
                                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                                        if (dayNumber in 1..month.lengthOfMonth()) {
                                            val date = month.atDay(dayNumber)
                                            val message = days[date]
                                            val future = date.isAfter(today)
                                            Box(
                                                Modifier.fillMaxSize().clip(CircleShape).clickable(enabled = !future) { onDay(date) },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (message != null) {
                                                    DayThumb(message, snapshot.files) { graph.store.requestFileAsync(it) }
                                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                                }
                                                Text(
                                                    dayNumber.toString(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (message != null) FontWeight.Bold else null,
                                                    color = when {
                                                        message != null -> Color.White
                                                        future -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayThumb(message: TdApi.Message, files: Map<Int, String>, onNeedFile: (Int) -> Unit) {
    val (mini, file) = when (val content = message.content) {
        is TdApi.MessagePhoto -> content.photo.minithumbnail?.data to content.photo.sizes.firstOrNull { it.type == "m" || it.type == "s" }?.photo
        is TdApi.MessageVideo -> content.video.minithumbnail?.data to content.video.thumbnail?.file
        else -> null to null
    }
    val path = file?.let { f -> files[f.id] ?: f.local.path.takeIf { f.local.isDownloadingCompleted && it.isNotEmpty() } }
    if (file != null && path == null) LaunchedEffect(file.id) { onNeedFile(file.id) }
    AsyncImage(path?.let(::File) ?: mini, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
}

private const val MaxPages = 40
