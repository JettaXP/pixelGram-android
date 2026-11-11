package com.pixelgram.android

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.pixelgram.android.nav.AppNavigation
import com.pixelgram.android.settings.ThemeMode
import com.pixelgram.android.ui.theme.PixelTheme
import org.drinkless.tdlib.TdApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi", application = android.app.Application::class)
class Screens {
    @get:Rule val rule = createComposeRule()

    private fun graph(): AppGraph {
        val graph = AppGraph(ApplicationProvider.getApplicationContext(), offline = true)
        graph.appearance.update { it.copy(themeMode = ThemeMode.Dark, dynamicColor = false) }
        DemoData.fill(graph)
        return graph
    }

    private fun shoot(name: String, graph: AppGraph = graph(), before: (() -> Unit)? = null) {
        rule.setContent {
            val appearance by graph.appearance.state.collectAsState()
            PixelTheme(appearance) {
                CompositionLocalProvider(LocalGraph provides graph) { AppNavigation(graph) }
            }
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        before?.invoke()
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        val dir = File(System.getProperty("screens.dir") ?: "build/screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun chats() = shoot("chats")
}

object DemoData {
    fun fill(graph: AppGraph) {
        val td = graph.td
        td.emit(TdApi.UpdateOption("my_id", TdApi.OptionValueInteger(1)))
        val names = listOf("Jetta", "Alice", "Bob", "Material Design", "Family", "Kotlin", "News", "Compose", "Weekend", "Music")
        names.forEachIndexed { i, name ->
            val id = (i + 1).toLong()
            td.emit(TdApi.UpdateUser(user(id, name)))
        }
        val now = (System.currentTimeMillis() / 1000).toInt()
        val texts = listOf("Saved", "See you tomorrow 😀", "Rocket launch at 9 🚀", "Expressive shapes 💜", "Dinner at 7? 🍕", "Coroutines are neat", "Breaking news", "Recomposition 🤔", "Party time 🥳", "New track")
        names.forEachIndexed { i, name ->
            val id = (i + 1).toLong()
            val chat = TdApi.Chat().apply {
                this.id = id
                title = name
                type = TdApi.ChatTypePrivate(id)
                notificationSettings = TdApi.ChatNotificationSettings().apply { muteFor = if (i == 6) Int.MAX_VALUE else 0 }
                positions = emptyArray()
                unreadCount = listOf(0, 0, 2, 4, 0, 0, 7, 0, 1, 0)[i]
            }
            td.emit(TdApi.UpdateNewChat(chat))
            val message = TdApi.Message().apply {
                this.id = 100L
                chatId = id
                senderId = TdApi.MessageSenderUser(if (i % 3 == 0) 1 else id)
                isOutgoing = i % 3 == 0
                date = now - i * 3600
                content = TdApi.MessageText(TdApi.FormattedText(texts[i], emptyArray()), null, null)
            }
            td.emit(TdApi.UpdateChatLastMessage(id, message, arrayOf(TdApi.ChatPosition(TdApi.ChatListMain(), 1000L - i, i == 0, null))))
        }
        td.emit(TdApi.UpdateChatFolders(arrayOf(
            TdApi.ChatFolderInfo(1, TdApi.ChatFolderName(TdApi.FormattedText("Personal", emptyArray()), false), TdApi.ChatFolderIcon("Private"), 0, false, false),
            TdApi.ChatFolderInfo(2, TdApi.ChatFolderName(TdApi.FormattedText("Work", emptyArray()), false), TdApi.ChatFolderIcon("Work"), 0, false, false),
        ), 0, false))
        td.emit(TdApi.UpdateUnreadChatCount(TdApi.ChatListMain(), 10, 5, 3, 2, 1))
    }

    private fun user(id: Long, name: String) = TdApi.User().apply {
        this.id = id
        firstName = name
        lastName = ""
        phoneNumber = "7999000000$id"
        status = if (id == 2L) TdApi.UserStatusOnline(0) else TdApi.UserStatusOffline(0)
        type = TdApi.UserTypeRegular()
    }
}
