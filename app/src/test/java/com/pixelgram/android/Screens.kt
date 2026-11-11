package com.pixelgram.android

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
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

    private fun shoot(name: String, graph: AppGraph = graph(), start: List<androidx.navigation3.runtime.NavKey> = listOf(com.pixelgram.android.nav.MainKey), popup: Boolean = false, before: (() -> Unit)? = null) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val appearance by graph.appearance.state.collectAsState()
            PixelTheme(appearance) {
                CompositionLocalProvider(LocalGraph provides graph) { AppNavigation(graph, start) }
            }
        }
        repeat(30) { rule.mainClock.advanceTimeBy(50) }
        before?.invoke()
        repeat(30) { rule.mainClock.advanceTimeBy(50) }
        val dir = File(System.getProperty("screens.dir") ?: "build/screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            val node = if (popup) rule.onNode(androidx.compose.ui.test.isRoot() and androidx.compose.ui.test.hasAnyDescendant(androidx.compose.ui.test.hasText("Reply"))) else rule.onRoot()
            node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun chats() = shoot("chats")

    @Test fun fabMenu() = shoot("fab_menu") { rule.onNodeWithContentDescription("New message").performClick() }

    @Test fun keys() {
        val graph = AppGraph(ApplicationProvider.getApplicationContext(), offline = true)
        graph.appearance.update { it.copy(themeMode = ThemeMode.Dark, dynamicColor = false) }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val appearance by graph.appearance.state.collectAsState()
            PixelTheme(appearance) { com.pixelgram.android.feature.auth.AuthFlow(graph.auth) }
        }
        repeat(30) { rule.mainClock.advanceTimeBy(50) }
        val dir = File(System.getProperty("screens.dir") ?: "build/screens").apply { mkdirs() }
        File(dir, "keys.png").outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun chat() = chatScene("chat")

    @Test fun messageMenu() = chatScene("message_menu", popup = true) {
        val node = rule.onAllNodes(androidx.compose.ui.test.hasText("Agree", substring = true)).onLast()
        node.performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(900)
        node.performTouchInput { up() }
    }

    private fun chatScene(name: String, popup: Boolean = false, action: (() -> Unit)? = null) {
        val graph = graph()
        val now = (System.currentTimeMillis() / 1000).toInt()
        fun message(id: Long, out: Boolean, text: String, reply: Long = 0) = TdApi.Message().apply {
            this.id = id
            chatId = 2
            senderId = TdApi.MessageSenderUser(if (out) 1 else 2)
            isOutgoing = out
            date = now - (10 - id.toInt()) * 60
            content = TdApi.MessageText(TdApi.FormattedText(text, emptyArray()), null, null)
            if (reply != 0L) replyTo = TdApi.MessageReplyToMessage(2, reply, null, 0, null, null, 0, null)
        }
        val track = message(10, false, "").apply {
            content = TdApi.MessageAudio(TdApi.Audio().apply {
                duration = 214
                title = "Midnight Shapes"
                performer = "Expressive Hours"
                fileName = "Expressive Hours - Midnight Shapes.mp3"
                mimeType = "audio/mpeg"
                audio = TdApi.File().apply { id = 700; local = TdApi.LocalFile(); remote = TdApi.RemoteFile() }
            }, TdApi.FormattedText("", emptyArray()))
        }
        fun doc(id: Long, fileId: Int, name: String) = message(id, false, "").apply {
            content = TdApi.MessageDocument(TdApi.Document().apply {
                fileName = name
                mimeType = "application/octet-stream"
                document = TdApi.File().apply { this.id = fileId; size = 52_000_000; local = TdApi.LocalFile(); remote = TdApi.RemoteFile() }
            }, TdApi.FormattedText("", emptyArray()))
        }
        val tgs = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "emoji.tgs")
        val lottie = """{"v":"5.7.4","fr":60,"ip":0,"op":60,"w":512,"h":512,"layers":[{"ty":4,"ind":1,"ip":0,"op":60,"st":0,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[256,256,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"shapes":[{"ty":"gr","it":[{"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[440,440]}},{"ty":"fl","c":{"a":0,"k":[1,0.72,0.1,1]},"o":{"a":0,"k":100}},{"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}]},{"ty":"gr","it":[{"ty":"rc","p":{"a":0,"k":[0,40]},"s":{"a":0,"k":[220,60]},"r":{"a":0,"k":30}},{"ty":"fl","c":{"a":0,"k":[0.35,0.2,0.05,1]},"o":{"a":0,"k":100}},{"ty":"tr","p":{"a":0,"k":[0,40]},"a":{"a":0,"k":[0,40]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}]}]}]}"""
        java.util.zip.GZIPOutputStream(tgs.outputStream()).use { it.write(lottie.toByteArray()) }
        val sticker = TdApi.Sticker().apply {
            id = 1; setId = 1; width = 512; height = 512; emoji = "😀"
            format = TdApi.StickerFormatTgs()
            fullType = TdApi.StickerFullTypeCustomEmoji(42, false)
            sticker = TdApi.File().apply { id = 900; local = TdApi.LocalFile().apply { path = tgs.absolutePath; isDownloadingCompleted = true }; remote = TdApi.RemoteFile() }
        }
        fun emojiMessage(id: Long, text: String, offsets: List<Int>) = message(id, false, "").apply {
            content = TdApi.MessageText(TdApi.FormattedText(text, offsets.map { TdApi.TextEntity(it, 2, TdApi.TextEntityTypeCustomEmoji(42)) }.toTypedArray()), null, null)
        }
        val quote = message(15, true, "").apply {
            val t = "Material You is the best\nAgree, and the shapes too"
            content = TdApi.MessageText(TdApi.FormattedText(t, arrayOf(TdApi.TextEntity(0, 24, TdApi.TextEntityTypeBlockQuote()))), null, null)
            replyTo = TdApi.MessageReplyToMessage(2, 7, null, 0, null, null, 0, null)
        }
        val history = listOf(
            quote,
            emojiMessage(14, "😀😀", listOf(0, 2)),
            emojiMessage(13, "Premium 😀 emoji", listOf(8)),
            doc(12, 801, "pixelGram-0.55.apk"),
            doc(11, 800, "Expressive Shapes.pdf"),
            track,
            message(9, false, "See you tomorrow 😀", 8).apply {
                interactionInfo = TdApi.MessageInteractionInfo().apply {
                    reactions = TdApi.MessageReactions(arrayOf(
                        TdApi.MessageReaction(TdApi.ReactionTypeEmoji("❤"), 2, true, null, arrayOf(TdApi.MessageSenderUser(1), TdApi.MessageSenderUser(2))),
                        TdApi.MessageReaction(TdApi.ReactionTypeEmoji("🔥"), 1, false, null, emptyArray()),
                    ), false, emptyArray(), false)
                }
            },
            message(8, true, "Sure, at 10?"),
            message(7, false, "Shall we meet tomorrow?"),
            message(6, true, "The new Material 3 Expressive shapes look great"),
            message(5, false, "Hi there 👋"),
        )
        graph.td.offlineReply = { function ->
            when (function) {
                is TdApi.GetChatHistory -> TdApi.Messages(history.size, history.toTypedArray())
                is TdApi.GetRepliedMessage -> history.firstOrNull { it.id == 8L }
                is TdApi.GetCustomEmojiStickers -> TdApi.Stickers(arrayOf(sticker))
                else -> null
            }
        }
        graph.td.emit(TdApi.UpdateFile(TdApi.File().apply {
            id = 801; size = 52_000_000
            local = TdApi.LocalFile().apply { isDownloadingActive = true; downloadedSize = 21_000_000 }
            remote = TdApi.RemoteFile()
        }))
        shoot(name, graph, listOf(com.pixelgram.android.nav.MainKey, com.pixelgram.android.nav.ChatKey(2)), popup) { action?.invoke(); repeat(10) { Thread.sleep(300); org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle(); rule.mainClock.advanceTimeBy(50) } }
    }

    @Test fun settingsTab() = shoot("settings_tab") { rule.onNodeWithContentDescription("Settings").performClick() }

    @Test fun contactsTab() {
        val graph = graph()
        graph.td.offlineReply = { function -> if (function is TdApi.GetContacts) TdApi.Users(3, longArrayOf(2, 3, 6)) else null }
        shoot("contacts_tab", graph) { rule.onNodeWithContentDescription("Contacts").performClick() }
    }

    @Test fun telegramIcons() {
        val graph = graph()
        graph.appearance.update { it.copy(iconPack = com.pixelgram.android.settings.IconPack.Telegram) }
        shoot("icons_telegram", graph) { rule.onNodeWithContentDescription("Settings").performClick() }
    }

    @Test fun sessions() = shoot("sessions", start = listOf(com.pixelgram.android.nav.MainKey, com.pixelgram.android.nav.SessionsKey))

    @Test fun search() = shoot("search", start = listOf(com.pixelgram.android.nav.MainKey, com.pixelgram.android.nav.SearchKey))

    @Test fun deleted() {
        val graph = graph()
        graph.td.emit(TdApi.UpdateUser(TdApi.User().apply {
            id = 3
            firstName = ""
            lastName = ""
            phoneNumber = ""
            status = TdApi.UserStatusEmpty()
            type = TdApi.UserTypeDeleted()
        }))
        shoot("deleted", graph, listOf(com.pixelgram.android.nav.MainKey, com.pixelgram.android.nav.ChatKey(3)))
    }

    @Test fun pixelSettings() = shoot("pixel_settings", start = listOf(com.pixelgram.android.nav.MainKey, com.pixelgram.android.nav.PixelSettingsKey()))

    @Test fun profile() = shoot("profile", start = listOf(com.pixelgram.android.nav.MainKey, com.pixelgram.android.nav.ProfileKey(2, 2)))

    @Test fun story() {
        val graph = graph()
        val dir = File(System.getProperty("screens.dir") ?: "build/screens").apply { mkdirs() }
        val image = File(dir, "story-source.png")
        val bitmap = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawPaint(android.graphics.Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, 360f, 640f, 0xFF6750A4.toInt(), 0xFFFFB4AB.toInt(), android.graphics.Shader.TileMode.CLAMP) })
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        graph.td.offlineReply = { function ->
            when (function) {
                is TdApi.GetStory -> TdApi.Story().apply {
                    id = function.storyId
                    posterChatId = function.storyPosterChatId
                    date = (System.currentTimeMillis() / 1000).toInt() - 600
                    content = TdApi.StoryContentPhoto(TdApi.Photo(false, null, arrayOf(TdApi.PhotoSize("x", TdApi.File().apply { id = 900; local = TdApi.LocalFile(); remote = TdApi.RemoteFile() }, 360, 640, IntArray(0)))))
                    caption = TdApi.FormattedText("Expressive shapes everywhere", emptyArray())
                }
                is TdApi.DownloadFile -> TdApi.File().apply {
                    id = function.fileId
                    local = TdApi.LocalFile(image.absolutePath, true, true, false, true, 0, 0, 0)
                    remote = TdApi.RemoteFile()
                }
                else -> null
            }
        }
        com.pixelgram.android.feature.stories.StoryViewer.open(2L, listOf(1L, 2L, 3L, 6L))
        shoot("story", graph)
        com.pixelgram.android.feature.stories.StoryViewer.chatId = null
    }

    private fun withMusic(): AppGraph {
        val graph = graph()
        val audios = listOf("Midnight Shapes" to "Expressive Hours", "Cookie Waves" to "Material Band", "Soft Burst" to "Pill Club").mapIndexed { i, (title, performer) ->
            TdApi.Audio().apply {
                duration = 200 + i * 20
                this.title = title
                this.performer = performer
                fileName = "$performer - $title.mp3"
                mimeType = "audio/mpeg"
                audio = TdApi.File().apply {
                    id = 500 + i
                    local = TdApi.LocalFile()
                    remote = TdApi.RemoteFile()
                }
            }
        }
        graph.music.play(audios, 0, "Alice")
        com.pixelgram.android.feature.player.FullPlayer.open = true
        return graph
    }

    @Test fun chatsMusic() {
        val graph = withMusic()
        com.pixelgram.android.feature.player.FullPlayer.open = false
        shoot("chats_music", graph)
    }

    @Test fun player() {
        com.pixelgram.android.feature.player.FullPlayer.view = com.pixelgram.android.feature.player.PlayerView.Cover
        shoot("player", withMusic())
    }

    @Test fun playerLyrics() {
        val text = listOf("Shapes are dancing in the night", "Every corner morphs to light", "Cookie waves and pills that glow", "Expressive hours, soft and slow", "Hold the beat and let it go", "Midnight shapes are all we know")
        val lines = text.mapIndexed { i, t -> com.pixelgram.android.music.LyricLine(i * 3f - 1.2f, t) }
        com.pixelgram.android.music.Lyrics.prime("Expressive Hours", "Midnight Shapes", com.pixelgram.android.music.LyricsResult.Synced(lines))
        com.pixelgram.android.feature.player.FullPlayer.view = com.pixelgram.android.feature.player.PlayerView.Lyrics
        shoot("player_lyrics", withMusic())
    }

    @Test fun playerQueue() {
        com.pixelgram.android.feature.player.FullPlayer.view = com.pixelgram.android.feature.player.PlayerView.Queue
        shoot("player_queue", withMusic())
    }
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
        listOf(1L, 2L, 3L, 6L).forEachIndexed { i, id ->
            td.emit(TdApi.UpdateChatActiveStories(TdApi.ChatActiveStories(id, TdApi.StoryListMain(), 100L - i, true, if (i == 3) 2 else 0, arrayOf(TdApi.StoryInfo(1, now, false, false), TdApi.StoryInfo(2, now, false, false)))))
        }
        td.emit(TdApi.UpdateUnreadChatCount(TdApi.ChatListMain(), 10, 5, 3, 2, 1))
    }

    private fun user(id: Long, name: String) = TdApi.User().apply {
        this.id = id
        firstName = name
        lastName = ""
        phoneNumber = "7999000000$id"
        status = if (id == 2L) TdApi.UserStatusOnline(0) else TdApi.UserStatusOffline(0)
        if (id == 2L) usernames = TdApi.Usernames().apply { activeUsernames = arrayOf("alice", "alice_md"); disabledUsernames = arrayOf(); editableUsername = "alice" }
        type = TdApi.UserTypeRegular()
    }
}
