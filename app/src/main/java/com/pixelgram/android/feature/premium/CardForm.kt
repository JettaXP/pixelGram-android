package com.pixelgram.android.feature.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.PasswordChars
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

private val FieldShape = RoundedCornerShape(16.dp)

private fun TextFieldValue.end() = copy(selection = TextRange(text.length))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CardForm(
    graph: AppGraph,
    invoice: TdApi.InputInvoice,
    form: TdApi.PaymentForm,
    type: TdApi.PaymentFormTypeRegular,
    onPaid: (verificationUrl: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stripe = type.paymentProvider as? TdApi.PaymentProviderStripe
    val needs = type.invoice
    var number by remember { mutableStateOf(TextFieldValue("")) }
    var month by remember { mutableStateOf(TextFieldValue("")) }
    var year by remember { mutableStateOf(TextFieldValue("")) }
    var cvc by remember { mutableStateOf(TextFieldValue("")) }
    var holder by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var postal by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(type.savedOrderInfo?.name.orEmpty()) }
    var phone by remember { mutableStateOf(type.savedOrderInfo?.phoneNumber.orEmpty()) }
    var email by remember { mutableStateOf(type.savedOrderInfo?.emailAddress.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val monthFocus = remember { FocusRequester() }
    val yearFocus = remember { FocusRequester() }
    val cvcFocus = remember { FocusRequester() }
    var cvcFocused by remember { mutableStateOf(false) }

    val digits = number.text
    val numberError = digits.length >= maxLength(digits) && !Card.luhn(digits)
    val expiryError = month.text.length == 2 && (!Card.monthValid(month.text) || (year.text.length == 2 && !Card.expiryValid(month.text, year.text)))
    val cvcLength = Card.cvcLength(digits)
    val orderOk = (!needs.needName || name.isNotBlank()) && (!needs.needPhoneNumber || phone.isNotBlank()) && (!needs.needEmailAddress || email.contains('@'))
    val valid = Card.luhn(digits) && Card.expiryValid(month.text, year.text) && cvc.text.length == cvcLength &&
        (stripe?.needCardholderName != true || holder.isNotBlank()) &&
        (stripe?.needCountry != true || country.isNotBlank()) &&
        (stripe?.needPostalCode != true || postal.isNotBlank()) && orderOk
    val total = needs.priceParts.sumOf { it.amount }
    val failed = stringResource(R.string.card_failed)

    val submit = submit@{
        if (!valid || busy) return@submit
        busy = true
        error = null
        scope.launch {
            val orderId = if (needs.needName || needs.needPhoneNumber || needs.needEmailAddress) {
                runCatching {
                    graph.td.send(TdApi.ValidateOrderInfo(invoice, TdApi.OrderInfo(name.trim(), phone.trim(), email.trim(), null), true)).orderInfoId
                }.onFailure { error = it.message }.getOrNull()
            } else ""
            if (orderId == null) {
                busy = false
                return@launch
            }
            val token = Card.tokenize(
                type.paymentProvider,
                needs.isTest,
                Card.Details(digits, month.text, year.text, cvc.text, holder.trim(), country.trim(), postal.trim()),
            )
            if (token == null) {
                busy = false
                error = failed
                return@launch
            }
            runCatching {
                graph.td.send(TdApi.SendPaymentForm(invoice, form.id, orderId, "", TdApi.InputCredentialsNew(token, false), 0))
            }.onSuccess { result ->
                busy = false
                onPaid(result.verificationUrl.takeIf { it.isNotBlank() })
            }.onFailure {
                busy = false
                error = it.message
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = number,
            onValueChange = { v ->
                val clean = Card.digits(v.text, 19)
                number = TextFieldValue(clean, TextRange(clean.length))
                if (clean.length == maxLength(clean) && Card.luhn(clean)) runCatching { monthFocus.requestFocus() }
            },
            label = { Text(stringResource(R.string.card_number)) },
            leadingIcon = { Icon(Icons.Rounded.CreditCard, null) },
            trailingIcon = Card.brand(digits)?.let { b -> { Text(b, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 12.dp)) } },
            isError = numberError,
            supportingText = if (numberError) ({ Text(stringResource(R.string.card_invalid_number)) }) else null,
            visualTransformation = Card.Grouping,
            singleLine = true,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = month,
                onValueChange = { v ->
                    val m = Card.month(v.text)
                    month = TextFieldValue(m, TextRange(m.length))
                    if (m.length == 2 && Card.monthValid(m)) runCatching { yearFocus.requestFocus() }
                },
                placeholder = { Text(stringResource(R.string.card_expiry_month), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                isError = expiryError,
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f).focusRequester(monthFocus),
            )
            Text("/", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = year,
                onValueChange = { v ->
                    val y = Card.digits(v.text, 2)
                    year = TextFieldValue(y, TextRange(y.length))
                    if (y.length == 2 && Card.expiryValid(month.text, y)) runCatching { cvcFocus.requestFocus() }
                },
                placeholder = { Text(stringResource(R.string.card_expiry_year), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                isError = expiryError,
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(yearFocus)
                    .onPreviewKeyEvent {
                        if (it.key == Key.Backspace && year.text.isEmpty()) {
                            month = month.end()
                            runCatching { monthFocus.requestFocus() }
                            true
                        } else false
                    },
            )
            CvcField(
                value = cvc,
                length = cvcLength,
                focused = cvcFocused,
                onValueChange = { v ->
                    val c = Card.digits(v.text, cvcLength)
                    cvc = TextFieldValue(c, TextRange(c.length))
                },
                onDone = { submit() },
                fieldModifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(cvcFocus)
                    .onFocusChanged { cvcFocused = it.isFocused }
                    .onPreviewKeyEvent {
                        if (it.key == Key.Backspace && cvc.text.isEmpty()) {
                            year = year.end()
                            runCatching { yearFocus.requestFocus() }
                            true
                        } else false
                    },
                modifier = Modifier.weight(1.3f),
            )
        }
        AnimatedVisibility(expiryError) {
            Text(stringResource(R.string.card_invalid_expiry), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp))
        }
        if (stripe?.needCardholderName == true) {
            TextInput(holder, { holder = it.filter { c -> c.isLetter() || c == ' ' || c == '-' || c == '\'' }.uppercase() }, R.string.card_holder, KeyboardType.Text, KeyboardCapitalization.Characters)
        }
        if (stripe?.needCountry == true || stripe?.needPostalCode == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stripe.needCountry) TextInput(country, { country = it.filter { c -> c.isLetter() }.take(2).uppercase() }, R.string.card_country, KeyboardType.Text, KeyboardCapitalization.Characters, Modifier.weight(1f))
                if (stripe.needPostalCode) TextInput(postal, { postal = it.filter { c -> c.isLetterOrDigit() || c == ' ' || c == '-' }.take(10) }, R.string.card_postal, KeyboardType.Text, KeyboardCapitalization.Characters, Modifier.weight(1f))
            }
        }
        if (needs.needName) TextInput(name, { name = it }, R.string.order_name, KeyboardType.Text, KeyboardCapitalization.Words)
        if (needs.needPhoneNumber) TextInput(phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) }, R.string.order_phone, KeyboardType.Phone)
        if (needs.needEmailAddress) TextInput(email, { email = it.trim() }, R.string.order_email, KeyboardType.Email)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        Button(
            onClick = { submit() },
            enabled = valid && !busy,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (busy) LoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(R.string.card_pay, price(total, needs.currency)), style = MaterialTheme.typography.titleMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.card_secure, Card.providerName(type.paymentProvider) ?: context.getString(R.string.pay_card)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun maxLength(number: String) = when {
    number.startsWith("34") || number.startsWith("37") -> 15
    number.startsWith("4") || number.startsWith("5") || number.startsWith("2") -> 16
    else -> 19
}

@Composable
private fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    keyboard: KeyboardType,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        shape = FieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = capitalization, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

@Composable
private fun CvcField(
    value: TextFieldValue,
    length: Int,
    focused: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    onDone: () -> Unit,
    fieldModifier: Modifier,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .height(56.dp)
            .border(if (focused) 2.dp else 1.dp, if (focused) scheme.primary else scheme.outline, FieldShape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.text.isEmpty()) {
            Text(stringResource(R.string.card_cvc), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
        }
        PasswordChars(length = value.text.length, selection = value.selection, cursorVisible = focused, modifier = Modifier.fillMaxWidth().fillMaxHeight())
        CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(Color.Transparent, Color.Transparent)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = fieldModifier,
            )
        }
    }
}
