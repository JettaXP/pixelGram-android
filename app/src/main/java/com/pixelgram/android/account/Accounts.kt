package com.pixelgram.android.account

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable

@Immutable
data class SavedAccount(val id: Int, val name: String, val phone: String, val userId: Long)

object Accounts {
    private const val Prefs = "accounts"

    private fun prefs(context: Context) = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)

    fun active(context: Context): Int = prefs(context).getInt("active", 0)

    fun ids(context: Context): List<Int> =
        prefs(context).getString("ids", null)?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.isNotEmpty() } ?: listOf(0)

    fun all(context: Context): List<SavedAccount> = ids(context).map { id ->
        val p = prefs(context)
        SavedAccount(id, p.getString("name_$id", "").orEmpty(), p.getString("phone_$id", "").orEmpty(), p.getLong("user_$id", 0L))
    }

    fun databaseDir(id: Int): String = if (id == 0) "tdlib" else "tdlib-$id"

    fun filesDir(id: Int): String = if (id == 0) "tdlib-files" else "tdlib-files-$id"

    fun remember(context: Context, name: String, phone: String, userId: Long) {
        val id = active(context)
        prefs(context).edit().putString("name_$id", name).putString("phone_$id", phone).putLong("user_$id", userId).apply()
    }

    fun add(context: Context) {
        val ids = ids(context)
        val next = (ids.maxOrNull() ?: 0) + 1
        prefs(context).edit().putString("ids", (ids + next).joinToString(",")).apply()
        switchTo(context, next)
    }

    fun forgetActive(context: Context) {
        val id = active(context)
        val rest = ids(context) - id
        val p = prefs(context).edit().remove("name_$id").remove("phone_$id").remove("user_$id")
        if (rest.isEmpty()) {
            p.putString("ids", "0").putInt("active", 0).apply()
        } else {
            p.putString("ids", rest.joinToString(",")).apply()
            switchTo(context, rest.first())
        }
    }

    fun switchTo(context: Context, id: Int) {
        prefs(context).edit().putInt("active", id).commit()
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launch)
        Runtime.getRuntime().exit(0)
    }
}
