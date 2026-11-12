package com.pixelgram.android.feature.premium

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.Notice
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.drinkless.tdlib.TdApi

val PremiumGradient = listOf(Color(0xFF6B93FF), Color(0xFF976FFF), Color(0xFFE46ACE))
val StarGold = listOf(Color(0xFFFFC93C), Color(0xFFFF9F1C))

internal fun price(amount: Long, currency: String): String = runCatching {
    val cur = java.util.Currency.getInstance(currency)
    java.text.NumberFormat.getCurrencyInstance().apply { this.currency = cur }.format(amount / Math.pow(10.0, cur.defaultFractionDigits.toDouble()))
}.getOrDefault("$amount $currency")

internal fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

internal class Feature(val icon: ImageVector, val title: Int, val text: Int, val key: String = "")

private val Features = listOf(
    Feature(Icons.Rounded.PlayCircle, R.string.premium_f_stories, R.string.premium_f_stories_d),
    Feature(Icons.Rounded.CloudUpload, R.string.premium_f_storage, R.string.premium_f_storage_d),
    Feature(Icons.Rounded.AllInclusive, R.string.premium_f_limits, R.string.premium_f_limits_d),
    Feature(Icons.Rounded.Storefront, R.string.premium_f_business, R.string.premium_f_business_d),
    Feature(Icons.Rounded.Speed, R.string.premium_f_speed, R.string.premium_f_speed_d),
    Feature(Icons.Rounded.Mic, R.string.premium_f_voice, R.string.premium_f_voice_d),
    Feature(Icons.Rounded.Translate, R.string.premium_f_translate, R.string.premium_f_translate_d),
    Feature(Icons.Rounded.Block, R.string.premium_f_ads, R.string.premium_f_ads_d),
    Feature(Icons.Rounded.Favorite, R.string.premium_f_reactions, R.string.premium_f_reactions_d),
    Feature(Icons.Rounded.EmojiEmotions, R.string.premium_f_emoji, R.string.premium_f_emoji_d),
    Feature(Icons.Rounded.Badge, R.string.premium_f_status, R.string.premium_f_status_d),
    Feature(Icons.Rounded.AutoAwesome, R.string.premium_f_badge, R.string.premium_f_badge_d),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GradientMark(icon: ImageVector, colors: List<Color>, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).clip(MaterialShapes.Cookie12Sided.toShape()).background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.56f)) }
}

@Composable
internal fun FeatureRows(features: List<Feature> = Features, onClick: ((Feature) -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        features.forEachIndexed { index, feature ->
            val shape = when (index) {
                0 -> RoundedCornerShape(24.dp, 24.dp, 6.dp, 6.dp)
                features.lastIndex -> RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp)
                else -> RoundedCornerShape(6.dp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .then(if (onClick != null) Modifier.clickable { onClick(feature) } else Modifier).padding(16.dp),
            ) {
                val tone = PremiumGradient[index % PremiumGradient.size]
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tone.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(feature.icon, null, tint = tone, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(feature.title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(feature.text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PremiumScreen(graph: AppGraph, navigator: Navigator, business: Boolean = false) {
    var editor by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val snapshot by graph.store.snapshot.collectAsState()
    val me = snapshot.users[snapshot.meId]
    val state by produceState<TdApi.PremiumState?>(null) { value = runCatching { graph.td.send(TdApi.GetPremiumState()) }.getOrNull() ?: TdApi.PremiumState(TdApi.FormattedText("", emptyArray()), emptyArray(), emptyArray(), emptyArray()) }
    val options = state?.paymentOptions.orEmpty().map { it.paymentOption }.sortedByDescending { it.monthCount }
    var picked by remember(options.size) { mutableStateOf(0) }
    var paying by remember { mutableStateOf(false) }
    Scaffold(
        bottomBar = {
            val option = options.getOrNull(picked)
            if (me?.premium != true && option != null) {
                Button(
                    onClick = { paying = true },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(58.dp),
                ) {
                    Text(
                        stringResource(if (option.monthCount >= 12) R.string.premium_subscribe_year else R.string.premium_subscribe_month, price(option.amount, option.currency)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PremiumGradient[1].copy(alpha = 0.45f), Color.Transparent)))) {
                IconButton(onClick = { navigator.back() }, modifier = Modifier.statusBarsPadding().padding(4.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, bottom = 16.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GradientMark(if (business) Icons.Rounded.Storefront else Icons.Rounded.Star, PremiumGradient, 112.dp)
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(if (business) R.string.business_title else R.string.premium_title), style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
                    Text(
                        stringResource(if (business) (if (me?.premium == true) R.string.business_active else R.string.business_lead) else if (me?.premium == true) R.string.premium_active else R.string.premium_lead),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state == null) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                } else if (me?.premium != true && options.isNotEmpty()) {
                    val monthly = options.minByOrNull { it.monthCount }
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            options.forEachIndexed { index, option ->
                                val perMonth = option.amount / option.monthCount.coerceAtLeast(1)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxWidth().clickable { picked = index }.padding(horizontal = 16.dp, vertical = 12.dp),
                                ) {
                                    Icon(if (picked == index) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(planName(context, option.monthCount), style = MaterialTheme.typography.titleMedium)
                                            if (option.discountPercentage > 0) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = PremiumGradient[1]) {
                                                    Text("-${option.discountPercentage}%", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        if (option.monthCount > 1) Text(price(option.amount, option.currency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(stringResource(R.string.premium_per_month, price(perMonth, option.currency)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (monthly == null) Unit
                            }
                        }
                    }
                }
                if (business) {
                    FeatureRows(BusinessFeatures, if (me?.premium == true) ({ editor = it.key }) else null)
                    Text(stringResource(R.string.business_later), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                } else FeatureRows()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    editor?.let { BusinessEditor(graph, it) { editor = null } }
    if (paying) {
        val option = options.getOrNull(picked)
        PayMethodSheet(
            graph = graph,
            title = stringResource(R.string.premium_title),
            link = option?.paymentLink,
            fragment = "https://fragment.com/premium/gift" + (me?.username?.let { "?recipient=$it&months=${option?.monthCount ?: 12}" } ?: ""),
            onDismiss = { paying = false },
        )
    }
}

private fun planName(context: Context, months: Int): String = when {
    months >= 12 && months % 12 == 0 -> if (months == 12) context.getString(R.string.premium_annual) else context.getString(R.string.premium_years, months / 12)
    months == 1 -> context.getString(R.string.premium_monthly)
    else -> context.getString(R.string.premium_months, months)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PayMethodSheet(graph: AppGraph, title: String, link: TdApi.InternalLinkType?, invoice: TdApi.InputInvoice? = null, fragment: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            Text(stringResource(R.string.pay_choose), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
            PayRow(Icons.Rounded.CreditCard, stringResource(R.string.pay_card), stringResource(R.string.pay_card_sub)) {
                if (invoice == null) Notice.show(context.getString(R.string.pay_opening))
                onDismiss()
                graph.scope.launch {
                    val target = invoice ?: resolveInvoice(graph, link)
                    if (target != null) {
                        PaymentRequest.current = target
                    } else if (link != null) {
                        val url = runCatching { graph.td.send(TdApi.GetInternalLink(link, true)).url }.getOrNull()
                        if (url != null) openUrl(context, url) else Notice.show(context.getString(R.string.pay_unavailable))
                    } else Notice.show(context.getString(R.string.pay_unavailable))
                }
            }
            PayRow(Icons.Rounded.Diamond, stringResource(R.string.pay_fragment), stringResource(R.string.pay_fragment_sub)) {
                openUrl(context, fragment)
                onDismiss()
            }
        }
    }
}

suspend fun resolveInvoice(graph: AppGraph, link: TdApi.InternalLinkType?): TdApi.InputInvoice? = when (link) {
    is TdApi.InternalLinkTypeInvoice -> TdApi.InputInvoiceName(link.invoiceName)
    is TdApi.InternalLinkTypeBotStart -> runCatching {
        val chat = graph.td.send(TdApi.SearchPublicChat(link.botUsername))
        val botId = (chat.type as TdApi.ChatTypePrivate).userId
        kotlinx.coroutines.coroutineScope {
            val waiter = async {
                kotlinx.coroutines.withTimeoutOrNull(20_000) {
                    graph.td.updates.first { u -> u is TdApi.UpdateNewMessage && u.message.chatId == chat.id && u.message.content is TdApi.MessageInvoice }
                } as? TdApi.UpdateNewMessage
            }
            graph.td.send(TdApi.SendBotStartMessage(botId, chat.id, link.startParameter))
            waiter.await()?.message?.let { TdApi.InputInvoiceMessage(it.chatId, it.id) }
        }
    }.getOrNull()
    else -> null
}

@Composable
private fun PayRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick).padding(16.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun stars(amount: TdApi.StarAmount?): String {
    if (amount == null) return "0"
    val whole = amount.starCount
    val frac = amount.nanostarCount
    return if (frac == 0) "%,d".format(whole).replace(',', ' ') else "%,d.%d".format(whole, frac / 100_000_000).replace(',', ' ')
}

suspend fun starBalance(graph: AppGraph): TdApi.StarAmount? = runCatching {
    graph.td.send(TdApi.GetStarTransactions(TdApi.MessageSenderUser(graph.store.snapshot.value.meId), "", null, "", 1)).starAmount
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StarsScreen(graph: AppGraph, navigator: Navigator) {
    val snapshot by graph.store.snapshot.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var topUp by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<TdApi.StarTransaction?>(null) }
    var reload by remember { mutableStateOf(0) }
    val data by produceState<TdApi.StarTransactions?>(null, tab, reload) {
        val direction = when (tab) {
            1 -> TdApi.TransactionDirectionIncoming()
            2 -> TdApi.TransactionDirectionOutgoing()
            else -> null
        }
        value = runCatching { graph.td.send(TdApi.GetStarTransactions(TdApi.MessageSenderUser(snapshot.meId), "", direction, "", 50)) }.getOrNull()
    }
    LaunchedEffect(PaymentRequest.completed) { reload++ }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
        item {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(StarGold[0].copy(alpha = 0.3f), Color.Transparent)))) {
                IconButton(onClick = { navigator.back() }, modifier = Modifier.statusBarsPadding().padding(4.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GradientMark(Icons.Rounded.Star, StarGold, 112.dp)
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.stars_title), style = MaterialTheme.typography.headlineMediumEmphasized)
                    Text(stringResource(R.string.stars_lead), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Star, null, tint = StarGold[0], modifier = Modifier.size(36.dp))
                        Text(stars(data?.starAmount), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Text(stringResource(R.string.stars_balance), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { topUp = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Rounded.AddCircle, null)
                        Text(stringResource(R.string.stars_top_up), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        item {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent, modifier = Modifier.padding(top = 16.dp)) {
                listOf(R.string.stars_all, R.string.stars_incoming, R.string.stars_outgoing).forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(label)) })
                }
            }
        }
        val list = data?.transactions.orEmpty()
        if (data == null) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
        else if (list.isEmpty()) item { Text(stringResource(R.string.stars_empty), modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(list, key = { it.id }) { tx -> TransactionRow(graph, tx, snapshot) { opened = tx } }
    }
    }
    if (topUp) TopUpSheet(graph, snapshot.users[snapshot.meId]?.username) { topUp = false }
    opened?.let { TransactionSheet(graph, it) { opened = null } }
}

private fun txPeer(type: TdApi.StarTransactionType): TdApi.MessageSender? {
    var fallback: TdApi.MessageSender? = null
    for (field in type.javaClass.fields) {
        val value = runCatching { field.get(type) }.getOrNull() ?: continue
        when {
            value is TdApi.MessageSender -> return value
            field.name == "userId" && value is Long && value != 0L -> return TdApi.MessageSenderUser(value)
            field.name == "chatId" && value is Long && value != 0L -> fallback = TdApi.MessageSenderChat(value)
        }
    }
    return fallback
}

private fun txSticker(type: TdApi.StarTransactionType): TdApi.Sticker? {
    for (field in type.javaClass.fields) {
        when (val value = runCatching { field.get(type) }.getOrNull()) {
            is TdApi.Gift -> return value.sticker
            is TdApi.UpgradedGift -> return value.model?.sticker
            is TdApi.Sticker -> return value
        }
    }
    return null
}

internal data class Peer(val id: Long, val title: String, val photoFileId: Int, val minithumb: ByteArray?)

@Composable
internal fun rememberPeer(graph: AppGraph, sender: TdApi.MessageSender?): Peer? {
    val peer by produceState<Peer?>(null, sender) {
        value = when (sender) {
            is TdApi.MessageSenderUser -> runCatching { graph.td.send(TdApi.GetUser(sender.userId)) }.getOrNull()?.let {
                Peer(it.id, listOf(it.firstName, it.lastName).filter { s -> s.isNotBlank() }.joinToString(" "), it.profilePhoto?.small?.id ?: 0, it.profilePhoto?.minithumbnail?.data)
            }
            is TdApi.MessageSenderChat -> runCatching { graph.td.send(TdApi.GetChat(sender.chatId)) }.getOrNull()?.let {
                Peer(it.id, it.title, it.photo?.small?.id ?: 0, it.photo?.minithumbnail?.data)
            }
            else -> null
        }
    }
    return peer
}

@Composable
private fun PeerAvatar(graph: AppGraph, peer: Peer, size: androidx.compose.ui.unit.Dp) {
    val snapshot by graph.store.snapshot.collectAsState()
    com.pixelgram.android.ui.components.Avatar(
        title = peer.title,
        seed = peer.id,
        path = snapshot.files[peer.photoFileId],
        minithumb = peer.minithumb,
        size = size,
        onNeedFile = if (peer.photoFileId != 0) ({ graph.store.requestFileAsync(peer.photoFileId) }) else null,
    )
}

@Composable
private fun TxSticker(graph: AppGraph, sticker: TdApi.Sticker, size: androidx.compose.ui.unit.Dp) {
    val snapshot by graph.store.snapshot.collectAsState()
    com.pixelgram.android.feature.chat.StickerView(sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.size(size))
}

private val TxShortDate = java.text.SimpleDateFormat("MMM d, HH:mm:ss", java.util.Locale.getDefault())
private val TxLongDate = java.text.SimpleDateFormat("d MMMM yyyy, HH:mm:ss", java.util.Locale.getDefault())

private fun TdApi.StarTransaction.incoming() = starAmount.starCount > 0 || (starAmount.starCount == 0L && starAmount.nanostarCount > 0)

@Composable
private fun TxAmount(tx: TdApi.StarTransaction, style: androidx.compose.ui.text.TextStyle, star: androidx.compose.ui.unit.Dp) {
    val incoming = tx.incoming()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text((if (incoming) "+" else "") + stars(tx.starAmount), style = style, color = if (incoming) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
        Icon(Icons.Rounded.Star, null, tint = StarGold[0], modifier = Modifier.size(star))
    }
}

@Composable
private fun TransactionRow(graph: AppGraph, tx: TdApi.StarTransaction, snapshot: com.pixelgram.android.td.StoreSnapshot, onClick: () -> Unit) {
    val context = LocalContext.current
    val (label, _) = describe(context, tx, snapshot)
    val peer = rememberPeer(graph, txPeer(tx.type))
    val sticker = txSticker(tx.type)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        if (peer != null) PeerAvatar(graph, peer, 48.dp)
        else Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(StarGold)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Star, null, tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(peer?.title?.takeIf { it.isNotBlank() } ?: label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (peer != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (sticker != null) TxSticker(graph, sticker, 18.dp)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(TxShortDate.format(java.util.Date(tx.date * 1000L)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TxAmount(tx, MaterialTheme.typography.titleMedium, 18.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TransactionSheet(graph: AppGraph, tx: TdApi.StarTransaction, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val snapshot by graph.store.snapshot.collectAsState()
    val (label, _) = describe(context, tx, snapshot)
    val other = rememberPeer(graph, txPeer(tx.type))
    val me = rememberPeer(graph, remember { TdApi.MessageSenderUser(snapshot.meId) })
    val sticker = txSticker(tx.type)
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val copied = stringResource(R.string.stars_tx_copied)
    val (from, to) = if (tx.incoming()) other to me else me to other
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                sticker != null -> TxSticker(graph, sticker, 140.dp)
                other != null -> PeerAvatar(graph, other, 96.dp)
                else -> GradientMark(Icons.Rounded.Star, StarGold, 96.dp)
            }
            Text(label, style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center)
            TxAmount(tx, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), 28.dp)
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Column {
                    @Composable
                    fun Line(name: Int, content: @Composable () -> Unit) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(stringResource(name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
                            Box(Modifier.weight(1f)) { content() }
                        }
                    }
                    @Composable
                    fun PeerLine(peer: Peer) = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PeerAvatar(graph, peer, 28.dp)
                        Text(peer.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val divider = @Composable { androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                    if (from != null) { Line(R.string.stars_tx_from) { PeerLine(from) }; divider() }
                    if (to != null) { Line(R.string.stars_tx_to) { PeerLine(to) }; divider() }
                    Line(R.string.stars_tx_id) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tx.id, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(tx.id))
                                Notice.show(copied)
                            }) { Icon(Icons.Rounded.ContentCopy, stringResource(R.string.stars_tx_id)) }
                        }
                    }
                    divider()
                    Line(R.string.stars_tx_date) { Text(TxLongDate.format(java.util.Date(tx.date * 1000L)), style = MaterialTheme.typography.titleSmall) }
                }
            }
            if (tx.isRefund) Text(stringResource(R.string.stars_tx_refund), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            androidx.compose.material3.TextButton(onClick = { openUrl(context, "https://telegram.org/tos/stars") }) { Text(stringResource(R.string.stars_tos)) }
            Button(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(android.R.string.ok), style = MaterialTheme.typography.titleMedium) }
        }
    }
}

private fun describe(context: Context, tx: TdApi.StarTransaction, snapshot: com.pixelgram.android.td.StoreSnapshot): Pair<String, String?> {
    fun user(id: Long) = snapshot.users[id]?.name
    return when (val t = tx.type) {
        is TdApi.StarTransactionTypeFragmentDeposit -> context.getString(R.string.stars_tx_fragment) to null
        is TdApi.StarTransactionTypePremiumBotDeposit -> context.getString(R.string.stars_tx_bot_deposit) to null
        is TdApi.StarTransactionTypeAppStoreDeposit, is TdApi.StarTransactionTypeGooglePlayDeposit -> context.getString(R.string.stars_tx_purchase) to null
        is TdApi.StarTransactionTypeUserDeposit -> context.getString(R.string.stars_tx_gift_from) to user(t.userId)
        is TdApi.StarTransactionTypeGiftPurchase -> context.getString(R.string.stars_tx_gift_sent) to null
        is TdApi.StarTransactionTypeGiftSale -> context.getString(R.string.stars_tx_gift_converted) to user(t.userId)
        is TdApi.StarTransactionTypeBotInvoicePurchase -> context.getString(R.string.stars_tx_bot_purchase) to user(t.userId)
        is TdApi.StarTransactionTypeBotPaidMediaPurchase -> context.getString(R.string.stars_tx_media) to user(t.userId)
        is TdApi.StarTransactionTypeChannelPaidMediaPurchase -> context.getString(R.string.stars_tx_media) to null
        is TdApi.StarTransactionTypeChannelPaidReactionSend -> context.getString(R.string.stars_tx_reaction) to null
        is TdApi.StarTransactionTypeBotSubscriptionPurchase -> context.getString(R.string.stars_tx_subscription) to user(t.userId)
        else -> context.getString(if (tx.starAmount.starCount >= 0) R.string.stars_tx_incoming else R.string.stars_tx_outgoing) to null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopUpSheet(graph: AppGraph, username: String?, onDismiss: () -> Unit) {
    val options by produceState<List<TdApi.StarPaymentOption>?>(null) {
        value = runCatching { graph.td.send(TdApi.GetStarPaymentOptions()).options.sortedBy { it.starCount } }.getOrNull() ?: emptyList()
    }
    var chosen by remember { mutableStateOf<TdApi.StarPaymentOption?>(null) }
    var more by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.stars_top_up), style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            val list = options
            if (list == null) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            list?.filter { more || !it.isAdditional }?.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { chosen = option }.padding(16.dp),
                ) {
                    Icon(Icons.Rounded.Star, null, tint = StarGold[0])
                    Text(stringResource(R.string.stars_count, option.starCount), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(price(option.amount, option.currency), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!more && list?.any { it.isAdditional } == true) {
                androidx.compose.material3.TextButton(onClick = { more = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.stars_more)) }
            }
        }
    }
    chosen?.let { option ->
        PayMethodSheet(
            graph = graph,
            title = stringResource(R.string.stars_count, option.starCount),
            link = null,
            invoice = TdApi.InputInvoiceTelegram(TdApi.TelegramPaymentPurposeStars(option.currency, option.amount, option.starCount, 0)),
            fragment = "https://fragment.com/stars/buy" + (username?.let { "?recipient=$it&quantity=${option.starCount}" } ?: ""),
            onDismiss = {
                chosen = null
                onDismiss()
            },
        )
    }
}

object PaymentRequest {
    var current by mutableStateOf<TdApi.InputInvoice?>(null)
    var completed by mutableStateOf(0)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PaymentHost() {
    val invoice = PaymentRequest.current ?: return
    val graph = LocalGraph.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember(invoice) { mutableStateOf<String?>(null) }
    val form by produceState<TdApi.PaymentForm?>(null, invoice) {
        value = runCatching { graph.td.send(TdApi.GetPaymentForm(invoice, null)) }.onFailure { error = it.message }.getOrNull()
    }
    val balance by produceState<TdApi.StarAmount?>(null) { value = starBalance(graph) }
    var busy by remember(invoice) { mutableStateOf(false) }
    val close = { PaymentRequest.current = null }
    val done = stringResource(R.string.pay_done)
    ModalBottomSheet(onDismissRequest = close) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val loaded = form
            when {
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                loaded == null -> LoadingIndicator(Modifier.padding(24.dp))
                else -> {
                    val type = loaded.type
                    GradientMark(if (type is TdApi.PaymentFormTypeStars) Icons.Rounded.Star else Icons.Rounded.CreditCard, if (type is TdApi.PaymentFormTypeStars) StarGold else PremiumGradient, 72.dp)
                    Text(loaded.productInfo.title, style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
                    if (loaded.productInfo.description.text.isNotBlank()) {
                        Text(loaded.productInfo.description.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    graph.store.snapshot.value.users[loaded.sellerBotUserId]?.let { seller ->
                        Text(stringResource(R.string.pay_seller, seller.name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    when (type) {
                        is TdApi.PaymentFormTypeStars -> {
                            Text(stringResource(R.string.pay_balance, stars(balance)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val enough = (balance?.starCount ?: 0) >= type.starCount
                            Button(
                                onClick = {
                                    busy = true
                                    scope.launch {
                                        val result = runCatching { graph.td.send(TdApi.SendPaymentForm(invoice, loaded.id, "", "", null, 0)) }
                                        busy = false
                                        result.onSuccess {
                                            Notice.show(done)
                                            PaymentRequest.completed++
                                            close()
                                        }.onFailure { error = it.message }
                                    }
                                },
                                enabled = !busy && enough,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                            ) {
                                if (busy) LoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                else Text(stringResource(R.string.pay_stars, type.starCount), style = MaterialTheme.typography.titleMedium)
                            }
                            if (!enough) Text(stringResource(R.string.pay_not_enough), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        }
                        is TdApi.PaymentFormTypeRegular -> {
                            val native = (type.paymentProvider is TdApi.PaymentProviderStripe || type.paymentProvider is TdApi.PaymentProviderSmartGlocal) && !type.invoice.needShippingAddress && !type.invoice.isFlexible
                            if (native) {
                                val verify = stringResource(R.string.card_verify)
                                CardForm(graph, invoice, loaded, type) { url ->
                                    if (url != null) {
                                        openUrl(context, url)
                                        Notice.show(verify)
                                    } else Notice.show(done)
                                    PaymentRequest.completed++
                                    close()
                                }
                            } else {
                                val total = type.invoice.priceParts.sumOf { it.amount }
                                Text(price(total, type.invoice.currency), style = MaterialTheme.typography.headlineSmall)
                                val url = (type.paymentProvider as? TdApi.PaymentProviderOther)?.url
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val target = url ?: runCatching {
                                                when (invoice) {
                                                    is TdApi.InputInvoiceName -> graph.td.send(TdApi.GetInternalLink(TdApi.InternalLinkTypeInvoice(invoice.name), true)).url
                                                    is TdApi.InputInvoiceMessage -> graph.td.send(TdApi.GetMessageLink().apply { chatId = invoice.chatId; messageId = invoice.messageId }).link
                                                    else -> null
                                                }
                                            }.getOrNull()
                                            if (target != null) openUrl(context, target) else error = context.getString(R.string.pay_unavailable)
                                            if (target != null) close()
                                        }
                                    },
                                    shapes = ButtonDefaults.shapes(),
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                ) { Text(stringResource(R.string.pay_card_open), style = MaterialTheme.typography.titleMedium) }
                                Text(stringResource(R.string.pay_card_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                        else -> Text(stringResource(R.string.pay_unavailable), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

object EmojiStatusSheet {
    var userId by mutableStateOf(0L)
    var emojiId by mutableStateOf(0L)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmojiStatusHost(onPremium: () -> Unit) {
    val emoji = EmojiStatusSheet.emojiId
    if (emoji == 0L) return
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val user = snapshot.users[EmojiStatusSheet.userId]
    val pack by produceState<String?>(null, emoji) {
        value = runCatching {
            val sticker = graph.td.send(TdApi.GetCustomEmojiStickers(longArrayOf(emoji))).stickers.firstOrNull() ?: return@runCatching null
            if (sticker.setId == 0L) null else graph.td.send(TdApi.GetStickerSet(sticker.setId)).title
        }.getOrNull()
    }
    val close = { EmojiStatusSheet.emojiId = 0L }
    val me = snapshot.users[snapshot.meId]
    ModalBottomSheet(onDismissRequest = close) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.pixelgram.android.ui.components.CustomEmojiView(emoji, "⭐", size = 120.dp)
            Text(
                if (pack != null) stringResource(R.string.status_from_pack, user?.name.orEmpty(), pack.orEmpty()) else stringResource(R.string.status_set, user?.name.orEmpty()),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (me?.premium != true) {
                Text(stringResource(R.string.status_premium), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FeatureRows()
                Button(
                    onClick = {
                        close()
                        onPremium()
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGradient[1], contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(stringResource(R.string.premium_learn), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}
val BusinessIcon get() = androidx.compose.material.icons.Icons.Rounded.Storefront
