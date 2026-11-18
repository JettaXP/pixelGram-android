package com.pixelgram.android.feature.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin

sealed interface GiftTarget {
    data class Received(val gift: TdApi.ReceivedGift, val own: Boolean) : GiftTarget
    data class Message(val content: TdApi.MessageContent, val date: Int, val outgoing: Boolean) : GiftTarget
    data class Unique(val name: String) : GiftTarget
}

object GiftSheets {
    var current by mutableStateOf<GiftTarget?>(null)
    var changed by mutableStateOf(0)
}

fun giftColor(rgb: Int) = Color(0xFF000000.toInt() or rgb)

private fun rarityText(rarity: TdApi.UpgradedGiftAttributeRarity?): String? = when (rarity) {
    is TdApi.UpgradedGiftAttributeRarityPerMille -> java.math.BigDecimal.valueOf(rarity.perMille.toLong(), 1).stripTrailingZeros().toPlainString() + "%"
    is TdApi.UpgradedGiftAttributeRarityUncommon -> "Uncommon"
    is TdApi.UpgradedGiftAttributeRarityRare -> "Rare"
    is TdApi.UpgradedGiftAttributeRarityEpic -> "Epic"
    is TdApi.UpgradedGiftAttributeRarityLegendary -> "Legendary"
    else -> null
}

private fun formatPrice(amount: Long, currency: String): String = runCatching {
    val code = java.util.Currency.getInstance(currency)
    val format = java.text.NumberFormat.getCurrencyInstance().apply {
        this.currency = code
        minimumFractionDigits = 0
        maximumFractionDigits = code.defaultFractionDigits
    }
    "~" + format.format(amount / Math.pow(10.0, code.defaultFractionDigits.toDouble()))
}.getOrElse { "$amount $currency" }

private const val PatternRings = 2
private const val PatternPerRing = 8
private const val PatternAlpha = 0.35f
private const val SymbolScale = 0.11f

@Composable
fun UniqueGiftArt(gift: TdApi.UpgradedGift, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp), modelScale: Float = 0.62f, square: Boolean = true) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val colors = gift.backdrop?.colors
    BoxWithConstraints(
        modifier
            .then(if (square) Modifier.aspectRatio(1f) else Modifier)
            .clip(shape)
            .background(if (colors != null) Brush.radialGradient(listOf(giftColor(colors.centerColor), giftColor(colors.edgeColor))) else SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest)),
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth, maxHeight)
        val thumb = gift.symbol?.sticker?.thumbnail?.file
        if (thumb != null) {
            val path = snapshot.files[thumb.id]
            if (path == null) LaunchedEffect(thumb.id) { graph.store.requestFileAsync(thumb.id) }
            val image by produceState<ImageBitmap?>(null, path) {
                value = path?.let { withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() } }
            }
            val tint = colors?.let { giftColor(it.symbolColor) } ?: MaterialTheme.colorScheme.onSurfaceVariant
            val filter = remember(tint) { ColorFilter.tint(tint, BlendMode.SrcIn) }
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val picture = image ?: return@Canvas
                val box = minOf(size.width, size.height)
                val mark = (box * SymbolScale).toInt().coerceAtLeast(1)
                repeat(PatternRings) { ring ->
                    val radius = box * (0.3f + ring * 0.14f)
                    repeat(PatternPerRing) { index ->
                        val angle = (index + ring * 0.5) * 2 * Math.PI / PatternPerRing
                        val x = size.width / 2 + radius * cos(angle).toFloat() - mark / 2f
                        val y = size.height / 2 + radius * sin(angle).toFloat() - mark / 2f
                        drawImage(picture, dstOffset = IntOffset(x.toInt(), y.toInt()), dstSize = IntSize(mark, mark), alpha = PatternAlpha, colorFilter = filter)
                    }
                }
            }
        }
        gift.model?.sticker?.let { sticker ->
            StickerView(sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.size(side * modelScale))
        }
    }
}

@Composable
internal fun GiftMessageCard(content: TdApi.MessageContent, date: Int, outgoing: Boolean, color: Color) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val sender = when (content) {
        is TdApi.MessageGift -> content.senderId
        is TdApi.MessageUpgradedGift -> content.senderId
        else -> null
    }
    val senderName = senderTitle(sender)
    val text = when (content) {
        is TdApi.MessageGift -> content.text?.text
        is TdApi.MessageUpgradedGift -> content.text?.text
        else -> null
    }.orEmpty()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(4.dp)
            .width(250.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.08f))
            .clickable { GiftSheets.current = GiftTarget.Message(content, date, outgoing) }
            .padding(14.dp),
    ) {
        when (content) {
            is TdApi.MessageUpgradedGift -> UniqueGiftArt(content.gift, Modifier.size(150.dp))
            is TdApi.MessageGift -> StickerView(content.gift.sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.size(140.dp))
            else -> Icon(Icons.Rounded.CardGiftcard, null, Modifier.size(64.dp))
        }
        if (senderName != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.gift_from_short), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                SenderAvatar(sender, 20.dp)
                Text(senderName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        when (content) {
            is TdApi.MessageUpgradedGift -> Text("${content.gift.title} #${"%,d".format(content.gift.number)}", style = MaterialTheme.typography.bodyMedium, color = color.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            is TdApi.MessageGift -> Text(stringResource(R.string.content_gift_stars, content.gift.starCount), style = MaterialTheme.typography.bodyMedium, color = color.copy(alpha = 0.8f))
            else -> Unit
        }
        if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        FilledTonalButton(onClick = { GiftSheets.current = GiftTarget.Message(content, date, outgoing) }, modifier = Modifier.padding(top = 2.dp)) {
            Text(stringResource(R.string.gift_view))
        }
    }
}

@Composable
private fun senderTitle(sender: TdApi.MessageSender?): String? {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    return when (sender) {
        is TdApi.MessageSenderUser -> {
            val user = snapshot.users[sender.userId]
            if (user == null) LaunchedEffect(sender.userId) { runCatching { graph.td.send(TdApi.GetUser(sender.userId)) } }
            user?.name
        }
        is TdApi.MessageSenderChat -> graph.store.chat(sender.chatId)?.title
        else -> null
    }
}

@Composable
private fun SenderAvatar(sender: TdApi.MessageSender?, size: androidx.compose.ui.unit.Dp) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    when (sender) {
        is TdApi.MessageSenderUser -> {
            val user = snapshot.users[sender.userId]
            Avatar(user?.name.orEmpty(), sender.userId, user?.localPhoto ?: user?.photoFileId?.let { snapshot.files[it] }, user?.minithumb, size, onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) })
        }
        is TdApi.MessageSenderChat -> {
            val chat = graph.store.chat(sender.chatId)
            val photo = chat?.photo?.small?.id ?: 0
            Avatar(chat?.title.orEmpty(), sender.chatId, snapshot.files[photo], chat?.photo?.minithumbnail?.data, size, onNeedFile = { if (photo != 0) graph.store.requestFileAsync(photo) })
        }
        else -> Unit
    }
}

private class GiftRow(val label: String?, val content: @Composable () -> Unit)

@Composable
private fun GiftTable(rows: List<GiftRow>) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Surface(shape = RoundedCornerShape(18.dp), color = Color.Transparent, border = BorderStroke(1.dp, outline), modifier = Modifier.fillMaxWidth()) {
        Column {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = outline)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    if (row.label != null) {
                        Box(Modifier.width(104.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)).padding(horizontal = 14.dp, vertical = 14.dp), contentAlignment = Alignment.CenterStart) {
                            Text(row.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(outline))
                    }
                    Box(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) { row.content() }
                }
            }
        }
    }
}

@Composable
private fun ValueText(text: String, pill: String? = null, onPill: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (pill != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = if (onPill != null) Modifier.clip(CircleShape).clickable(onClick = onPill) else Modifier,
            ) { Text(pill, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) }
        }
    }
}

@Composable
private fun PersonValue(sender: TdApi.MessageSender) {
    val name = senderTitle(sender).orEmpty()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SenderAvatar(sender, 28.dp)
        Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GiftSheetHost() {
    val target = GiftSheets.current ?: return
    val graph = LocalGraph.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dismiss = { GiftSheets.current = null }
    val uniqueName = when (target) {
        is GiftTarget.Unique -> target.name
        is GiftTarget.Received -> (target.gift.gift as? TdApi.SentGiftUpgraded)?.gift?.name
        is GiftTarget.Message -> (target.content as? TdApi.MessageUpgradedGift)?.gift?.name
    }
    val initialUnique = when (target) {
        is GiftTarget.Received -> (target.gift.gift as? TdApi.SentGiftUpgraded)?.gift
        is GiftTarget.Message -> (target.content as? TdApi.MessageUpgradedGift)?.gift
        else -> null
    }
    val unique by produceState(initialUnique, uniqueName) {
        if (uniqueName != null) runCatching { graph.td.send(TdApi.GetUpgradedGift(uniqueName)) }.getOrNull()?.let { value = it }
    }
    val copy: (String) -> Unit = { value ->
        context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText(null, value))
        android.widget.Toast.makeText(context, context.getString(R.string.profile_copied), android.widget.Toast.LENGTH_SHORT).show()
    }
    val share: (String) -> Unit = { link ->
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, link)
        context.startActivity(android.content.Intent.createChooser(send, null))
    }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp),
        ) {
            val nft = unique
            if (uniqueName != null) {
                if (nft == null) {
                    LoadingIndicator(Modifier.padding(32.dp))
                } else {
                    UniqueGiftArt(nft, Modifier.fillMaxWidth().height(260.dp), modelScale = 0.6f, square = false)
                    Text("${nft.title} #${"%,d".format(nft.number)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { share("https://t.me/nft/${nft.name}") }) {
                            Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.profile_share))
                        }
                        FilledTonalButton(onClick = { copy("https://t.me/nft/${nft.name}") }) {
                            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.gift_copy_link))
                        }
                    }
                    if (nft.giftAddress.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.gift_ton), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://tonviewer.com/${nft.giftAddress}"))) }) { Text(stringResource(R.string.gift_view)) }
                        }
                    }
                    val rows = buildList {
                        when {
                            nft.ownerAddress.isNotBlank() -> add(GiftRow(stringResource(R.string.gift_owner)) {
                                Text(nft.ownerAddress.let { if (it.length > 24) it.take(12) + "…" + it.takeLast(12) else it }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, modifier = Modifier.clickable { copy(nft.ownerAddress) })
                            })
                            nft.ownerId != null -> add(GiftRow(stringResource(R.string.gift_owner)) { PersonValue(nft.ownerId!!) })
                            nft.ownerName.isNotBlank() -> add(GiftRow(stringResource(R.string.gift_owner)) { ValueText(nft.ownerName) })
                        }
                        if (nft.ownerAddress.isNotBlank() && nft.ownerId != null) add(GiftRow("Telegram") { PersonValue(nft.ownerId!!) })
                        nft.model?.let { add(GiftRow(stringResource(R.string.gift_model)) { ValueText(it.name, rarityText(it.rarity)) }) }
                        nft.symbol?.let { add(GiftRow(stringResource(R.string.gift_symbol)) { ValueText(it.name, rarityText(it.rarity)) }) }
                        nft.backdrop?.let { add(GiftRow(stringResource(R.string.gift_backdrop)) { ValueText(it.name, rarityText(it.rarity)) }) }
                        val issued = if (nft.maxUpgradedCount > 0) stringResource(R.string.gift_issued_of, "%,d".format(nft.totalUpgradedCount), "%,d".format(nft.maxUpgradedCount)) else "%,d".format(nft.totalUpgradedCount)
                        add(GiftRow(stringResource(R.string.gift_quantity)) { ValueText(issued) })
                        if (nft.valueAmount > 0 && nft.valueCurrency.isNotBlank()) add(GiftRow(stringResource(R.string.gift_value)) { ValueText(formatPrice(nft.valueAmount, nft.valueCurrency)) })
                    }
                    GiftTable(rows)
                }
            } else {
                val regular: TdApi.Gift? = when (target) {
                    is GiftTarget.Received -> (target.gift.gift as? TdApi.SentGiftRegular)?.gift
                    is GiftTarget.Message -> (target.content as? TdApi.MessageGift)?.gift
                    else -> null
                }
                val snapshot by graph.store.snapshot.collectAsState()
                regular?.sticker?.let { StickerView(it, snapshot.files, { id -> graph.store.requestFileAsync(id) }, Modifier.size(170.dp)) }
                val own = when (target) {
                    is GiftTarget.Received -> target.own
                    is GiftTarget.Message -> !target.outgoing
                    else -> false
                }
                Text(stringResource(if (own) R.string.gift_received_title else R.string.content_gift), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val sender: TdApi.MessageSender? = when (target) {
                    is GiftTarget.Received -> target.gift.senderId
                    is GiftTarget.Message -> (target.content as? TdApi.MessageGift)?.senderId
                    else -> null
                }
                val receiver = ((target as? GiftTarget.Message)?.content as? TdApi.MessageGift)?.receiverId
                val date = when (target) {
                    is GiftTarget.Received -> target.gift.date
                    is GiftTarget.Message -> target.date
                    else -> 0
                }
                val text = when (target) {
                    is GiftTarget.Received -> target.gift.text?.text
                    is GiftTarget.Message -> (target.content as? TdApi.MessageGift)?.text?.text
                    else -> null
                }.orEmpty()
                val receivedId = when (target) {
                    is GiftTarget.Received -> target.gift.receivedGiftId
                    is GiftTarget.Message -> (target.content as? TdApi.MessageGift)?.receivedGiftId
                    else -> null
                }
                val initialSaved = when (target) {
                    is GiftTarget.Received -> target.gift.isSaved
                    is GiftTarget.Message -> (target.content as? TdApi.MessageGift)?.isSaved ?: false
                    else -> false
                }
                val sellStars = when (target) {
                    is GiftTarget.Received -> target.gift.sellStarCount
                    is GiftTarget.Message -> (target.content as? TdApi.MessageGift)?.sellStarCount ?: 0L
                    else -> 0L
                }
                var saved by remember(receivedId) { mutableStateOf(initialSaved) }
                val rows = buildList {
                    if (sender != null) add(GiftRow(stringResource(R.string.gift_from)) { PersonValue(sender) })
                    if (receiver != null && !own) add(GiftRow(stringResource(R.string.gift_to)) { PersonValue(receiver) })
                    if (date > 0) add(GiftRow(stringResource(R.string.gift_date)) { ValueText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(date * 1000L))) })
                    regular?.let { g -> add(GiftRow(stringResource(R.string.gift_value)) { ValueText("⭐ ${g.starCount}", if (own && sellStars > 0 && !receivedId.isNullOrBlank()) stringResource(R.string.gift_convert_short, sellStars) else null) {
                        scope.launch {
                            runCatching { graph.td.send(TdApi.SellGift("", receivedId!!)) }.onSuccess {
                                GiftSheets.changed++
                                dismiss()
                            }
                        }
                    } }) }
                    if (text.isNotBlank()) add(GiftRow(null) { Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) })
                    regular?.let { g -> add(GiftRow("ID") { ValueText(g.id.toString(), stringResource(R.string.action_copy)) { copy(g.id.toString()) } }) }
                }
                GiftTable(rows)
                if (own && !receivedId.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(if (saved) R.string.gift_visible else R.string.gift_hidden), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            val next = !saved
                            scope.launch {
                                runCatching { graph.td.send(TdApi.ToggleGiftIsSaved(receivedId, next)) }.onSuccess {
                                    saved = next
                                    GiftSheets.changed++
                                }
                            }
                        }) { Text(stringResource(if (saved) R.string.gift_hide else R.string.gift_show)) }
                    }
                }
            }
            Button(onClick = dismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("OK") }
        }
    }
}
