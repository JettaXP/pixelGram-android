package com.pixelgram.android.feature.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.td.SensitiveRequests
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensitiveRequestHost() {
    val request by SensitiveRequests.pending.collectAsState()
    val current = request ?: return
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val me = snapshot.users[snapshot.meId]
    val decline = { current.answer.complete(false); Unit }
    ModalBottomSheet(onDismissRequest = decline, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp),
        ) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Report, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Text(stringResource(R.string.sensitive_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(current.category), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val outline = MaterialTheme.colorScheme.outlineVariant
            Surface(shape = RoundedCornerShape(18.dp), color = Color.Transparent, border = BorderStroke(1.dp, outline), modifier = Modifier.fillMaxWidth()) {
                Column {
                    val rows = buildList<Pair<String, @Composable () -> Unit>> {
                        add(stringResource(R.string.sensitive_account) to {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Avatar(me?.name.orEmpty(), snapshot.meId, me?.localPhoto ?: me?.photoFileId?.let { snapshot.files[it] }, me?.minithumb, 28.dp, onNeedFile = { me?.photoFileId?.let(graph.store::requestFileAsync) })
                                Text(me?.name.orEmpty(), color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        })
                        current.fields.forEach { (label, value) -> add(label to { Text(value, maxLines = 3, overflow = TextOverflow.Ellipsis) }) }
                        add(stringResource(R.string.sensitive_request) to { Text(current.method, fontFamily = FontFamily.Monospace) })
                    }
                    rows.forEachIndexed { index, (label, value) ->
                        if (index > 0) HorizontalDivider(color = outline)
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(132.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)).padding(14.dp), contentAlignment = Alignment.CenterStart) {
                                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Box(Modifier.width(1.dp).fillMaxHeight().background(outline))
                            Box(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) { value() }
                        }
                    }
                }
            }
            Text(stringResource(R.string.sensitive_warning), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = { current.answer.complete(true) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.sensitive_allow)) }
            FilledTonalButton(onClick = decline, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.sensitive_decline)) }
        }
    }
}
