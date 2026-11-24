package com.pixelgram.android.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

enum class UsernameIssue { Invalid, StartsWithDigit, StartsWithUnderscore, EndsWithUnderscore, TooShort }

object UsernameRules {
    const val Min = 5
    const val Max = 32

    fun check(value: String): UsernameIssue? {
        if (value.isEmpty()) return null
        if (value.any { !(it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_') }) return UsernameIssue.Invalid
        if (value.first().isDigit()) return UsernameIssue.StartsWithDigit
        if (value.first() == '_') return UsernameIssue.StartsWithUnderscore
        if (value.length < Min) return UsernameIssue.TooShort
        if (value.last() == '_') return UsernameIssue.EndsWithUnderscore
        return null
    }
}

private sealed interface Availability {
    data object Checking : Availability
    data object Free : Availability
    data object Taken : Availability
    data object ForSale : Availability
    data object Limit : Availability
    data object Wrong : Availability
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameScreen(graph: AppGraph, navigator: Navigator) {
    val snapshot by graph.store.snapshot.collectAsState()
    val current = snapshot.users[snapshot.meId]?.username.orEmpty()
    var value by remember { mutableStateOf(current) }
    var availability by remember { mutableStateOf<Availability?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val issue = UsernameRules.check(value)
    LaunchedEffect(value) {
        failure = null
        availability = null
        if (value.isEmpty() || issue != null || value.equals(current, ignoreCase = true)) return@LaunchedEffect
        availability = Availability.Checking
        delay(CheckDelayMs)
        val result = runCatching { graph.td.send(TdApi.CheckChatUsername(snapshot.meId, value)) }.getOrNull()
        availability = when (result) {
            is TdApi.CheckChatUsernameResultOk -> Availability.Free
            is TdApi.CheckChatUsernameResultUsernameOccupied -> Availability.Taken
            is TdApi.CheckChatUsernameResultUsernamePurchasable -> Availability.ForSale
            is TdApi.CheckChatUsernameResultPublicChatsTooMany -> Availability.Limit
            else -> Availability.Wrong
        }
    }
    val canSave = !busy && issue == null && availability != Availability.Checking && availability != Availability.Taken && availability != Availability.Wrong && availability != Availability.ForSale
    val save = {
        if (canSave) {
            if (value == current) navigator.back() else {
                busy = true
                scope.launch {
                    runCatching { graph.td.send(TdApi.SetUsername(value)) }
                        .onSuccess { navigator.back() }
                        .onFailure { failure = (it as? com.pixelgram.android.td.TdException)?.error?.message ?: it.message }
                    busy = false
                }
            }
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.username_title)) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
                actions = { IconButton(onClick = { save() }, enabled = canSave) { Icon(Icons.Rounded.Check, stringResource(R.string.action_done)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.username_set),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 10.dp),
            )
            TextField(
                value,
                { value = it.filterNot { c -> c.isWhitespace() || c == '@' }.take(UsernameRules.Max) },
                prefix = { Text("t.me/") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            val status: Pair<String, Color>? = when {
                failure != null -> failure!! to MaterialTheme.colorScheme.error
                issue == UsernameIssue.Invalid -> stringResource(R.string.username_invalid) to MaterialTheme.colorScheme.error
                issue == UsernameIssue.StartsWithDigit -> stringResource(R.string.username_digit) to MaterialTheme.colorScheme.error
                issue == UsernameIssue.StartsWithUnderscore -> stringResource(R.string.username_underscore_start) to MaterialTheme.colorScheme.error
                issue == UsernameIssue.EndsWithUnderscore -> stringResource(R.string.username_underscore_end) to MaterialTheme.colorScheme.error
                issue == UsernameIssue.TooShort -> stringResource(R.string.username_short) to MaterialTheme.colorScheme.error
                availability == Availability.Checking -> stringResource(R.string.username_checking) to MaterialTheme.colorScheme.onSurfaceVariant
                availability == Availability.Free -> stringResource(R.string.username_free, value) to Color(0xFF7CC444)
                availability == Availability.Taken -> stringResource(R.string.username_taken) to MaterialTheme.colorScheme.error
                availability == Availability.ForSale -> stringResource(R.string.username_sale) to MaterialTheme.colorScheme.error
                availability == Availability.Limit -> stringResource(R.string.username_limit) to MaterialTheme.colorScheme.error
                availability == Availability.Wrong -> stringResource(R.string.username_invalid) to MaterialTheme.colorScheme.error
                else -> null
            }
            if (status != null) Text(status.first, color = status.second, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp))
            val bold = SpanStyle(fontWeight = FontWeight.SemiBold)
            val about = stringResource(R.string.username_about)
            val rules = stringResource(R.string.username_rules)
            Text(
                buildAnnotatedString {
                    appendBold(about, bold)
                    append("\n\n")
                    appendBold(rules, bold)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
            )
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendBold(text: String, style: SpanStyle) {
    val parts = text.split("**")
    parts.forEachIndexed { index, part -> if (index % 2 == 1) withStyle(style) { append(part) } else append(part) }
}

private const val CheckDelayMs = 300L
