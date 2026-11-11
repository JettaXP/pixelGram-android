package com.pixelgram.android.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.android.R
import com.pixelgram.android.util.Phones
import com.pixelgram.android.ui.components.ExpressiveQrCode
import com.pixelgram.android.ui.components.Numpad
import com.pixelgram.android.ui.components.ShapeCodeField
import com.pixelgram.android.ui.components.ShapePasswordBar
import kotlinx.coroutines.delay

@Composable
fun AuthFlow(controller: AuthController) {
    val ui by controller.state.collectAsState()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fast = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        targetState = ui.step::class,
        transitionSpec = {
            (fadeIn(effects) + scaleIn(spatial, initialScale = 0.92f)).togetherWith(fadeOut(fast))
        },
        label = "auth-step",
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) { _ ->
        when (val step = ui.step) {
            AuthStep.Loading, AuthStep.Ready -> LoadingScreen()
            AuthStep.Phone -> PhoneScreen(ui, controller::submitPhone, controller::requestQr, controller::clearError)
            is AuthStep.Code -> CodeScreen(step, ui, controller)
            is AuthStep.Password -> PasswordScreen(step, ui, controller)
            is AuthStep.Qr -> QrScreen(step, controller)
            is AuthStep.Unsupported -> UnsupportedScreen(step, controller)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(72.dp)) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AuthHeader(title: String, subtitle: String, iconShape: androidx.compose.ui.graphics.Shape) {
    Box(
        Modifier
            .size(88.dp)
            .clip(iconShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_pixelgram),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp),
        )
    }
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.displaySmallEmphasized, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AuthScaffold(
    onBack: (() -> Unit)?,
    bottom: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onBack != null) BackHandler(onBack = onBack)
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        bottom()
    }
    }
}

internal enum class PhoneField { Code, Number }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PhoneScreen(ui: AuthUi, onSubmit: (String) -> Unit, onQr: () -> Unit, onClearError: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val initialCode = remember {
        val telephony = context.getSystemService(android.telephony.TelephonyManager::class.java)
        val region = listOfNotNull(telephony?.networkCountryIso, telephony?.simCountryIso, java.util.Locale.getDefault().country)
            .firstOrNull { it.length == 2 }
        region?.let { runCatching { Phones.codeForRegion(it) }.getOrNull() }.orEmpty()
    }
    var code by rememberSaveable { mutableStateOf(initialCode) }
    var number by rememberSaveable { mutableStateOf("") }
    var field by rememberSaveable { mutableStateOf(if (initialCode.isNotEmpty()) PhoneField.Number else PhoneField.Code) }
    val validCode = remember(code) { Phones.isCallingCode(code) }
    val country = remember(code, number) { Phones.country(code, number) }
    val formatted = remember(code, number) { Phones.formatNational(code, number) }
    val placeholder = remember(code) { Phones.placeholder(code) }
    AuthScaffold(
        onBack = null,
        bottom = {
            Numpad(
                onDigit = { digit ->
                    onClearError()
                    when (field) {
                        PhoneField.Code -> if (code.length < 4 && Phones.isCodePrefix(code + digit)) {
                            code += digit
                            if (Phones.completesCode(code)) field = PhoneField.Number
                        }
                        PhoneField.Number -> if (number.length < Phones.maxNationalLength(code)) number += digit
                    }
                },
                onBackspace = {
                    when (field) {
                        PhoneField.Number -> if (number.isEmpty()) {
                            field = PhoneField.Code
                        } else {
                            number = number.dropLast(1)
                        }
                        PhoneField.Code -> code = code.dropLast(1)
                    }
                },
                onSubmit = { onSubmit("+$code$number") },
                submitEnabled = validCode && number.length >= 4,
                busy = ui.busy,
            )
        },
    ) {
        Spacer(Modifier.height(16.dp))
        AuthHeader(stringResource(R.string.auth_phone_title), stringResource(R.string.auth_phone_subtitle), MaterialShapes.Cookie12Sided.toShape())
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PhonePart(
                active = field == PhoneField.Code,
                onClick = { field = PhoneField.Code },
            ) {
                Text(
                    "+$code",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = if (code.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            PhonePart(
                active = field == PhoneField.Number,
                onClick = { if (validCode) field = PhoneField.Number },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (number.isEmpty()) placeholder ?: stringResource(R.string.auth_phone_number) else formatted,
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = if (number.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                country != null -> "${country.first}  ${country.second}"
                code.isEmpty() -> stringResource(R.string.auth_country_hint)
                else -> stringResource(R.string.auth_country_invalid)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (country == null && code.isNotEmpty() && !Phones.isCodePrefix(code)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ErrorText(ui.error)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onQr, shapes = ButtonDefaults.shapes()) {
            Icon(Icons.Rounded.QrCode2, null, Modifier.size(20.dp))
            Text(stringResource(R.string.auth_qr_button), Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun PhonePart(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val border by androidx.compose.animation.animateColorAsState(if (active) scheme.primary else scheme.outlineVariant, label = "phone-border")
    androidx.compose.material3.OutlinedCard(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(containerColor = if (active) scheme.surfaceContainerHigh else scheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, border),
        modifier = modifier.height(64.dp),
    ) {
        Box(Modifier.fillMaxHeight().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { content() }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(12.dp))
        Text(authErrorText(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun authErrorText(error: String): String = when (error) {
    "PHONE_NUMBER_INVALID" -> stringResource(R.string.auth_error_phone)
    "PHONE_CODE_INVALID" -> stringResource(R.string.auth_error_code)
    "PHONE_CODE_EXPIRED" -> stringResource(R.string.auth_error_code_expired)
    "PASSWORD_HASH_INVALID" -> stringResource(R.string.auth_error_password)
    "PHONE_NUMBER_FLOOD", "FLOOD_WAIT" -> stringResource(R.string.auth_error_flood)
    "API_ID_INVALID", "API_ID_PUBLISHED_FLOOD" -> stringResource(R.string.auth_error_api)
    else -> if (error.startsWith("Too Many Requests") || error.startsWith("FLOOD_WAIT")) stringResource(R.string.auth_error_flood)
    else stringResource(R.string.auth_error_generic, error)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CodeScreen(step: AuthStep.Code, ui: AuthUi, controller: AuthController) {
    var code by rememberSaveable(step.phone) { mutableStateOf("") }
    val length = step.length.takeIf { it > 0 } ?: 5
    LaunchedEffect(ui.error) { if (ui.error != null) code = "" }
    var left by remember(step) { mutableIntStateOf(step.timeout) }
    LaunchedEffect(step) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }
    AuthScaffold(
        onBack = controller::backToPhone,
        bottom = {
            Numpad(
                onDigit = {
                    controller.clearError()
                    if (code.length < length) code += it
                    if (code.length == length) controller.submitCode(code)
                },
                onBackspace = { code = code.dropLast(1) },
                onSubmit = { controller.submitCode(code) },
                submitEnabled = code.isNotEmpty(),
                busy = ui.busy,
            )
        },
    ) {
        AuthHeader(
            stringResource(R.string.auth_code_title),
            codeSubtitle(step.type, step.phone),
            MaterialShapes.Sunny.toShape(),
        )
        Spacer(Modifier.height(32.dp))
        ShapeCodeField(code, length, error = ui.error != null)
        ErrorText(ui.error)
        if (step.details.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(step.details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(12.dp))
        if (step.nextType != null) {
            TextButton(onClick = controller::resendCode, enabled = left == 0 && !ui.busy, shapes = ButtonDefaults.shapes()) {
                Text(
                    if (left > 0) stringResource(R.string.auth_resend_in, left / 60, left % 60)
                    else stringResource(R.string.auth_resend, codeTypeName(step.nextType)),
                )
            }
        }
        if (step.type == CodeType.App) {
            Text(
                stringResource(R.string.auth_code_where),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = controller::backToPhone, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.auth_change_number)) }
            FilledTonalButton(onClick = controller::requestQr, enabled = !ui.busy, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.QrCode2, null, Modifier.size(18.dp))
                Text(stringResource(R.string.auth_qr_short), Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun codeSubtitle(type: CodeType, phone: String): String = when (type) {
    CodeType.App -> stringResource(R.string.auth_code_app)
    CodeType.Sms -> stringResource(R.string.auth_code_sms, phone)
    CodeType.Call -> stringResource(R.string.auth_code_call, phone)
    CodeType.FlashCall, CodeType.MissedCall -> stringResource(R.string.auth_code_missed_call, phone)
    CodeType.Fragment -> stringResource(R.string.auth_code_fragment)
    CodeType.Firebase -> stringResource(R.string.auth_code_firebase)
    CodeType.Email -> stringResource(R.string.auth_code_email)
    CodeType.Other -> stringResource(R.string.auth_code_sms, phone)
}

@Composable
private fun codeTypeName(type: CodeType): String = when (type) {
    CodeType.Sms -> stringResource(R.string.auth_via_sms)
    CodeType.Call, CodeType.FlashCall, CodeType.MissedCall -> stringResource(R.string.auth_via_call)
    CodeType.App -> stringResource(R.string.auth_via_app)
    else -> stringResource(R.string.auth_via_other)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PasswordScreen(step: AuthStep.Password, ui: AuthUi, controller: AuthController) {
    AuthScaffold(onBack = controller::backToPhone) {
        AuthHeader(
            stringResource(R.string.auth_password_title),
            if (step.hint.isNotBlank()) stringResource(R.string.auth_password_hint, step.hint) else stringResource(R.string.auth_password_subtitle),
            MaterialShapes.Clover4Leaf.toShape(),
        )
        Spacer(Modifier.height(32.dp))
        ShapePasswordBar(
            busy = ui.busy,
            error = ui.error,
            onSubmit = controller::submitPassword,
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().imePadding(),
        )
        if (ui.error != null && ui.error != "PASSWORD_HASH_INVALID") ErrorText(ui.error)
    }
}

@Composable
private fun QrScreen(step: AuthStep.Qr, controller: AuthController) {
    AuthScaffold(onBack = controller::backToPhone) {
        Text(stringResource(R.string.auth_qr_title), style = MaterialTheme.typography.displaySmallEmphasized, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.auth_qr_instructions), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        ExpressiveQrCode(step.link, Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = controller::backToPhone, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.auth_qr_use_phone))
        }
    }
}

@Composable
private fun UnsupportedScreen(step: AuthStep.Unsupported, controller: AuthController) {
    AuthScaffold(onBack = controller::backToPhone) {
        Text(stringResource(R.string.auth_unsupported_title), style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (step.reason == "registration") R.string.auth_unsupported_registration else R.string.auth_unsupported_email),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
