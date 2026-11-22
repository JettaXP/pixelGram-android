package com.pixelgram.android.feature.settings

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.PixelSwitch
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.segmentPosition
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.util.Locale

object AppLanguage {
    private const val Prefs = "app_language"
    private const val Key = "tag"

    fun saved(context: Context): String = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(Key, "").orEmpty()

    fun wrap(base: Context): Context {
        val tag = saved(base)
        if (tag.isEmpty() || Build.VERSION.SDK_INT >= 33) return base
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(config)
    }

    fun apply(context: Context, tag: String) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putString(Key, tag).apply()
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) android.os.LocaleList.getEmptyLocaleList() else android.os.LocaleList.forLanguageTags(tag)
        } else {
            (context as? Activity)?.recreate()
        }
    }
}

@Composable
fun LanguageSection() {
    val context = LocalContext.current
    var current by remember { mutableStateOf(AppLanguage.saved(context)) }
    val options = listOf("" to stringResource(R.string.language_system), "en" to "English", "ru" to "Русский")
    SettingsGroup {
        options.forEachIndexed { index, (tag, label) ->
            SettingsItem(
                title = label,
                position = segmentPosition(index, options.size),
                icon = Icons.Rounded.Language,
                onClick = {
                    current = tag
                    AppLanguage.apply(context, tag)
                },
            ) {
                RadioButton(selected = current == tag, onClick = null)
            }
        }
    }
}
