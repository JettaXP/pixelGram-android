package com.pixelgram.android.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.FontFamily
import com.pixelgram.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

object AppFonts {
    const val GoogleSansKey = "google_sans"
    const val FlexKey = "google_sans_flex"
    const val SystemKey = "system"
    const val CustomKey = "custom"

    const val EmojiSystem = "system"
    const val EmojiNoto = "noto"
    const val EmojiNoto3D = "noto3d"
    const val EmojiApple = "apple"

    val progress = MutableStateFlow<Pair<String, Float>?>(null)

    private val GoogleSansFonts = listOf(R.font.google_sans_regular to 400, R.font.google_sans_medium to 500, R.font.google_sans_semibold to 600, R.font.google_sans_bold to 700)
    private val FlexFonts = listOf(R.font.google_sans_flex_regular to 400, R.font.google_sans_flex_medium to 500, R.font.google_sans_flex_semibold to 600, R.font.google_sans_flex_bold to 700)

    fun customFile(context: Context) = File(context.filesDir, "fonts/custom.ttf")

    fun emojiFile(context: Context, style: String) = File(context.filesDir, "emoji/$style.ttf")

    fun emojiReady(context: Context, style: String) = style == EmojiSystem || emojiFile(context, style).let { it.isFile && it.length() > 0 }

    fun family(context: Context, font: String, emoji: String): FontFamily {
        val emojiPath = emojiFile(context, emoji).takeIf { emoji != EmojiSystem && it.isFile }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { build(context, font, emojiPath) }.getOrNull()?.let { return FontFamily(androidx.compose.ui.text.font.Typeface(it)) }
        }
        return when (font) {
            SystemKey -> FontFamily.Default
            FlexKey -> FontFamily(FlexFonts.map { (res, w) -> androidx.compose.ui.text.font.Font(res, androidx.compose.ui.text.font.FontWeight(w)) })
            CustomKey -> customFile(context).takeIf { it.isFile }?.let { runCatching { FontFamily(androidx.compose.ui.text.font.Typeface(Typeface.createFromFile(it))) }.getOrNull() } ?: GoogleSans
            else -> GoogleSans
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun build(context: Context, font: String, emoji: File?): Typeface? {
        val res = context.resources
        fun resFamily(list: List<Pair<Int, Int>>): android.graphics.fonts.FontFamily {
            val fonts = list.map { (id, w) -> android.graphics.fonts.Font.Builder(res, id).setWeight(w).build() }
            return android.graphics.fonts.FontFamily.Builder(fonts.first()).apply { fonts.drop(1).forEach { addFont(it) } }.build()
        }
        val sans = resFamily(GoogleSansFonts)
        val main = when (font) {
            SystemKey -> null
            FlexKey -> resFamily(FlexFonts)
            CustomKey -> customFile(context).takeIf { it.isFile }?.let { file ->
                runCatching { android.graphics.fonts.FontFamily.Builder(android.graphics.fonts.Font.Builder(file).build()).build() }.getOrNull()
            } ?: sans
            else -> sans
        }
        val emojiFamily = emoji?.let { file -> runCatching { android.graphics.fonts.FontFamily.Builder(android.graphics.fonts.Font.Builder(file).build()).build() }.getOrNull() }
        if (main == null && emojiFamily == null) return null
        val builder = if (main != null) Typeface.CustomFallbackBuilder(main) else Typeface.CustomFallbackBuilder(emojiFamily!!)
        if (main != null && main !== sans) builder.addCustomFallback(sans)
        if (main != null && emojiFamily != null) builder.addCustomFallback(emojiFamily)
        builder.setSystemFallback("sans-serif")
        return builder.build()
    }

    private fun sourceUrl(style: String): String? = when (style) {
        EmojiApple -> "https://github.com/samuelngs/apple-emoji-ttf/releases/latest/download/AppleColorEmoji-Linux.ttf"
        EmojiNoto3D -> "https://media.githubusercontent.com/media/googlefonts/noto-emoji/main/3D/fonts/Noto-3D-watch.ttf"
        EmojiNoto -> notoUrl()
        else -> null
    }

    private fun notoUrl(): String? = runCatching {
        val connection = URI("https://fonts.googleapis.com/css2?family=Noto+Color+Emoji").toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/4.0")
        val css = connection.inputStream.bufferedReader().use { it.readText() }
        Regex("url\\((https://[^)]+\\.ttf)\\)").find(css)?.groupValues?.get(1)
    }.getOrNull()

    suspend fun download(context: Context, style: String): Boolean = withContext(Dispatchers.IO) {
        if (style == EmojiSystem) return@withContext true
        val target = emojiFile(context, style)
        if (target.isFile && target.length() > 0) return@withContext true
        val url = sourceUrl(style) ?: return@withContext false
        val part = File(target.parentFile, "$style.part").apply { parentFile?.mkdirs() }
        val ok = runCatching {
            var connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            val total = connection.contentLengthLong.coerceAtLeast(1L)
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        progress.value = style to (done.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
            }
            true
        }.getOrDefault(false)
        progress.value = null
        if (!ok || !renders(part)) {
            part.delete()
            return@withContext false
        }
        part.renameTo(target)
    }

    private fun renders(file: File): Boolean = runCatching {
        val typeface = Typeface.createFromFile(file)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            this.typeface = typeface
            textSize = 48f
        }
        Canvas(bitmap).drawText("😀", 4f, 52f, paint)
        val pixels = IntArray(64 * 64)
        bitmap.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        pixels.any { (it ushr 24) != 0 && (it and 0xFFFFFF) != 0 }
    }.getOrDefault(false)

    private var emojiTypeface: Pair<String, Typeface?>? = null

    fun emojiTypeface(context: Context, style: String): Typeface? {
        if (style == EmojiSystem) return null
        emojiTypeface?.takeIf { it.first == style }?.let { return it.second }
        val typeface = emojiFile(context, style).takeIf { it.isFile }?.let { file ->
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Typeface.CustomFallbackBuilder(android.graphics.fonts.FontFamily.Builder(android.graphics.fonts.Font.Builder(file).build()).build()).setSystemFallback("sans-serif").build()
                else Typeface.createFromFile(file)
            }.getOrNull()
        }
        emojiTypeface = style to typeface
        return typeface
    }

    fun applyToPicker(root: android.view.View, typeface: Typeface?) {
        if (typeface == null) return
        runCatching { androidx.emoji2.emojipicker.EmojiPickerView::class.java.getDeclaredField("Companion").get(null)?.let { companion -> companion.javaClass.getMethod("setEmojiCompatLoaded\$emoji2_emojipicker", Boolean::class.javaPrimitiveType).invoke(companion, false) } }
        val cls = runCatching { Class.forName("androidx.emoji2.emojipicker.EmojiView") }.getOrNull() ?: return
        val field = runCatching { cls.getDeclaredField("textPaint").apply { isAccessible = true } }.getOrNull() ?: return
        val getter = runCatching { cls.getMethod("getEmoji") }.getOrNull() ?: return
        val setter = runCatching { cls.getMethod("setEmoji", CharSequence::class.java) }.getOrNull() ?: return
        fun walk(view: android.view.View) {
            if (cls.isInstance(view)) {
                val paint = field.get(view) as? android.text.TextPaint ?: return
                if (paint.typeface !== typeface) {
                    paint.typeface = typeface
                    (getter.invoke(view) as? CharSequence)?.let { setter.invoke(view, it.toString()) }
                }
            } else if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) walk(view.getChildAt(i))
            }
        }
        walk(root)
    }

    fun importFont(context: Context, uri: android.net.Uri): Boolean = runCatching {
        val target = customFile(context).apply { parentFile?.mkdirs() }
        context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        Typeface.createFromFile(target)
        true
    }.getOrDefault(false)
}
