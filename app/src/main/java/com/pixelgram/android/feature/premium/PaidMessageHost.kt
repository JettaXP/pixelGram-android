package com.pixelgram.android.feature.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.td.PaidMessages

@Composable
fun PaidMessageHost(graph: AppGraph) {
    val request by PaidMessages.pending.collectAsState()
    val current = request ?: return
    val context = LocalContext.current
    var skip by remember(current) { mutableStateOf(false) }
    val balance by produceState<org.drinkless.tdlib.TdApi.StarAmount?>(null, current) { value = starBalance(graph) }
    val total = current.price * current.count
    AlertDialog(
        onDismissRequest = { current.answer.complete(false) },
        title = { Text(stringResource(R.string.paid_confirm_title)) },
        text = {
            Column {
                Text(
                    pluralStringResource(R.plurals.paid_confirm_text, current.count, current.title, current.price, total, current.count),
                    style = MaterialTheme.typography.bodyLarge,
                )
                balance?.let {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.paid_balance) + " ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Image(painterResource(R.drawable.tg_star_small), null, Modifier.size(16.dp))
                        Text(" " + stars(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(skip, { skip = it })
                    Text(stringResource(R.string.paid_dont_ask))
                }
            }
        },
        dismissButton = { TextButton(onClick = { current.answer.complete(false) }) { Text(stringResource(R.string.action_cancel)) } },
        confirmButton = {
            TextButton(onClick = {
                if (skip) PaidMessages.setSkipConfirm(context, current.chatId, true)
                current.answer.complete(true)
            }) { Text(pluralStringResource(R.plurals.paid_confirm_button, current.count, current.count)) }
        },
    )
}
