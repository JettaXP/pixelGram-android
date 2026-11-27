package com.pixelgram.android.feature.settings

import android.widget.NumberPicker
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pixelgram.android.R
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
                Row(
                    Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Wheel(min = 1, value = shownDay, max = maxDay, displayed = null) { day = it }
                    Wheel(min = 0, value = month - 1, max = months.size - 1, displayed = months) { month = it + 1 }
                    Wheel(min = 0, value = year, max = years.size - 1, displayed = years) { year = it }
                }
                Column(Modifier.matchParentSize().padding(horizontal = 20.dp)) {
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.height(40.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.weight(1f))
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

@Composable
private fun Wheel(
    min: Int,
    value: Int,
    max: Int,
    displayed: Array<String>?,
    onValue: (Int) -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val textSize = with(LocalDensity.current) { MaterialTheme.typography.titleMedium.fontSize.toPx() }
    val latest = rememberUpdatedState(onValue)
    AndroidView(
        modifier = Modifier.height(200.dp),
        factory = { context ->
            NumberPicker(context).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = false
                setSelectionDividerHeight(0)
                minValue = min
                maxValue = max
                displayed?.let { displayedValues = it }
                this.value = value
                setTextSize(textSize)
                setTextColor(textColor)
                setOnValueChangedListener { _, _, new -> latest.value(new) }
            }
        },
        update = { picker ->
            picker.setTextColor(textColor)
            if (picker.minValue != min) picker.minValue = min
            if (picker.maxValue != max) picker.maxValue = max
            displayed?.let { picker.displayedValues = it }
            if (picker.value != value) picker.value = value
        },
    )
}
