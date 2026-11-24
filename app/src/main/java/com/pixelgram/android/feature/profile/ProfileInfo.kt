package com.pixelgram.android.feature.profile
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.td.UserInfo
import com.pixelgram.android.ui.components.ExpressiveMenu
import com.pixelgram.android.ui.components.HiddenPhone
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.MenuEntry
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.segmentPosition
import com.pixelgram.android.util.formatPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi

object RegDates {
    private var table: List<Pair<Long, Long>>? = null

    private fun load(context: Context): List<Pair<Long, Long>> = table ?: runCatching {
        context.assets.open("registration_dates.txt").bufferedReader().readLines().mapNotNull { line ->
            val parts = line.trim().split(' ')
            if (parts.size == 2) parts[0].toLong() to parts[1].toLong() else null
        }
    }.getOrDefault(emptyList()).also { table = it }

    enum class Kind { Earlier, Approximately, Later }

    fun estimate(context: Context, userId: Long, earliestPhoto: Int): Pair<Long, Kind>? {
        val rows = load(context)
        if (rows.isEmpty()) return null
        val (date, kind) = when {
            userId <= rows.first().first -> rows.first().second to Kind.Earlier
            userId >= rows.last().first -> rows.last().second to Kind.Later
            else -> {
                val upper = rows.indexOfFirst { it.first >= userId }
                val (hiId, hiDate) = rows[upper]
                val (loId, loDate) = rows[upper - 1]
                val ratio = (userId - loId).toDouble() / (hiId - loId).coerceAtLeast(1)
                (loDate + (hiDate - loDate) * ratio).toLong() to Kind.Approximately
            }
        }
        val photo = earliestPhoto.toLong().takeIf { it > 0 }
        return if (photo != null && photo < date) photo to Kind.Approximately else date to kind
    }
}

object DataCenters {
    fun location(dc: Int): String? = when (dc) {
        1, 3 -> "Miami FL, USA"
        2, 4 -> "Amsterdam, NL"
        5 -> "Singapore, SG"
        else -> null
    }

    fun fromRemoteId(remote: String?): Int {
        if (remote.isNullOrBlank()) return 0
        val raw = runCatching { android.util.Base64.decode(remote, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING) }.getOrNull() ?: return 0
        val bytes = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < raw.size) {
            val b = raw[i]
            if (b.toInt() == 0 && i + 1 < raw.size) {
                repeat(raw[i + 1].toInt() and 0xFF) { bytes.write(0) }
                i += 2
            } else {
                bytes.write(b.toInt())
                i++
            }
        }
        val data = bytes.toByteArray()
        if (data.size < 8) return 0
        val buffer = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.int
        return buffer.int.takeIf { it in 1..5 } ?: 0
    }
}

private fun copyText(context: Context, value: String, toast: String) {
    context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText(null, value))
    android.widget.Toast.makeText(context, toast, android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
private fun MenuRow(
    menu: List<MenuEntry>,
    onClick: (() -> Unit)? = null,
    content: @Composable (onClick: () -> Unit, onLongClick: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Box {
        content(
            { if (onClick != null) onClick() else open = true },
            {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                open = true
            },
        )
        Box(Modifier.align(Alignment.BottomEnd)) { ExpressiveMenu(open, { open = false }, listOf(menu)) }
    }
}

@Composable
fun UserInfoRows(
    graph: AppGraph,
    userId: Long,
    user: UserInfo?,
    full: TdApi.UserFullInfo?,
    self: Boolean,
    names: List<String>,
    phoneDetails: String?,
    hidePhone: Boolean,
    bioText: androidx.compose.ui.text.AnnotatedString?,
    onUsername: (String) -> Unit,
    onQr: () -> Unit,
    onNoteChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.profile_copied)
    val copy: (String) -> Unit = { copyText(context, it, copied) }
    val bio = full?.bio?.text.orEmpty()
    val phone = user?.phone.orEmpty()
    val birthday = full?.birthdate
    val note = full?.note?.text.orEmpty()
    var editingNote by remember { mutableStateOf(false) }
    var translated by remember { mutableStateOf<String?>(null) }
    val dc by produceState(0, userId) {
        value = runCatching { DataCenters.fromRemoteId(graph.td.send(TdApi.GetUser(userId)).profilePhoto?.small?.remote?.id) }.getOrDefault(0)
    }
    val rows = buildList {
        if (bio.isNotBlank()) add("bio")
        if (names.isNotEmpty()) add("user")
        if (phone.isNotBlank()) add("phone")
        add("id")
        if (birthday != null) add("birthday")
        if (note.isNotBlank()) add("note")
        if (full?.businessInfo?.location != null) add("location")
        if (full?.businessInfo?.openingHours?.openingHours?.isNotEmpty() == true) add("hours")
    }
    SettingsGroup {
        rows.forEachIndexed { index, row ->
            val position = segmentPosition(index, rows.size)
            when (row) {
                "bio" -> MenuRow(
                    listOf(
                        MenuEntry(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy)) { copy(bio) },
                        MenuEntry(Icons.Rounded.Translate, stringResource(R.string.action_translate)) {
                            scope.launch {
                                val lang = java.util.Locale.getDefault().language
                                translated = runCatching { graph.td.send(TdApi.TranslateText(TdApi.FormattedText(bio, emptyArray()), lang, "")).text }.getOrNull() ?: bio
                            }
                        },
                    ),
                ) { click, long ->
                    SettingsItem(
                        bio,
                        position,
                        stringResource(R.string.profile_bio),
                        Icons.Rounded.Info,
                        pill = true,
                        onClick = click,
                        onLongClick = long,
                        headlineContent = bioText?.let { { Text(it, color = MaterialTheme.colorScheme.onSurface) } },
                    )
                }
                "user" -> {
                    val main = names.first()
                    MenuRow(
                        listOf(
                            MenuEntry(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy)) { copy("@$main") },
                            MenuEntry(Icons.Rounded.Link, stringResource(R.string.profile_copy_link)) { copy("https://t.me/$main") },
                            MenuEntry(Icons.Rounded.QrCode2, stringResource(R.string.profile_qr)) { onQr() },
                        ),
                        onClick = { onUsername(main) },
                    ) { click, long ->
                        SettingsItem(
                            "@$main",
                            position,
                            stringResource(R.string.profile_username),
                            Icons.Rounded.AlternateEmail,
                            pill = true,
                            onClick = click,
                            onLongClick = long,
                            headlineContent = {
                                Column {
                                    Text("@$main", color = MaterialTheme.colorScheme.onSurface)
                                    val extra = names.drop(1)
                                    if (extra.isNotEmpty()) {
                                        val muted = MaterialTheme.colorScheme.onSurfaceVariant
                                        val accent = MaterialTheme.colorScheme.primary
                                        val also = stringResource(R.string.profile_also)
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(SpanStyle(color = muted)) { append("$also ") }
                                                extra.forEachIndexed { i, other ->
                                                    if (i > 0) withStyle(SpanStyle(color = muted)) { append(", ") }
                                                    withLink(androidx.compose.ui.text.LinkAnnotation.Clickable(other) { onUsername(other) }) {
                                                        withStyle(SpanStyle(color = accent)) { append("@$other") }
                                                    }
                                                }
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                            },
                            trailing = { IconButton(onClick = onQr) { Icon(Icons.Rounded.QrCode2, stringResource(R.string.profile_qr)) } },
                        )
                    }
                }
                "phone" -> MenuRow(
                    listOf(
                        MenuEntry(Icons.Rounded.Call, stringResource(R.string.profile_call)) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:+$phone"))) }
                        },
                        MenuEntry(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.profile_open_in)) {
                            runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("tel:+$phone")), null)) }
                        },
                        MenuEntry(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy)) { copy("+$phone") },
                    ),
                ) { click, long ->
                    SettingsItem(
                        "+$phone",
                        position,
                        subtitle = listOfNotNull(stringResource(R.string.profile_phone), phoneDetails?.takeIf { it.isNotBlank() }).joinToString(" · "),
                        icon = Icons.Rounded.Phone,
                        pill = true,
                        onClick = click,
                        onLongClick = long,
                        headlineContent = { HiddenPhone("+" + formatPhone(phone), hidden = hidePhone && self) },
                    )
                }
                "id" -> {
                    val haptic = LocalHapticFeedback.current
                    var tip by remember { mutableStateOf(false) }
                    Box {
                        SettingsItem(
                            userId.toString(),
                            position,
                            subtitle = DataCenters.location(dc)?.let { "DC$dc, $it" } ?: "ID",
                            icon = Icons.Rounded.Tag,
                            pill = true,
                            onClick = { copy(userId.toString()) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                copy("tg://user?id=$userId")
                            },
                            trailing = {
                                Box {
                                    IconButton(onClick = { tip = !tip }) { Icon(Icons.Rounded.EventNote, stringResource(R.string.profile_regdate)) }
                                    if (tip) RegDateTip(graph, userId, user, self) { tip = false }
                                }
                            },
                        )
                    }
                }
                "birthday" -> MenuRow(
                    listOf(MenuEntry(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy)) { copy(birthdayText(context, birthday!!)) }),
                ) { click, long ->
                    SettingsItem(birthdayText(context, birthday!!), position, stringResource(R.string.profile_birthday), Icons.Rounded.Cake, pill = true, onClick = click, onLongClick = long)
                }
                "location" -> {
                    val place = full!!.businessInfo!!.location!!
                    MenuRow(
                        listOf(MenuEntry(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy)) { copy(place.address) }),
                        onClick = {
                            val point = place.location
                            val uri = if (point != null) "geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(${android.net.Uri.encode(place.address)})" else "geo:0,0?q=${android.net.Uri.encode(place.address)}"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        },
                    ) { click, long ->
                        SettingsItem(place.address, position, stringResource(R.string.profile_location), androidx.compose.material.icons.Icons.Rounded.LocationOn, pill = true, onClick = click, onLongClick = long)
                    }
                }
                "hours" -> BusinessHoursRow(full!!.businessInfo!!.openingHours!!, position)
                "note" -> MenuRow(
                    listOf(
                        MenuEntry(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy)) { copy(note) },
                        MenuEntry(Icons.Rounded.Edit, stringResource(R.string.action_edit)) { editingNote = true },
                        MenuEntry(Icons.Rounded.Delete, stringResource(R.string.action_delete), error = true) {
                            scope.launch {
                                runCatching { graph.td.send(TdApi.SetUserNote(userId, TdApi.FormattedText("", emptyArray()))) }
                                onNoteChanged()
                            }
                        },
                    ),
                ) { click, long ->
                    SettingsItem(
                        note,
                        position,
                        stringResource(R.string.profile_notes),
                        Icons.AutoMirrored.Rounded.Notes,
                        pill = true,
                        onClick = click,
                        onLongClick = long,
                        trailing = { Text(stringResource(R.string.profile_notes_private), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp)) },
                    )
                }
            }
        }
    }
    if (editingNote) NoteDialog(note, onDismiss = { editingNote = false }) { value ->
        editingNote = false
        scope.launch {
            runCatching { graph.td.send(TdApi.SetUserNote(userId, TdApi.FormattedText(value.trim(), emptyArray()))) }
            onNoteChanged()
        }
    }
    translated?.let { text ->
        AlertDialog(
            onDismissRequest = { translated = null },
            title = { Text(stringResource(R.string.action_translate)) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { copy(text); translated = null }) { Text(stringResource(R.string.action_copy)) } },
            dismissButton = { TextButton(onClick = { translated = null }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

@Composable
fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_notes)) },
        text = {
            OutlinedTextField(
                value,
                { value = it.take(128) },
                supportingText = { Text(stringResource(R.string.profile_notes_hint)) },
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RegDateTip(graph: AppGraph, userId: Long, user: UserInfo?, self: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val estimate by produceState<Pair<Long, RegDates.Kind>?>(null, userId) {
        val earliest = kotlinx.coroutines.withTimeoutOrNull(1500) {
            runCatching { graph.td.send(TdApi.GetUserProfilePhotos(userId, 0, 100)).photos.minOfOrNull { it.addedDate } ?: 0 }.getOrDefault(0)
        } ?: 0
        value = withContext(Dispatchers.IO) { RegDates.estimate(context, userId, earliest) }
    }
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    val close = { closing = true }
    LaunchedEffect(estimate) {
        if (estimate == null) return@LaunchedEffect
        appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 500f))
        delay(TipShowMs)
        closing = true
    }
    LaunchedEffect(closing) {
        if (!closing) return@LaunchedEffect
        appear.animateTo(0f, androidx.compose.animation.core.tween(160))
        onDismiss()
    }
    val result = estimate ?: return
    val month = monthIn(result.first)
    val name = user?.firstName.orEmpty()
    val text = when {
        self -> stringResource(
            when (result.second) {
                RegDates.Kind.Earlier -> R.string.regdate_self_earlier
                RegDates.Kind.Later -> R.string.regdate_self_later
                else -> R.string.regdate_self
            },
            month,
        )
        else -> stringResource(
            when (result.second) {
                RegDates.Kind.Earlier -> R.string.regdate_user_earlier
                RegDates.Kind.Later -> R.string.regdate_user_later
                else -> R.string.regdate_user
            },
            name,
            month,
        )
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    Popup(alignment = Alignment.BottomEnd, offset = with(density) { IntOffset(0, 44.dp.roundToPx()) }, onDismissRequest = close) {
            Surface(
                onClick = close,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .padding(end = 8.dp)
                    .graphicsLayer {
                        val p = appear.value
                        alpha = p.coerceIn(0f, 1f)
                        scaleX = 0.7f + 0.3f * p
                        scaleY = 0.7f + 0.3f * p
                        translationY = -(1f - p) * size.height / 4f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                    },
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(40.dp).clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Cake, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                    }
                    androidx.compose.foundation.layout.Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.profile_regdate), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
    }
}

private fun monthIn(seconds: Long): String {
    val date = java.util.Date(seconds * 1000L)
    val locale = java.util.Locale.getDefault()
    if (locale.language == "ru") {
        val calendar = java.util.Calendar.getInstance().apply { time = date }
        return RuMonthsIn[calendar.get(java.util.Calendar.MONTH)] + " " + calendar.get(java.util.Calendar.YEAR)
    }
    return java.text.SimpleDateFormat("LLLL yyyy", locale).format(date)
}

private val RuMonthsIn = listOf("январе", "феврале", "марте", "апреле", "мае", "июне", "июле", "августе", "сентябре", "октябре", "ноябре", "декабре")
private const val TipShowMs = 4000L

@Composable
private fun BusinessHoursRow(hours: TdApi.BusinessOpeningHours, position: com.pixelgram.android.ui.components.SegmentPosition) {
    val intervals = remember(hours) { BusinessHours.normalize(hours) }
    val always = remember(intervals) { BusinessHours.always(intervals) }
    val zone = remember(hours.timeZoneId) { runCatching { java.time.ZoneId.of(hours.timeZoneId) }.getOrDefault(java.time.ZoneId.systemDefault()) }
    val open = remember(intervals, zone) {
        val now = java.time.ZonedDateTime.now(zone)
        val minute = (now.dayOfWeek.value - 1) * BusinessHours.Day + now.hour * 60 + now.minute
        always || intervals.any { minute >= it.first && minute < it.last }
    }
    var expanded by remember { mutableStateOf(false) }
    val rotation by androidx.compose.animation.core.animateFloatAsState(if (expanded) 180f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "hours")
    val green = Color(0xFF7CC444)
    val locale = java.util.Locale.getDefault()
    val today = java.time.ZonedDateTime.now(zone).dayOfWeek.value - 1
    val closed = stringResource(R.string.hours_closed_day)
    val allDay = stringResource(R.string.hours_all_day)
    SettingsItem(
        stringResource(if (open) R.string.hours_open else R.string.hours_closed),
        position,
        null,
        androidx.compose.material.icons.Icons.Rounded.Schedule,
        pill = true,
        onClick = if (always) null else ({ expanded = !expanded }),
        headlineContent = {
            androidx.compose.foundation.layout.Column {
                Text(stringResource(if (open) R.string.hours_open else R.string.hours_closed), color = if (open) green else MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.profile_business_hours), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.animation.AnimatedVisibility(
                    expanded && !always,
                    enter = androidx.compose.animation.expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + androidx.compose.animation.fadeOut(),
                ) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(7) { day ->
                            val name = java.time.DayOfWeek.of(day + 1).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
                            val value = BusinessHours.dayText(intervals, day, closed, allDay)
                            val style = if (day == today) MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium
                            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
                                Text(name, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                Text(value, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        trailing = {
            if (always) {
                Text(stringResource(R.string.hours_always), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
            } else {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.ExpandMore, null, Modifier.graphicsLayer { rotationZ = rotation })
            }
        },
    )
}

object BusinessHours {
    const val Day = 24 * 60
    private const val Week = 7 * Day

    fun normalize(hours: TdApi.BusinessOpeningHours): List<IntRange> {
        val raw = hours.openingHours.flatMap { interval ->
            val start = interval.startMinute
            val end = interval.endMinute
            if (end > Week) listOf(start until Week, 0 until (end - Week)) else listOf(start until end)
        }.filter { !it.isEmpty() }.sortedBy { it.first }
        val merged = ArrayList<IntRange>()
        raw.forEach { range ->
            val last = merged.lastOrNull()
            if (last != null && range.first <= last.last + 1) merged[merged.size - 1] = last.first..maxOf(last.last, range.last)
            else merged += range
        }
        return merged.map { it.first until it.last + 1 }
    }

    fun always(intervals: List<IntRange>): Boolean = intervals.size == 1 && intervals[0].first <= 0 && intervals[0].last + 1 >= Week

    fun dayText(intervals: List<IntRange>, day: Int, closed: String, allDay: String): String {
        val from = day * Day
        val to = from + Day
        val parts = intervals.mapNotNull { r ->
            val a = maxOf(r.first, from)
            val b = minOf(r.last + 1, to)
            if (a < b) (a - from) to (b - from) else null
        }
        if (parts.isEmpty()) return closed
        if (parts.size == 1 && parts[0].first == 0 && parts[0].second == Day) return allDay
        return parts.joinToString(", ") { (a, b) -> "${clock(a)} – ${clock(b)}" }
    }

    private fun clock(minute: Int): String = "%02d:%02d".format((minute / 60) % 24, minute % 60)
}
