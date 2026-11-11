package com.pixelgram.android.music

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

class LyricWord(val time: Float, val text: String, val end: Float = Float.NaN)

class LyricLine(val time: Float, val text: String, val words: List<LyricWord> = emptyList())

sealed interface LyricsResult {
    data object Loading : LyricsResult
    data object NotFound : LyricsResult
    data object Instrumental : LyricsResult
    class Plain(val lines: List<String>) : LyricsResult
    class Synced(val lines: List<LyricLine>) : LyricsResult
}

object Lyrics {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, LyricsResult>()

    fun cached(artist: String, title: String): LyricsResult? = cache[key(artist, title)]

    internal fun prime(artist: String, title: String, result: LyricsResult) {
        cache[key(artist, title)] = result
    }

    fun load(artist: String, title: String, duration: Int): LyricsResult {
        val key = key(artist, title)
        cache[key]?.let { return it }
        if (title.isBlank()) return LyricsResult.NotFound
        failed.set(false)
        better(artist, title, duration)?.let {
            cache[key] = it
            return it
        }
        val base = "https://lrclib.net/api/get?artist_name=${encode(artist)}&track_name=${encode(title)}"
        val direct = listOfNotNull(
            if (duration > 0) request("$base&duration=$duration") else null,
            request(base),
        ).map(::parse).firstOrNull { it.useful() }
        val result = direct ?: search(artist, title) ?: LyricsResult.NotFound
        if (result != LyricsResult.NotFound || !failed.get()) cache[key] = result
        return result
    }

    private fun better(artist: String, title: String, duration: Int): LyricsResult? {
        val url = "https://lyrics-api.boidu.dev/getLyrics?s=${encode(title)}&a=${encode(artist)}" + if (duration > 0) "&d=$duration" else ""
        val body = request(url, "Mozilla/5.0") ?: return null
        val ttml = field(body, "ttml")?.takeIf { it.isNotBlank() } ?: return null
        val lines = runCatching { parseTtml(ttml) }.getOrNull() ?: return null
        return LyricsResult.Synced(lines).takeIf { it.useful() }
    }

    internal fun parseTtml(ttml: String): List<LyricLine> {
        val parser = android.util.Xml.newPullParser()
        parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(java.io.StringReader(ttml))
        val lines = mutableListOf<LyricLine>()
        var lineStart = Float.NaN
        val words = mutableListOf<LyricWord>()
        val text = StringBuilder()
        var background = 0
        var spanDepth = 0
        var wordStart = Float.NaN
        var wordEnd = Float.NaN
        val wordText = StringBuilder()
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                    "p" -> {
                        lineStart = ttmlTime(parser.getAttributeValue(null, "begin"))
                        words.clear()
                        text.clear()
                    }
                    "span" -> {
                        spanDepth++
                        val role = parser.getAttributeValue(null, "ttm:role")
                        if (role == "x-bg" || background > 0) background++
                        else if (!lineStart.isNaN()) {
                            wordStart = ttmlTime(parser.getAttributeValue(null, "begin"))
                            wordEnd = ttmlTime(parser.getAttributeValue(null, "end"))
                            wordText.clear()
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> if (!lineStart.isNaN() && background == 0) {
                    if (spanDepth > 0) wordText.append(parser.text) else if (parser.text.isNotEmpty() && parser.text.isBlank() && text.isNotEmpty()) text.append(' ')
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.name) {
                    "span" -> {
                        spanDepth--
                        if (background > 0) background--
                        else if (!wordStart.isNaN() && wordText.isNotBlank()) {
                            val piece = wordText.toString().trim()
                            words += LyricWord(wordStart, piece, wordEnd)
                            text.append(piece)
                            wordStart = Float.NaN
                        }
                    }
                    "p" -> {
                        val line = text.toString().replace(Regex("\\s+"), " ").trim()
                        if (!lineStart.isNaN() && line.isNotEmpty()) lines += LyricLine(lineStart, line, words.toList())
                        lineStart = Float.NaN
                    }
                }
            }
            event = parser.next()
        }
        return lines.sortedBy { it.time }
    }

    private fun ttmlTime(value: String?): Float {
        if (value.isNullOrBlank()) return Float.NaN
        val parts = value.trim().removeSuffix("s").split(':')
        return runCatching { parts.fold(0f) { acc, part -> acc * 60f + part.toFloat() } }.getOrDefault(Float.NaN)
    }

    private fun search(artist: String, title: String): LyricsResult? {
        val queries = listOfNotNull(
            "track_name=${encode(title)}" + if (artist.isNotBlank()) "&artist_name=${encode(artist)}" else "",
            "q=${encode("$artist $title".trim())}",
        )
        val wanted = normalize(title)
        val candidates = queries.asSequence()
            .mapNotNull { request("https://lrclib.net/api/search?$it") }
            .flatMap { objects(it) }
            .filter { item -> field(item, "trackName")?.let { normalize(it) == wanted || normalize(it).contains(wanted) } == true }
            .map(::parse)
            .filter { it.useful() }
            .toList()
        return candidates.firstOrNull { it is LyricsResult.Synced }
            ?: candidates.firstOrNull { it is LyricsResult.Instrumental }
            ?: candidates.firstOrNull()
    }

    private fun LyricsResult.useful(): Boolean = when (this) {
        LyricsResult.Instrumental -> true
        is LyricsResult.Synced -> lines.count { it.text.isNotBlank() } >= MinLines
        is LyricsResult.Plain -> lines.count { it.isNotBlank() } >= MinLines
        else -> false
    }

    private fun objects(body: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var start = -1
        var quoted = false
        var escaped = false
        body.forEachIndexed { i, c ->
            when {
                escaped -> escaped = false
                c == '\\' && quoted -> escaped = true
                c == '"' -> quoted = !quoted
                quoted -> Unit
                c == '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                c == '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) out += body.substring(start, i + 1)
                }
            }
        }
        return out
    }

    private val failed = ThreadLocal.withInitial { false }

    private fun request(url: String, agent: String? = null): String? = runCatching {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("User-Agent", agent ?: "pixelGram Android/${com.pixelgram.android.BuildConfig.VERSION_NAME} (https://github.com/JettaXP/pixelGram-android)")
        try {
            val code = connection.responseCode
            if (code != 200 && code != 404) failed.set(true)
            if (code != 200) return@runCatching null
            connection.inputStream.bufferedReader().use { it.readText() }.takeIf { it.isNotBlank() && it != "[]" }
        } finally {
            connection.disconnect()
        }
    }.onFailure { failed.set(true) }.getOrNull()

    private fun parse(body: String): LyricsResult {
        val instrumental = Regex(""""instrumental"\s*:\s*true""").containsMatchIn(body)
        val synced = field(body, "syncedLyrics")
        val plain = field(body, "plainLyrics")
        return when {
            !synced.isNullOrBlank() -> parseSynced(synced).takeIf { it.isNotEmpty() }?.let { LyricsResult.Synced(it) }
                ?: LyricsResult.Plain(synced.lines())
            !plain.isNullOrBlank() -> LyricsResult.Plain(plain.lines())
            instrumental -> LyricsResult.Instrumental
            else -> LyricsResult.NotFound
        }
    }

    private fun parseSynced(text: String): List<LyricLine> {
        val tag = Regex("""\[(\d+):(\d+(?:\.\d+)?)]""")
        return text.lines().flatMap { line ->
            val stamps = tag.findAll(line).toList()
            if (stamps.isEmpty()) return@flatMap emptyList()
            val body = line.substring(stamps.last().range.last + 1).trim()
            val wordTag = Regex("""<(\d+):(\d+(?:\.\d+)?)>""")
            val words = mutableListOf<LyricWord>()
            val marks = wordTag.findAll(body).toList()
            marks.forEachIndexed { i, mark ->
                val end = marks.getOrNull(i + 1)?.range?.first ?: body.length
                val piece = body.substring(mark.range.last + 1, end)
                if (piece.isNotEmpty()) words += LyricWord(mark.groupValues[1].toInt() * 60 + mark.groupValues[2].toFloat(), piece)
            }
            val text = body.replace(wordTag, "").replace(Regex("\\s+"), " ").trim()
            stamps.map { stamp -> LyricLine(stamp.groupValues[1].toInt() * 60 + stamp.groupValues[2].toFloat(), text, words) }
        }.sortedBy { it.time }
    }

    private fun field(body: String, name: String): String? {
        val start = Regex(""""$name"\s*:\s*""").find(body) ?: return null
        var i = start.range.last + 1
        if (i >= body.length || body[i] != '"') return null
        i++
        val out = StringBuilder()
        while (i < body.length) {
            val c = body[i]
            when {
                c == '"' -> return out.toString()
                c == '\\' && i + 1 < body.length -> {
                    when (val next = body[i + 1]) {
                        'n' -> out.append('\n')
                        'r' -> Unit
                        't' -> out.append('\t')
                        'u' -> if (i + 5 < body.length) {
                            body.substring(i + 2, i + 6).toIntOrNull(16)?.let { out.append(it.toChar()) }
                            i += 4
                        }
                        else -> out.append(next)
                    }
                    i += 2
                    continue
                }
                else -> out.append(c)
            }
            i++
        }
        return null
    }

    private fun normalize(value: String) = value.lowercase().filter { it.isLetterOrDigit() }

    private fun key(artist: String, title: String) = "${artist.lowercase()}\u0000${title.lowercase()}"

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}

private const val MinLines = 3
