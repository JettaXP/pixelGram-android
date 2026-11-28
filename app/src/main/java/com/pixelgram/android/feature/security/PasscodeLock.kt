package com.pixelgram.android.feature.security

import android.content.Context
import android.os.Build
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.SettingsSwitch
import com.pixelgram.android.ui.components.segmentPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object PasscodeLock {
    private const val Prefs = "pixel_passcode"
    var locked by mutableStateOf(false)
    var revision by mutableStateOf(0)
    val AutoLockOptions = listOf(0, 60, 300, 3600, 18000)
    private var stoppedAt = 0L

    private fun prefs(context: Context) = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)

    fun enabled(context: Context) = prefs(context).getString("hash", null) != null

    fun biometric(context: Context) = prefs(context).getBoolean("biometric", false)

    fun autoLock(context: Context) = prefs(context).getInt("autoLock", 60)

    fun setAutoLock(context: Context, seconds: Int) = prefs(context).edit().putInt("autoLock", seconds).apply()

    fun setBiometric(context: Context, value: Boolean) = prefs(context).edit().putBoolean("biometric", value).apply()

    fun lockOnSwitch(context: Context) = prefs(context).getBoolean("lockOnSwitch", true)

    fun setLockOnSwitch(context: Context, value: Boolean) = prefs(context).edit().putBoolean("lockOnSwitch", value).apply()

    fun setSkipNext(context: Context, value: Boolean) = prefs(context).edit().putBoolean("skipNext", value).apply()

    private fun hash(salt: String, pin: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun set(context: Context, pin: String) {
        val salt = java.util.UUID.randomUUID().toString()
        prefs(context).edit().putString("salt", salt).putString("hash", hash(salt, pin)).apply()
        revision++
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        locked = false
        revision++
    }

    fun check(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val salt = p.getString("salt", null) ?: return false
        return p.getString("hash", null) == hash(salt, pin)
    }

    fun lockNow(context: Context) {
        if (enabled(context)) locked = true
    }

    fun onStart(context: Context) {
        if (!enabled(context)) return
        if (prefs(context).getBoolean("skipNext", false)) {
            prefs(context).edit().putBoolean("skipNext", false).apply()
            locked = false
            return
        }
        val auto = autoLock(context)
        if (stoppedAt == 0L || (auto > 0 && System.currentTimeMillis() - stoppedAt > auto * 1000L)) locked = true
    }

    fun onStop() {
        stoppedAt = System.currentTimeMillis()
    }
}

private val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
private fun NumPadKey(size: Dp, onPress: () -> Unit, action: Boolean = false, content: @Composable (Color, Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val normalBg = if (action) scheme.secondary else scheme.surfaceContainerHigh
    val normalText = if (action) scheme.onSecondary else scheme.onSurface
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val p = progress.value
    val bg = androidx.compose.ui.graphics.lerp(normalBg, scheme.primaryContainer, p)
    val fg = androidx.compose.ui.graphics.lerp(normalText, scheme.onPrimaryContainer, p)
    val radius = size / 2 - (size / 4) * p
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    scope.launch { progress.animateTo(1f, tween(33, easing = LinearEasing)) }
                    var inside = true
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                        val inBounds = change.position.x in 0f..this.size.width.toFloat() && change.position.y in 0f..this.size.height.toFloat()
                        if (inside && !inBounds) {
                            inside = false
                            scope.launch { progress.snapTo(0f) }
                        }
                        if (inside) change.consume()
                        if (!change.pressed) {
                            scope.launch { progress.animateTo(0f, tween(300, easing = Standard)) }
                            if (inside) {
                                change.consume()
                                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                onPress()
                            }
                            return@awaitEachGesture
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) { content(fg, 1f + 0.35f * p) }
}

@Composable
fun PinPad(title: String, subtitle: String?, error: Boolean, length: Int, biometric: Boolean, onDigit: (Char) -> Unit, onDelete: () -> Unit, onEnter: () -> Unit, onBiometric: () -> Unit) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error) {
            shake.snapTo(0f)
            shake.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = 1200f), initialVelocity = 3000f)
        }
    }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(AppearMs.toInt(), easing = LinearEasing)) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val entry = with(density) { 120.dp.toPx() }
    val entryStep = with(density) { 10.dp.toPx() }
    val header = Modifier.graphicsLayer {
        val p = StandardDecelerate.transform(appear.value)
        alpha = p
        translationY = entry * (1f - p)
    }
    fun rowModifier(row: Int) = Modifier.graphicsLayer {
        val p = StandardDecelerate.transform(appear.value)
        alpha = LegacyDecelerate.transform(((appear.value - 0.075f * row) / (1f - 0.075f * 4)).coerceIn(0f, 1f))
        translationY = (entry + entryStep * row) * (1f - p)
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding().navigationBarsPadding()) {
        val key = minOf((maxWidth - 64.dp) / 3, (maxHeight - 320.dp) / 4, 96.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Column(header, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(24.dp))
                PinShapes(length, if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, Modifier.padding(horizontal = 24.dp).graphicsLayer { translationX = shake.value / 30f })
            }
            Spacer(Modifier.weight(1f))
            val rows = listOf("123", "456", "789")
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                rows.forEachIndexed { index, row ->
                    Row(rowModifier(index), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { digit ->
                            NumPadKey(key, { onDigit(digit) }) { color, scale ->
                                Text(digit.toString(), color = color, fontSize = DigitSize, fontFamily = DigitFont, modifier = Modifier.graphicsLayer { scaleX = scale })
                            }
                        }
                    }
                }
                Row(rowModifier(3), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    NumPadKey(key, onDelete, action = true) { color, _ -> Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.passcode_delete), tint = color) }
                    NumPadKey(key, { onDigit('0') }) { color, scale ->
                        Text("0", color = color, fontSize = DigitSize, fontFamily = DigitFont, modifier = Modifier.graphicsLayer { scaleX = scale })
                    }
                    NumPadKey(key, onEnter, action = true) { color, _ -> Icon(Icons.AutoMirrored.Rounded.KeyboardReturn, stringResource(R.string.action_done), tint = color) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.height(64.dp).then(rowModifier(4)), contentAlignment = Alignment.Center) {
                if (biometric) {
                    TextButton(onClick = onBiometric) {
                        Icon(Icons.Rounded.Fingerprint, null, Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.passcode_fingerprint))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private class PinShape(val id: Long, val res: Int) {
    val scale = Animatable(1f)
    var removing = false
}

@Composable
private fun PinShapes(length: Int, color: Color, modifier: Modifier) {
    val order = remember { PinShapeRes.shuffled() }
    val items = remember { androidx.compose.runtime.mutableStateListOf<PinShape>() }
    val ids = remember { longArrayOf(0L) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(length) {
        val alive = items.filter { !it.removing }
        if (length > alive.size) {
            for (position in alive.size until length) items += PinShape(ids[0]++, order[position % order.size])
        } else if (length < alive.size) {
            val drop = alive.drop(length).reversed()
            val stagger = if (length == 0) minOf(ResetMaxDelayMs / drop.size, ResetStaggerMs) else 0L
            drop.forEachIndexed { i, item ->
                item.removing = true
                scope.launch {
                    delay(stagger * i)
                    item.scale.animateTo(0f, tween(DisappearMs, easing = LinearEasing))
                    items.remove(item)
                }
            }
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth().height(PinShapeSize), contentAlignment = Alignment.Center) {
        val step = PinShapeSize + PinShapeGap
        val total = step * items.size - PinShapeGap
        val shift = if (total > maxWidth) (maxWidth - total) / 2 else 0.dp
        items.forEachIndexed { index, item ->
            androidx.compose.runtime.key(item.id) {
                val target = (step * index - total / 2 + PinShapeSize / 2 + shift).value
                val x = remember { Animatable(target) }
                LaunchedEffect(target) { x.animateTo(target, tween(AppearShapeMs, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) }
                val argb = color.toArgb()
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        android.widget.ImageView(context).apply {
                            val drawable = context.getDrawable(item.res)?.mutate()
                            setImageDrawable(drawable)
                            imageTintList = android.content.res.ColorStateList.valueOf(argb)
                            (drawable as? android.graphics.drawable.AnimatedVectorDrawable)?.start()
                        }
                    },
                    update = { it.imageTintList = android.content.res.ColorStateList.valueOf(argb) },
                    modifier = Modifier.size(PinShapeSize).graphicsLayer {
                        translationX = x.value.dp.toPx()
                        scaleX = item.scale.value
                        scaleY = item.scale.value
                    },
                )
            }
        }
    }
}

private val PinShapeRes = listOf(R.drawable.pin_shape_1, R.drawable.pin_shape_2, R.drawable.pin_shape_3, R.drawable.pin_shape_4, R.drawable.pin_shape_5, R.drawable.pin_shape_6)
private val PinShapeSize = 30.dp
private val DigitSize = 32.sp
private val DigitFont = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.nunito_black, androidx.compose.ui.text.font.FontWeight.Black))
private val PinShapeGap = 6.dp
private const val AppearShapeMs = 160
private const val DisappearMs = 160
private const val ResetStaggerMs = 40L
private const val ResetMaxDelayMs = 200L
private const val AppearMs = 650L
private val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
private val LegacyDecelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)

fun showBiometric(context: Context, onSuccess: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
    val prompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
        .setTitle(context.getString(R.string.passcode_unlock_title))
        .setNegativeButton(context.getString(R.string.passcode_use_pin), context.mainExecutor) { _, _ -> }
        .build()
    runCatching {
        prompt.authenticate(android.os.CancellationSignal(), context.mainExecutor, object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) = onSuccess()
        })
    }
}

@Composable
fun PasscodeLockHost() {
    val context = LocalContext.current
    AnimatedVisibility(PasscodeLock.locked, enter = fadeIn(), exit = fadeOut() + scaleOut(targetScale = 1.05f)) {
        Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false, dismissOnClickOutside = false)) {
            var entered by remember { mutableStateOf("") }
            var error by remember { mutableStateOf(false) }
            val bio = PasscodeLock.biometric(context)
            val leave = remember { Animatable(0f) }
            val leaveScope = rememberCoroutineScope()
            val unlock: () -> Unit = {
                leaveScope.launch {
                    leave.animateTo(1f, tween(200, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)))
                    PasscodeLock.locked = false
                }
            }
            val lift = with(androidx.compose.ui.platform.LocalDensity.current) { 50.dp.toPx() }
            Box(Modifier.fillMaxSize().graphicsLayer {
                alpha = 1f - leave.value
                translationY = -lift * leave.value
            }) {
            LaunchedEffect(Unit) { if (bio) showBiometric(context) { unlock() } }
            val submit = {
                if (PasscodeLock.check(context, entered)) unlock() else {
                    error = true
                    entered = ""
                }
            }
            PinPad(
                stringResource(R.string.passcode_enter),
                if (error) stringResource(R.string.passcode_wrong) else null,
                error,
                entered.length,
                bio,
                onDigit = { d ->
                    error = false
                    if (entered.length < 12) entered += d
                },
                onDelete = { entered = entered.dropLast(1) },
                onEnter = { submit() },
                onBiometric = { showBiometric(context) { unlock() } },
            )
            }
        }
    }
}

@Composable
fun PasscodeSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(PasscodeLock.enabled(context)) }
    var bio by remember { mutableStateOf(PasscodeLock.biometric(context)) }
    var setting by remember { mutableStateOf(false) }
    var auto by remember { mutableStateOf(PasscodeLock.autoLock(context)) }
    var pickingAuto by remember { mutableStateOf(false) }
    if (pickingAuto) {
        AlertDialog(
            onDismissRequest = { pickingAuto = false },
            title = { Text(stringResource(R.string.passcode_auto_lock)) },
            text = {
                Column {
                    PasscodeLock.AutoLockOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    auto = option
                                    PasscodeLock.setAutoLock(context, option)
                                    pickingAuto = false
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(selected = auto == option, onClick = null, modifier = Modifier.padding(12.dp))
                            Text(autoLockLabel(option), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickingAuto = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
        return
    }
    if (setting) {
        Dialog(onDismissRequest = { setting = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            var first by remember { mutableStateOf<String?>(null) }
            var entered by remember { mutableStateOf("") }
            var error by remember { mutableStateOf(false) }
            PinPad(
                stringResource(if (first == null) R.string.passcode_new else R.string.passcode_repeat),
                if (error) stringResource(R.string.passcode_mismatch) else stringResource(R.string.passcode_hint),
                error,
                entered.length,
                false,
                onDigit = { d ->
                    error = false
                    if (entered.length < 12) entered += d
                },
                onDelete = { entered = entered.dropLast(1) },
                onEnter = {
                    if (entered.length >= 4) {
                        val firstPin = first
                        if (firstPin == null) {
                            first = entered
                            entered = ""
                        } else if (firstPin == entered) {
                            PasscodeLock.set(context, entered)
                            enabled = true
                            setting = false
                        } else {
                            error = true
                            first = null
                            entered = ""
                        }
                    }
                },
                onBiometric = {},
            )
        }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Lock, null) },
        title = { Text(stringResource(R.string.privacy_passcode)) },
        text = {
            Column {
                Text(stringResource(R.string.passcode_about), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                SettingsGroup {
                    SettingsSwitch(stringResource(R.string.passcode_enable), enabled, { v ->
                        if (v) setting = true else {
                            PasscodeLock.clear(context)
                            enabled = false
                            bio = false
                        }
                    }, segmentPosition(0, if (enabled) 4 else 1))
                    if (enabled) {
                        SettingsItem(stringResource(R.string.passcode_change), segmentPosition(1, 4), onClick = { setting = true })
                        SettingsItem(stringResource(R.string.passcode_auto_lock), segmentPosition(2, 4), trailing = {
                            Text(autoLockLabel(auto), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }, onClick = { pickingAuto = true })
                        SettingsSwitch(stringResource(R.string.passcode_fingerprint), bio, { v ->
                            bio = v
                            PasscodeLock.setBiometric(context, v)
                        }, segmentPosition(3, 4), enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}

@Composable
private fun autoLockLabel(seconds: Int): String = when {
    seconds == 0 -> stringResource(R.string.passcode_auto_off)
    seconds < 3600 -> androidx.compose.ui.res.pluralStringResource(R.plurals.passcode_minutes, seconds / 60, seconds / 60)
    else -> androidx.compose.ui.res.pluralStringResource(R.plurals.passcode_hours, seconds / 3600, seconds / 3600)
}
