package com.pixelgram.android.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import java.io.File
import java.util.zip.GZIPInputStream

object EmojiRenderer {
    private class Entry(val path: String, val px: Int) {
        var drawable: LottieDrawable? = null
        var buffers: Array<Bitmap>? = null
        @Volatile var front = -1
        var next = 0
        var users = 0
        var duration = 1L
        var failed = false
        val started = SystemClock.uptimeMillis()
    }

    private val entries = LinkedHashMap<String, Entry>()
    private val thread = HandlerThread("emoji-render").apply { start() }
    private val worker = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var tickPosted = false
    val tick = mutableIntStateOf(0)

    private val loop = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            val active = synchronized(entries) { entries.values.filter { it.users > 0 } }
            active.forEach { render(it, now) }
            postTick()
            val keep = synchronized(entries) { entries.values.any { it.users > 0 } }
            if (keep) worker.postDelayed(this, FrameMs) else synchronized(entries) { running = false }
        }
    }

    private fun postTick() {
        synchronized(entries) {
            if (tickPosted) return
            tickPosted = true
        }
        main.post {
            synchronized(entries) { tickPosted = false }
            tick.intValue++
        }
    }

    private fun prepare(entry: Entry): Boolean {
        if (entry.drawable != null) return true
        if (entry.failed) return false
        val composition = runCatching {
            val bytes = File(entry.path).readBytes()
            val gzip = bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
            val json = if (gzip) GZIPInputStream(bytes.inputStream()).bufferedReader().use { it.readText() } else String(bytes)
            LottieCompositionFactory.fromJsonStringSync(json, null).value
        }.getOrNull()
        if (composition == null) {
            entry.failed = true
            return false
        }
        entry.drawable = LottieDrawable().apply {
            this.composition = composition
            setBounds(0, 0, entry.px, entry.px)
        }
        entry.duration = composition.duration.toLong().coerceAtLeast(1L)
        entry.buffers = Array(Buffers) { Bitmap.createBitmap(entry.px, entry.px, Bitmap.Config.ARGB_8888) }
        return true
    }

    private fun render(entry: Entry, now: Long) {
        runCatching {
            if (!prepare(entry)) return
            val drawable = entry.drawable ?: return
            val buffers = entry.buffers ?: return
            val target = buffers[entry.next]
            target.eraseColor(0)
            drawable.progress = ((now - entry.started) % entry.duration).toFloat() / entry.duration
            drawable.draw(Canvas(target))
            entry.front = entry.next
            entry.next = (entry.next + 1) % Buffers
        }
    }

    private fun free(entry: Entry) {
        worker.post {
            if (entry.users > 0) return@post
            entry.front = -1
            entry.drawable = null
            entry.buffers = null
        }
    }

    private fun acquire(path: String, px: Int): Entry {
        val id = "$path@$px"
        val entry = synchronized(entries) {
            (entries.remove(id) ?: Entry(path, px)).also {
                entries[id] = it
                it.users++
            }
        }
        if (entry.front < 0) worker.post { render(entry, SystemClock.uptimeMillis()); postTick() }
        synchronized(entries) {
            if (!running) {
                running = true
                worker.post(loop)
            }
        }
        return entry
    }

    private fun release(path: String, px: Int) {
        val evicted = ArrayList<Entry>()
        synchronized(entries) {
            entries["$path@$px"]?.let { it.users-- }
            val idle = entries.entries.filter { it.value.users <= 0 }
            val extra = idle.size - MaxIdle
            if (extra > 0) idle.take(extra).forEach {
                entries.remove(it.key)
                evicted += it.value
            }
        }
        evicted.forEach(::free)
    }

    private fun bucket(px: Int): Int = Sizes.firstOrNull { it >= px } ?: Sizes.last()

    @Composable
    fun Frame(path: String, modifier: Modifier, tint: Color?, maxPx: Int = Int.MAX_VALUE) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val px = if (size.width > 0) bucket(minOf(maxOf(size.width, size.height), maxPx)) else 0
        var entry by remember { mutableStateOf<Entry?>(null) }
        if (px > 0) {
            DisposableEffect(path, px) {
                entry = acquire(path, px)
                onDispose {
                    entry = null
                    release(path, px)
                }
            }
        }
        val filter = remember(tint) { tint?.let { ColorFilter.tint(it, BlendMode.SrcIn) } }
        Spacer(
            modifier.onSizeChanged { size = it }.drawBehind {
                tick.intValue
                val current = entry ?: return@drawBehind
                val index = current.front
                val buffers = current.buffers ?: return@drawBehind
                if (index < 0) return@drawBehind
                drawImage(
                    buffers[index].asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(current.px, current.px),
                    dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
                    colorFilter = filter,
                )
            },
        )
    }

    private const val Buffers = 2
    private const val FrameMs = 33L
    private const val MaxIdle = 4
    private val Sizes = intArrayOf(48, 72, 96, 128, 192, 256, 384, 512)
}
