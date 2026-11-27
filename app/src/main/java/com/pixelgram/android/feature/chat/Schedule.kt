package com.pixelgram.android.feature.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.flow.distinctUntilChanged
import org.drinkless.tdlib.TdApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun WheelPicker(items: List<String>, initial: Int, modifier: Modifier, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState(initial)
    val fling = rememberSnapFlingBehavior(state)
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.div(2) ?: 0) 1 else 0 }
            .distinctUntilChanged()
            .collect { onSelect(it.coerceIn(0, items.lastIndex)) }
    }
    val itemPx = with(androidx.compose.ui.platform.LocalDensity.current) { WheelItem.toPx() }
    val center by remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / itemPx } }
    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = WheelItem * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(WheelItem * 5),
    ) {
        items(items.size) { index ->
            val delta = index - center
            val distance = kotlin.math.abs(delta).toInt()
            Box(Modifier.height(WheelItem).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    items[index],
                    fontSize = if (distance == 0) 22.sp else 19.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = when (distance) { 0 -> 1f; 1 -> 0.6f; else -> 0.3f }),
                    maxLines = 1,
                    modifier = Modifier
                        .graphicsLayer { alpha = 1f - ((-delta) - 1f).coerceIn(0f, 1f).let { it * it } }
                        .blur(((-delta) - 1f).coerceIn(0f, 1f) * 10.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSheet(reminder: Boolean, onlineName: String?, initial: Int? = null, onDismiss: () -> Unit, onPick: (TdApi.MessageSchedulingState) -> Unit) {
    val zone = ZoneId.systemDefault()
    val start = remember { initial?.let { java.time.Instant.ofEpochSecond(it.toLong()).atZone(zone).toLocalDateTime() } ?: LocalDateTime.now(zone).plusMinutes(10) }
    val today = remember { LocalDate.now(zone) }
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val todayLabel = stringResource(R.string.schedule_today)
    val days = remember { (0 until 366).map { if (it == 0) todayLabel else today.plusDays(it.toLong()).format(dayFormat) } }
    var day by remember { mutableStateOf(java.time.temporal.ChronoUnit.DAYS.between(today, start.toLocalDate()).toInt().coerceIn(0, 365)) }
    var hour by remember { mutableStateOf(start.hour) }
    var minute by remember { mutableStateOf(start.minute) }
    var menu by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(0) }
    var repeatMenu by remember { mutableStateOf(false) }
    val graph = LocalGraph.current
    val premium = graph.store.snapshot.value.let { it.users[it.meId]?.premium } == true
    val context = LocalContext.current
    val premiumOnly = stringResource(R.string.schedule_repeat_premium)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chosen = today.plusDays(day.toLong()).atTime(hour, minute)
    val epoch = chosen.atZone(zone).toEpochSecond().toInt()
    val time = "%02d:%02d".format(hour, minute)
    val label = when {
        day == 0 -> stringResource(if (reminder) R.string.schedule_remind_today else R.string.schedule_send_today, time)
        else -> stringResource(if (reminder) R.string.schedule_remind_on else R.string.schedule_send_on, chosen.format(DateTimeFormatter.ofPattern("MMM d")), time)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp)) {
                Text(stringResource(if (reminder) R.string.schedule_reminder_title else R.string.schedule_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (reminder) {
                    Icon(Icons.Rounded.NotificationsNone, null, Modifier.padding(12.dp))
                } else if (onlineName != null) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.schedule_when_online_named, onlineName)) },
                                onClick = {
                                    menu = false
                                    onPick(TdApi.MessageSchedulingStateSendWhenOnline())
                                },
                            )
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxWidth().height(WheelItem).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .graphicsLayer(),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp, modifier = Modifier.align(Alignment.TopCenter))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp, modifier = Modifier.align(Alignment.BottomCenter))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    WheelPicker(days, day, Modifier.weight(1.6f)) { day = it }
                    WheelPicker((0..23).map { "%02d".format(it) }, hour, Modifier.weight(1f)) { hour = it }
                    WheelPicker((0..59).map { "%02d".format(it) }, minute, Modifier.weight(1f)) { minute = it }
                }
            }
            Box(Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = { if (premium) repeatMenu = true else android.widget.Toast.makeText(context, premiumOnly, android.widget.Toast.LENGTH_SHORT).show() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(stringResource(R.string.schedule_repeat) + ": ", style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(RepeatOptions.first { it.first == repeat }.second), style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        if (!premium) Icon(Icons.Rounded.Lock, null, Modifier.padding(start = 6.dp).size(16.dp))
                    }
                }
                DropdownMenu(expanded = repeatMenu, onDismissRequest = { repeatMenu = false }, shape = RoundedCornerShape(16.dp)) {
                    RepeatOptions.forEach { (seconds, label) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { repeat = seconds; repeatMenu = false })
                    }
                }
            }
            Button(
                onClick = {
                    val now = (System.currentTimeMillis() / 1000).toInt()
                    onPick(TdApi.MessageSchedulingStateSendAtDate(maxOf(epoch, now + 30), repeat))
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp),
            ) { Text(label, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

private object EffectEmoji {
    val cache = java.util.concurrent.ConcurrentHashMap<Long, String>()
}

@Composable
fun SendOptionsPopup(
    preview: String,
    effects: Boolean,
    canWhenOnline: Boolean,
    onDismiss: () -> Unit,
    onSend: (TdApi.MessageSendOptions) -> Unit,
    onSchedule: () -> Unit,
) {
    val graph = LocalGraph.current
    var effect by remember { mutableStateOf(0L) }
    val available by produceState(emptyList<Pair<Long, String>>(), effects) {
        if (!effects) return@produceState
        value = com.pixelgram.android.td.MessageEffects.ids.take(24).mapNotNull { id ->
            val emoji = EffectEmoji.cache[id] ?: runCatching { graph.td.send(TdApi.GetMessageEffect(id)).emoji }.getOrNull()?.also { EffectEmoji.cache[id] = it }
            emoji?.let { id to it }
        }
    }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 500f)) }
    Popup(onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f * appear.value)).clickable(interactionSource = null, indication = null, onClick = onDismiss)) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp).padding(bottom = 64.dp).graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 40.dp.toPx()
                },
            ) {
                if (available.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.widthIn(max = 340.dp)) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(stringResource(R.string.send_effect_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                items(available, key = { it.first }) { (id, emoji) ->
                                    Box(
                                        Modifier.size(44.dp).clip(CircleShape)
                                            .background(if (effect == id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .clickable { effect = if (effect == id) 0L else id },
                                        contentAlignment = Alignment.Center,
                                    ) { Text(emoji, fontSize = 26.sp) }
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.widthIn(max = 260.dp)) {
                        Text(preview, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                    }
                    Surface(
                        onClick = { onSend(TdApi.MessageSendOptions().apply { effectId = effect }) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp),
                    ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.action_send)) } }
                }
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp, modifier = Modifier.width(260.dp)) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        SendOptionRow(Icons.Rounded.CalendarMonth, stringResource(R.string.send_schedule), onClick = onSchedule)
                        if (canWhenOnline) SendOptionRow(Icons.Rounded.AccountCircle, stringResource(R.string.send_when_online)) {
                            onSend(TdApi.MessageSendOptions().apply { effectId = effect; schedulingState = TdApi.MessageSchedulingStateSendWhenOnline() })
                        }
                        SendOptionRow(Icons.Rounded.NotificationsOff, stringResource(R.string.send_silent)) {
                            onSend(TdApi.MessageSendOptions().apply { effectId = effect; disableNotification = true })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SendOptionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icon, null, tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessagesScreen(controller: ChatController, onDismiss: () -> Unit) {
    val messages by controller.scheduled.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    LaunchedEffect(Unit) { controller.loadScheduled() }
    var menuFor by remember { mutableStateOf<TdApi.Message?>(null) }
    var rescheduling by remember { mutableStateOf<TdApi.Message?>(null) }
    var editing by remember { mutableStateOf<TdApi.Message?>(null) }
    val zone = ZoneId.systemDefault()
    val headerFormat = remember { DateTimeFormatter.ofPattern("MMMM d") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.scheduled_title)) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
                )
            },
        ) { padding ->
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.scheduled_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Scaffold
            }
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), reverseLayout = false) {
                var lastDay: LocalDate? = null
                messages.forEach { message ->
                    val date = (message.schedulingState as? TdApi.MessageSchedulingStateSendAtDate)?.sendDate
                    val at = date?.let { java.time.Instant.ofEpochSecond(it.toLong()).atZone(zone) }
                    val dayKey = at?.toLocalDate()
                    if (dayKey != lastDay || (at == null && lastDay == null)) {
                        lastDay = dayKey
                        item(key = "h${message.id}") {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                    Text(
                                        if (at != null) stringResource(R.string.scheduled_for, at.format(headerFormat)) else stringResource(R.string.scheduled_when_online),
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    item(key = message.id) {
                        val text = (message.content as? TdApi.MessageText)?.text?.text ?: com.pixelgram.android.td.contentPreviewLabel(message.content)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Surface(
                                onClick = { menuFor = message },
                                shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.widthIn(max = 300.dp),
                            ) {
                                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
                                    Text(text, style = MaterialTheme.typography.bodyLarge)
                                    Text(at?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                            }
                            DropdownMenu(expanded = menuFor?.id == message.id, onDismissRequest = { menuFor = null }, shape = RoundedCornerShape(16.dp)) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.scheduled_send_now)) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Send, null) }, onClick = {
                                    menuFor = null
                                    controller.reschedule(message.id, null)
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.action_copy)) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = {
                                    menuFor = null
                                    clipboard.setText(AnnotatedString(text))
                                })
                                if (message.content is TdApi.MessageText) DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = {
                                    menuFor = null
                                    editing = message
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.scheduled_reschedule)) }, leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null) }, onClick = {
                                    menuFor = null
                                    rescheduling = message
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = {
                                    menuFor = null
                                    controller.deleteScheduled(message.id)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
    rescheduling?.let { message ->
        ScheduleSheet(
            reminder = false,
            onlineName = null,
            initial = (message.schedulingState as? TdApi.MessageSchedulingStateSendAtDate)?.sendDate,
            onDismiss = { rescheduling = null },
        ) { state ->
            rescheduling = null
            controller.reschedule(message.id, state)
        }
    }
    editing?.let { message ->
        var value by remember(message.id) { mutableStateOf((message.content as? TdApi.MessageText)?.text?.text.orEmpty()) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.action_edit)) },
            text = { OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { controller.editScheduledText(message.id, value); editing = null }) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private val WheelItem = 48.dp

private val RepeatOptions = listOf(
    0 to R.string.repeat_never,
    86_400 to R.string.repeat_daily,
    604_800 to R.string.repeat_weekly,
    1_209_600 to R.string.repeat_biweekly,
    2_592_000 to R.string.repeat_monthly,
    7_776_000 to R.string.repeat_quarterly,
    15_552_000 to R.string.repeat_half_year,
    31_536_000 to R.string.repeat_yearly,
)
