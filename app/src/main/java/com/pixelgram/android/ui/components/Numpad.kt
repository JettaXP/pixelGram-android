package com.pixelgram.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Numpad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    busy: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val rows = listOf("123", "456", "789")
            for (row in rows) {
                KeyRow {
                    for (digit in row) {
                        DigitKey(digit, Modifier.weight(1f)) {
                            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            onDigit(digit)
                        }
                    }
                }
            }
            KeyRow {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        onBackspace()
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(KeyHeight),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.action_backspace), Modifier.size(26.dp))
                }
                DigitKey('0', Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onDigit('0')
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSubmit()
                    },
                    enabled = submitEnabled && !busy,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(KeyHeight),
                ) {
                    if (busy) {
                        LoadingIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.action_next), Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DigitKey(digit: Char, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.height(KeyHeight),
    ) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineMediumEmphasized)
    }
}

private val KeyHeight = 64.dp
