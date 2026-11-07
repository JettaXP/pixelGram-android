package com.pixelgram.android.feature.auth

import androidx.compose.runtime.Immutable
import com.pixelgram.android.td.TdClient
import com.pixelgram.android.td.TdException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

sealed interface AuthStep {
    data object Loading : AuthStep
    data object Phone : AuthStep
    @Immutable
    data class Code(val phone: String, val type: CodeType, val length: Int, val nextType: CodeType?, val timeout: Int) : AuthStep
    data class Password(val hint: String) : AuthStep
    data class Qr(val link: String) : AuthStep
    data object Ready : AuthStep
    data class Unsupported(val reason: String) : AuthStep
}

enum class CodeType { App, Sms, Call, FlashCall, MissedCall, Fragment, Firebase, Email, Other }

@Immutable
data class AuthUi(val step: AuthStep = AuthStep.Loading, val busy: Boolean = false, val error: String? = null, val wantQr: Boolean = false)

class AuthController(private val td: TdClient, private val scope: CoroutineScope) {
    private val ui = MutableStateFlow(AuthUi())
    val state: StateFlow<AuthUi> = ui.asStateFlow()

    init {
        scope.launch {
            td.authorizationState.collect { state ->
                val step = map(state)
                ui.value = ui.value.copy(step = step, busy = false)
            }
        }
    }

    private fun map(state: TdApi.AuthorizationState?): AuthStep = when (state) {
        null, is TdApi.AuthorizationStateWaitTdlibParameters, is TdApi.AuthorizationStateClosed,
        is TdApi.AuthorizationStateClosing, is TdApi.AuthorizationStateLoggingOut -> AuthStep.Loading
        is TdApi.AuthorizationStateWaitPhoneNumber -> if (ui.value.wantQr) AuthStep.Loading else AuthStep.Phone
        is TdApi.AuthorizationStateWaitCode -> state.codeInfo.let { info ->
            val (type, length) = info.type.describe()
            AuthStep.Code(info.phoneNumber, type, length, info.nextType?.describe()?.first, info.timeout)
        }
        is TdApi.AuthorizationStateWaitPassword -> AuthStep.Password(state.passwordHint)
        is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> AuthStep.Qr(state.link)
        is TdApi.AuthorizationStateReady -> AuthStep.Ready
        is TdApi.AuthorizationStateWaitEmailAddress, is TdApi.AuthorizationStateWaitEmailCode ->
            AuthStep.Unsupported("email")
        is TdApi.AuthorizationStateWaitRegistration -> AuthStep.Unsupported("registration")
        else -> AuthStep.Unsupported("other")
    }

    private fun run(action: suspend () -> Unit) {
        ui.value = ui.value.copy(busy = true, error = null)
        scope.launch {
            try {
                action()
            } catch (e: TdException) {
                ui.value = ui.value.copy(busy = false, error = e.error.message)
            }
        }
    }

    fun submitPhone(phone: String) = run {
        td.send(TdApi.SetAuthenticationPhoneNumber().apply {
            phoneNumber = phone
            settings = TdApi.PhoneNumberAuthenticationSettings().apply {
                allowFlashCall = false
                allowMissedCall = false
                isCurrentPhoneNumber = false
                hasUnknownPhoneNumber = false
                allowSmsRetrieverApi = false
                authenticationTokens = emptyArray()
            }
        })
    }

    fun submitCode(code: String) = run { td.send(TdApi.CheckAuthenticationCode(code)) }

    fun resendCode() = run { td.send(TdApi.ResendAuthenticationCode(TdApi.ResendCodeReasonUserRequest())) }

    fun submitPassword(password: String) = run { td.send(TdApi.CheckAuthenticationPassword(password)) }

    fun requestQr() {
        ui.value = ui.value.copy(wantQr = true)
        run { td.send(TdApi.RequestQrCodeAuthentication(LongArray(0))) }
    }

    fun backToPhone() {
        ui.value = ui.value.copy(step = AuthStep.Phone, wantQr = false, error = null, busy = false)
    }

    fun clearError() {
        ui.value = ui.value.copy(error = null)
    }
}

private fun TdApi.AuthenticationCodeType.describe(): Pair<CodeType, Int> = when (this) {
    is TdApi.AuthenticationCodeTypeTelegramMessage -> CodeType.App to length
    is TdApi.AuthenticationCodeTypeSms -> CodeType.Sms to length
    is TdApi.AuthenticationCodeTypeCall -> CodeType.Call to length
    is TdApi.AuthenticationCodeTypeFlashCall -> CodeType.FlashCall to 0
    is TdApi.AuthenticationCodeTypeMissedCall -> CodeType.MissedCall to length
    is TdApi.AuthenticationCodeTypeFragment -> CodeType.Fragment to length
    is TdApi.AuthenticationCodeTypeFirebaseAndroid -> CodeType.Firebase to length
    is TdApi.AuthenticationCodeTypeFirebaseIos -> CodeType.Firebase to length
    else -> CodeType.Other to 0
}
