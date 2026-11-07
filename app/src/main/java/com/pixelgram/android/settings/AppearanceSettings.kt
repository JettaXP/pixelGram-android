package com.pixelgram.android.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixelgram.android.ui.theme.AvatarShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode { System, Light, Dark }

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val monochrome: Boolean = false,
    val avatarShape: AvatarShape = AvatarShape.Circle,
    val avatarCorner: Int = 50,
    val hidePhone: Boolean = true,
)

private val Context.store by preferencesDataStore("appearance")

class AppearanceStore(private val context: Context, private val scope: CoroutineScope) {
    private object Keys {
        val Theme = stringPreferencesKey("theme")
        val Dynamic = booleanPreferencesKey("dynamic")
        val Mono = booleanPreferencesKey("monochrome")
        val Shape = stringPreferencesKey("avatar_shape")
        val Corner = intPreferencesKey("avatar_corner")
        val HidePhone = booleanPreferencesKey("hide_phone")
    }

    val state: StateFlow<AppearanceSettings> = context.store.data
        .map(::read)
        .stateIn(scope, SharingStarted.Eagerly, AppearanceSettings())

    private fun read(p: Preferences) = AppearanceSettings(
        themeMode = ThemeMode.entries.firstOrNull { it.name == p[Keys.Theme] } ?: ThemeMode.System,
        dynamicColor = p[Keys.Dynamic] ?: false,
        monochrome = p[Keys.Mono] ?: false,
        avatarShape = AvatarShape.entries.firstOrNull { it.name == p[Keys.Shape] } ?: AvatarShape.Circle,
        avatarCorner = p[Keys.Corner] ?: 50,
        hidePhone = p[Keys.HidePhone] ?: true,
    )

    fun update(change: (AppearanceSettings) -> AppearanceSettings) {
        scope.launch {
            context.store.edit { p ->
                val next = change(read(p))
                p[Keys.Theme] = next.themeMode.name
                p[Keys.Dynamic] = next.dynamicColor
                p[Keys.Mono] = next.monochrome
                p[Keys.Shape] = next.avatarShape.name
                p[Keys.Corner] = next.avatarCorner
                p[Keys.HidePhone] = next.hidePhone
            }
        }
    }
}
