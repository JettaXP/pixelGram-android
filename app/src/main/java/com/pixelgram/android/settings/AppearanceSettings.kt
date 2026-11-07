package com.pixelgram.android.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
    val dynamicColor: Boolean = Build.VERSION.SDK_INT >= 31,
    val dynamicIcon: Boolean = false,
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
        val DynamicIcon = booleanPreferencesKey("dynamic_icon")
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
        dynamicColor = p[Keys.Dynamic] ?: (Build.VERSION.SDK_INT >= 31),
        dynamicIcon = p[Keys.DynamicIcon] ?: false,
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
                p[Keys.DynamicIcon] = next.dynamicIcon
                p[Keys.Mono] = next.monochrome
                p[Keys.Shape] = next.avatarShape.name
                p[Keys.Corner] = next.avatarCorner
                p[Keys.HidePhone] = next.hidePhone
            }
            applyLauncherIcon(state.value.dynamicIcon)
        }
    }

    private fun applyLauncherIcon(dynamic: Boolean) {
        val pm = context.packageManager
        fun set(alias: String, enabled: Boolean) {
            val component = ComponentName(context, "com.pixelgram.android.$alias")
            val wanted = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (pm.getComponentEnabledSetting(component) != wanted) pm.setComponentEnabledSetting(component, wanted, PackageManager.DONT_KILL_APP)
        }
        set("LauncherDynamic", dynamic)
        set("LauncherDefault", !dynamic)
    }
}
