package com.pixelgram.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.WheelItem
import com.pixelgram.android.ui.components.WheelPicker
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.util.Calendar

private const val MinYear = 1875
private const val UnspecifiedYear = "—"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BirthdaySheet(
    initial: TdApi.Birthdate?,
    onSave: (TdApi.Birthdate) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val today = remember { Calendar.getInstance() }
    val years = remember { ((MinYear..today.get(Calendar.YEAR)).map { it.toString() } + UnspecifiedYear).toTypedArray() }
    val months = remember { java.time.Month.entries.map { it.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()).replaceFirstChar { c -> c.uppercase() } }.toTypedArray() }
    var day by remember { mutableIntStateOf(initial?.day ?: today.get(Calendar.DAY_OF_MONTH)) }
    var month by remember { mutableIntStateOf(initial?.month ?: (today.get(Calendar.MONTH) + 1)) }
    var year by remember { mutableIntStateOf(initial?.year?.takeIf { it > 0 }?.let { (it - MinYear).coerceIn(0, years.size - 2) } ?: years.size - 1) }
    val resolvedYear = if (year == years.size - 1) 0 else year + MinYear
    val maxDay = remember(month, resolvedYear) {
        when {
            resolvedYear > 0 -> java.time.YearMonth.of(resolvedYear, month).lengthOfMonth()
            month == 2 -> 29
            else -> java.time.YearMonth.of(2001, month).lengthOfMonth()
        }
    }
    val shownDay = day.coerceAtMost(maxDay)
    val close: () -> Unit = { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(onDismissRequest = close, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.profile_birthday),
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 12.dp),
            )
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.align(Alignment.Center).fillMaxWidth().height(WheelItem)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                ) {
                    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.primary))
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.primary))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    WheelPicker((1..maxDay).map { it.toString() }, shownDay - 1, Modifier.weight(1f)) { day = it + 1 }
                    WheelPicker(months.toList(), month - 1, Modifier.weight(1.7f)) { month = it + 1 }
                    WheelPicker(years.toList(), year, Modifier.weight(1f)) { year = it }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onRemove != null) {
                    OutlinedButton(
                        onClick = { scope.launch { sheet.hide() }.invokeOnCompletion { onRemove() } },
                        shapes = ButtonDefaults.shapes(),
                    ) { Text(stringResource(R.string.action_remove)) }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { scope.launch { sheet.hide() }.invokeOnCompletion { onSave(TdApi.Birthdate(shownDay, month, resolvedYear)) } },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(R.string.account_save)) }
            }
        }
    }
}
