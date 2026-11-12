package com.pixelgram.android.feature.premium

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.Notice
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi

internal val BusinessFeatures = listOf(
    Feature(Icons.Rounded.LocationOn, R.string.business_location, R.string.business_location_d, "location"),
    Feature(Icons.Rounded.Schedule, R.string.business_hours, R.string.business_hours_d, "hours"),
    Feature(Icons.AutoMirrored.Rounded.Reply, R.string.business_quick, R.string.business_quick_d, "quick"),
    Feature(Icons.Rounded.WavingHand, R.string.business_greeting, R.string.business_greeting_d, "greeting"),
    Feature(Icons.Rounded.Bedtime, R.string.business_away, R.string.business_away_d, "away"),
    Feature(Icons.Rounded.Link, R.string.business_links, R.string.business_links_d, "links"),
    Feature(Icons.Rounded.Info, R.string.business_start, R.string.business_start_d, "start"),
    Feature(Icons.Rounded.SmartToy, R.string.business_bots, R.string.business_bots_d, "bots"),
)

private const val GreetingShortcut = "hello"
private const val AwayShortcut = "away"

private suspend fun businessInfo(graph: AppGraph): TdApi.BusinessInfo? =
    runCatching { graph.td.send(TdApi.GetUserFullInfo(graph.store.snapshot.value.meId)).businessInfo }.getOrNull()

private suspend fun shortcuts(graph: AppGraph): Map<String, TdApi.QuickReplyShortcut> = coroutineScope {
    val found = mutableMapOf<Int, TdApi.QuickReplyShortcut>()
    var ids: IntArray? = null
    val collector = launch {
        graph.td.updates.collect { u ->
            when (u) {
                is TdApi.UpdateQuickReplyShortcut -> found[u.shortcut.id] = u.shortcut
                is TdApi.UpdateQuickReplyShortcuts -> ids = u.shortcutIds
            }
        }
    }
    delay(50)
    runCatching { graph.td.send(TdApi.LoadQuickReplyShortcuts()) }
    withTimeoutOrNull(3_000) {
        while (true) {
            val list = ids
            if (list != null && list.all { it in found }) break
            delay(50)
        }
    }
    collector.cancel()
    val order = ids
    (if (order != null) order.toList().mapNotNull { found[it] } else found.values.toList()).associateBy { it.name }
}

private suspend fun saveShortcut(graph: AppGraph, name: String, text: String): Int? = coroutineScope {
    shortcuts(graph)[name]?.let { runCatching { graph.td.send(TdApi.DeleteQuickReplyShortcut(it.id)) } }
    val waiter = async {
        withTimeoutOrNull(10_000) {
            graph.td.updates.first { it is TdApi.UpdateQuickReplyShortcut && it.shortcut.name == name } as TdApi.UpdateQuickReplyShortcut
        }
    }
    delay(50)
    val sent = runCatching {
        graph.td.send(TdApi.AddQuickReplyShortcutMessage(name, 0, TdApi.InputMessageText(TdApi.FormattedText(text, emptyArray()), null, false)))
    }
    if (sent.isFailure) {
        waiter.cancel()
        null
    } else waiter.await()?.shortcut?.id
}

private fun shortcutText(shortcut: TdApi.QuickReplyShortcut?) = (shortcut?.firstMessage?.content as? TdApi.MessageText)?.text?.text.orEmpty()

private val AllPrivate get() = TdApi.BusinessRecipients(LongArray(0), LongArray(0), true, true, true, true, false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessEditor(graph: AppGraph, key: String, onDismiss: () -> Unit) {
    val feature = BusinessFeatures.firstOrNull { it.key == key } ?: return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientMark(feature.icon, PremiumGradient, 44.dp)
                Column {
                    Text(stringResource(feature.title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(feature.text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (key) {
                "location" -> LocationEditor(graph, onDismiss)
                "hours" -> HoursEditor(graph, onDismiss)
                "quick" -> QuickRepliesEditor(graph)
                "greeting" -> AutoMessageEditor(graph, away = false, onDismiss)
                "away" -> AutoMessageEditor(graph, away = true, onDismiss)
                "links" -> LinksEditor(graph)
                "start" -> StartPageEditor(graph, onDismiss)
                "bots" -> BotsEditor(graph, onDismiss)
            }
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: Int, modifier: Modifier = Modifier.fillMaxWidth(), lines: Int = 1, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = lines == 1,
        minLines = lines,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SaveRow(busy: Boolean, onSave: () -> Unit, onClear: (() -> Unit)? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onClear != null) OutlinedButton(onClick = onClear, enabled = !busy, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.business_remove)) }
        Button(onClick = onSave, enabled = !busy, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).height(52.dp)) {
            if (busy) LoadingIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary) else Text(stringResource(R.string.business_save))
        }
    }
}

@Composable
private fun rememberSaver(graph: AppGraph, onDone: () -> Unit): Pair<Boolean, (suspend () -> Unit) -> Unit> {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    val saved = stringResource(R.string.business_saved)
    return busy to { block ->
        busy = true
        graph.scope.launch {
            val result = runCatching { block() }
            busy = false
            result.onSuccess {
                Notice.show(saved)
                onDone()
            }.onFailure { Notice.show(it.message ?: context.getString(R.string.pay_unavailable)) }
        }
    }
}

@Composable
private fun LocationEditor(graph: AppGraph, onDone: () -> Unit) {
    var address by remember { mutableStateOf("") }
    var had by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { businessInfo(graph)?.location?.let { address = it.address; had = true } }
    val (busy, save) = rememberSaver(graph, onDone)
    Field(address, { address = it }, R.string.business_address, lines = 2)
    SaveRow(
        busy,
        onSave = { save { graph.td.send(TdApi.SetBusinessLocation(if (address.isBlank()) null else TdApi.BusinessLocation(null, address.trim()))) } },
        onClear = if (had) ({ save { graph.td.send(TdApi.SetBusinessLocation(null)) } }) else null,
    )
}

private class Day(var open: Boolean, var from: String, var to: String)

private fun parseTime(text: String): Int? {
    val d = text.filter { it.isDigit() }
    if (d.length != 4) return null
    val h = d.take(2).toInt()
    val m = d.drop(2).toInt()
    return if (h in 0..24 && m in 0..59 && h * 60 + m <= 1440) h * 60 + m else null
}

private fun formatTime(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

private fun timeInput(text: String): String {
    val d = text.filter { it.isDigit() }.take(4)
    return if (d.length > 2) d.take(2) + ":" + d.drop(2) else d
}

@Composable
private fun HoursEditor(graph: AppGraph, onDone: () -> Unit) {
    val days = remember { mutableStateListOf<Day>().apply { repeat(7) { add(Day(it < 5, "09:00", "18:00")) } } }
    var tick by remember { mutableStateOf(0) }
    var had by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        businessInfo(graph)?.openingHours?.let { hours ->
            had = true
            for (i in 0 until 7) days[i] = Day(false, "09:00", "18:00")
            hours.openingHours.forEach { interval ->
                val day = (interval.startMinute / 1440).coerceIn(0, 6)
                days[day] = Day(true, formatTime(interval.startMinute - day * 1440), formatTime((interval.endMinute - day * 1440).coerceAtMost(1440)))
            }
            tick++
        }
    }
    val names = remember { java.text.DateFormatSymbols.getInstance().weekdays }
    val order = intArrayOf(java.util.Calendar.MONDAY, java.util.Calendar.TUESDAY, java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.FRIDAY, java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY)
    val (busy, save) = rememberSaver(graph, onDone)
    val invalid = stringResource(R.string.business_hours_invalid)
    key(tick) {
        days.forEachIndexed { index, day ->
            var open by remember { mutableStateOf(day.open) }
            var from by remember { mutableStateOf(day.from) }
            var to by remember { mutableStateOf(day.to) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(names[order[index]].replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (open) {
                    OutlinedTextField(from, { from = timeInput(it); day.from = from }, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(84.dp))
                    Text("–")
                    OutlinedTextField(to, { to = timeInput(it); day.to = to }, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(84.dp))
                }
                Switch(open, { open = it; day.open = it })
            }
        }
    }
    SaveRow(
        busy,
        onSave = {
            val intervals = days.mapIndexedNotNull { index, day ->
                if (!day.open) return@mapIndexedNotNull null
                val a = parseTime(day.from) ?: return@mapIndexedNotNull TdApi.BusinessOpeningHoursInterval(-1, -1)
                val b = parseTime(day.to) ?: return@mapIndexedNotNull TdApi.BusinessOpeningHoursInterval(-1, -1)
                val end = if (b <= a) b + 1440 else b
                TdApi.BusinessOpeningHoursInterval(index * 1440 + a, index * 1440 + end)
            }
            if (intervals.any { it.startMinute < 0 }) Notice.show(invalid)
            else save {
                graph.td.send(TdApi.SetBusinessOpeningHours(if (intervals.isEmpty()) null else TdApi.BusinessOpeningHours(java.util.TimeZone.getDefault().id, intervals.toTypedArray())))
            }
        },
        onClear = if (had) ({ save { graph.td.send(TdApi.SetBusinessOpeningHours(null)) } }) else null,
    )
}

@Composable
private fun key(value: Int, content: @Composable () -> Unit) = androidx.compose.runtime.key(value) { content() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoMessageEditor(graph: AppGraph, away: Boolean, onDone: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(7) }
    var offlineOnly by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val info = businessInfo(graph)
        val all = shortcuts(graph)
        if (away) {
            info?.awayMessageSettings?.let { enabled = true; offlineOnly = it.offlineOnly }
            text = shortcutText(all[AwayShortcut])
        } else {
            info?.greetingMessageSettings?.let { enabled = true; days = it.inactivityDays }
            text = shortcutText(all[GreetingShortcut])
        }
    }
    val (busy, save) = rememberSaver(graph, onDone)
    Field(text, { text = it }, if (away) R.string.business_away_text else R.string.business_greeting_text, lines = 3)
    if (away) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.business_away_offline), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(offlineOnly, { offlineOnly = it })
        }
    } else {
        Text(stringResource(R.string.business_greeting_days), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 14, 21, 28).forEach { d -> FilterChip(selected = days == d, onClick = { days = d }, label = { Text(stringResource(R.string.business_days, d)) }) }
        }
    }
    SaveRow(
        busy,
        onSave = {
            if (text.isNotBlank()) save {
                val id = saveShortcut(graph, if (away) AwayShortcut else GreetingShortcut, text.trim()) ?: error("shortcut")
                if (away) graph.td.send(TdApi.SetBusinessAwayMessageSettings(TdApi.BusinessAwayMessageSettings(id, AllPrivate, TdApi.BusinessAwayMessageScheduleAlways(), offlineOnly)))
                else graph.td.send(TdApi.SetBusinessGreetingMessageSettings(TdApi.BusinessGreetingMessageSettings(id, AllPrivate, days)))
            }
        },
        onClear = if (enabled) ({
            save {
                if (away) graph.td.send(TdApi.SetBusinessAwayMessageSettings(null))
                else graph.td.send(TdApi.SetBusinessGreetingMessageSettings(null))
            }
        }) else null,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuickRepliesEditor(graph: AppGraph) {
    var list by remember { mutableStateOf<List<TdApi.QuickReplyShortcut>?>(null) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) { list = shortcuts(graph).values.filter { it.name != GreetingShortcut && it.name != AwayShortcut } }
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    val (busy, save) = rememberSaver(graph) {
        name = ""
        text = ""
        reload++
    }
    val current = list
    if (current == null) LoadingIndicator(Modifier.align())
    current?.forEach { shortcut ->
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("/" + shortcut.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(shortcutText(shortcut), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { save { graph.td.send(TdApi.DeleteQuickReplyShortcut(shortcut.id)) } }) { Icon(Icons.Rounded.Delete, stringResource(R.string.business_remove)) }
            }
        }
    }
    Field(name, { name = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(32) }, R.string.business_quick_name)
    Field(text, { text = it }, R.string.business_quick_text, lines = 2)
    SaveRow(busy, onSave = {
        if (name.isNotBlank() && text.isNotBlank()) save {
            if (saveShortcut(graph, name, text.trim()) == null) error("shortcut")
        }
    })
}

@Composable
private fun Modifier.align() = this.padding(16.dp)

@Composable
private fun LinksEditor(graph: AppGraph) {
    val clipboard = LocalClipboardManager.current
    val copied = stringResource(R.string.business_link_copied)
    var links by remember { mutableStateOf<List<TdApi.BusinessChatLink>?>(null) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) { links = runCatching { graph.td.send(TdApi.GetBusinessChatLinks()).links.toList() }.getOrNull() ?: emptyList() }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    val (busy, save) = rememberSaver(graph) {
        title = ""
        text = ""
        reload++
    }
    links?.forEach { link ->
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.clickable {
            clipboard.setText(AnnotatedString(link.link))
            Notice.show(copied)
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(link.title.ifBlank { link.link }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(link.text.text.ifBlank { link.link }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.business_link_views, link.viewCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { save { graph.td.send(TdApi.DeleteBusinessChatLink(link.link)) } }) { Icon(Icons.Rounded.Delete, stringResource(R.string.business_remove)) }
            }
        }
    }
    Field(title, { title = it.take(32) }, R.string.business_link_title)
    Field(text, { text = it }, R.string.business_link_text, lines = 2)
    SaveRow(busy, onSave = { save { graph.td.send(TdApi.CreateBusinessChatLink(TdApi.InputBusinessChatLink(TdApi.FormattedText(text.trim(), emptyArray()), title.trim()))) } })
}

@Composable
private fun StartPageEditor(graph: AppGraph, onDone: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var had by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { businessInfo(graph)?.startPage?.let { title = it.title; message = it.message; had = true } }
    val (busy, save) = rememberSaver(graph, onDone)
    Field(title, { title = it.take(32) }, R.string.business_start_title)
    Field(message, { message = it.take(70) }, R.string.business_start_message, lines = 2)
    SaveRow(
        busy,
        onSave = { save { graph.td.send(TdApi.SetBusinessStartPage(if (title.isBlank() && message.isBlank()) null else TdApi.InputBusinessStartPage(title.trim(), message.trim(), null))) } },
        onClear = if (had) ({ save { graph.td.send(TdApi.SetBusinessStartPage(null)) } }) else null,
    )
}

@Composable
private fun BotsEditor(graph: AppGraph, onDone: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<TdApi.BusinessConnectedBot?>(null) }
    var canReply by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        current = runCatching { graph.td.send(TdApi.GetBusinessConnectedBot()).bot }.getOrNull()
        current?.let { bot ->
            canReply = bot.rights.canReply
            runCatching { graph.td.send(TdApi.GetUser(bot.botUserId)) }.getOrNull()?.usernames?.activeUsernames?.firstOrNull()?.let { username = it }
        }
    }
    val (busy, save) = rememberSaver(graph, onDone)
    Field(username, { username = it.removePrefix("@").filter { c -> c.isLetterOrDigit() || c == '_' } }, R.string.business_bot_username)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.business_bot_reply), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(canReply, { canReply = it })
    }
    SaveRow(
        busy,
        onSave = {
            if (username.isNotBlank()) save {
                val chat = graph.td.send(TdApi.SearchPublicChat(username))
                val botId = (chat.type as? TdApi.ChatTypePrivate)?.userId ?: error("bot")
                val rights = TdApi.BusinessBotRights(canReply, true, canReply, false, false, false, false, false, false, false, false, false, false, false)
                graph.td.send(TdApi.SetBusinessConnectedBot(TdApi.BusinessConnectedBot(botId, AllPrivate, rights)))
            }
        },
        onClear = current?.let { bot -> { save { graph.td.send(TdApi.DeleteBusinessConnectedBot(bot.botUserId)) } } },
    )
}
