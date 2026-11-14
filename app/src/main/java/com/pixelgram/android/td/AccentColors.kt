package com.pixelgram.android.td

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import org.drinkless.tdlib.TdApi

object AccentColors {
    private val builtIn = listOf(
        listOf(0xFFCC5049) to listOf(0xFFFF8E86),
        listOf(0xFFD67722) to listOf(0xFFFFAE5C),
        listOf(0xFF955CDB) to listOf(0xFFC8A5FF),
        listOf(0xFF40A920) to listOf(0xFF8AD86C),
        listOf(0xFF309EBA) to listOf(0xFF6FD3EC),
        listOf(0xFF368AD1) to listOf(0xFF7EBDF5),
        listOf(0xFFC7508B) to listOf(0xFFFF8CC4),
    )
    private val custom = mutableStateMapOf<Int, TdApi.AccentColor>()

    fun update(colors: Array<TdApi.AccentColor>) {
        colors.forEach { custom[it.id] = it }
    }

    fun colors(id: Int, dark: Boolean): List<Color> {
        if (id in builtIn.indices) return (if (dark) builtIn[id].second else builtIn[id].first).map { Color(it) }
        val accent = custom[id] ?: return (if (dark) builtIn[0].second else builtIn[0].first).map { Color(it) }
        val values = if (dark) accent.darkThemeColors else accent.lightThemeColors
        if (values.isEmpty()) return colors(accent.builtInAccentColorId, dark)
        return values.map { Color(0xFF000000.toInt() or it) }
    }
}
