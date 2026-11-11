package com.pixelgram.android.feature.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.launch

object SupportProject {
    var open by mutableStateOf(false)
    const val Contact = "jettaxp"
    val Wallets = listOf(
        "USDT · TON" to "UQCDaQUP33fPSuaAVDlyHk2ayYzcz8mDST9GvdFFAMJdlAvs",
        "USDT · TRC20" to "TUMktFcyp7WtBnVfYP9gYPY92rdnFXUzgx",
        "TON" to "UQCDaQUP33fPSuaAVDlyHk2ayYzcz8mDST9GvdFFAMJdlAvs",
    )
    val LinkPattern = Regex("pg://support", RegexOption.IGNORE_CASE)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SupportSheet() {
    if (!SupportProject.open) return
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { SupportProject.open = false } }
    ModalBottomSheet(onDismissRequest = { SupportProject.open = false }, sheetState = state) {
        Column(Modifier.navigationBarsPadding()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            ) {
                Box(
                    Modifier.size(96.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_pixelgram), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(60.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.support_title), style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.support_lead),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                SectionTitle(stringResource(R.string.support_perks))
                Perk(Icons.Rounded.Star, stringResource(R.string.support_numbered), stringResource(R.string.support_numbered_text))
                Perk(Icons.Rounded.Palette, stringResource(R.string.support_custom), stringResource(R.string.support_custom_text))
                SectionTitle(stringResource(R.string.support_how))
                Perk(Icons.Rounded.Payments, stringResource(R.string.support_step_pay), stringResource(R.string.support_step_pay_text))
                SupportProject.Wallets.forEach { (network, address) -> WalletRow(network, address) }
                Perk(Icons.Rounded.Image, stringResource(R.string.support_step_proof), stringResource(R.string.support_step_proof_text, SupportProject.Contact))
                Perk(Icons.Rounded.Celebration, stringResource(R.string.support_step_badge), stringResource(R.string.support_step_badge_text))
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    close()
                    uri.openUri("https://t.me/${SupportProject.Contact}")
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(56.dp),
            ) {
                Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.support_contact, SupportProject.Contact), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Perk(icon: ImageVector, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            Modifier.size(42.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WalletRow(network: String, address: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, top = 2.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable {
                clipboard.setText(AnnotatedString(address))
                copied = true
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(network, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(address, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
            stringResource(R.string.action_copy),
            tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp).size(20.dp),
        )
    }
}
