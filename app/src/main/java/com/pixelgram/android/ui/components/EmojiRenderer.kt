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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable

object EmojiRenderer {
    private class Entry(val composition: LottieComposition, val px: Int) {
        val drawable = LottieDrawable().apply {
            composition = this@Entry.composition
            setBounds(0, 0, px, px)
        }
        val buffers = Array(Buffers) { Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888) }
        @Volatile var front = -1
        var next = 0
        var users = 0
        var started = SystemClock.uptimeMillis()
        val duration = composition.duration.toLong().coerceAtLeast(1L)
    }

    private val entries = HashMap<String, Entry>()
    private val thread = HandlerThread("emoji-render").apply { start() }
    private val worker = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private var running = false
    val tick = mutableIntStateOf(0)

    private val loop = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            val active = synchronized(entries) { entries.values.filter { it.users > 0 } }
            active.forEach { render(it, now) }
            main.post { tick.intValue++ }
            val keep = synchronized(entries) { entries.values.any { it.users > 0 } }
            if (keep) worker.postDelayed(this, FrameMs) else synchronized(entries) { running = false }
        }
    }

    private fun render(entry: Entry, now: Long) {
        runCatching {
            val target = entry.buffers[entry.next]
            target.eraseColor(0)
            entry.drawable.progress = ((now - entry.started) % entry.duration).toFloat() / entry.duration
            entry.drawable.draw(Canvas(target))
            entry.front = entry.next
            entry.next = (entry.next + 1) % Buffers
        }
    }

    fun composition(json: String, key: String): LottieComposition? =
        runCatching { LottieCompositionFactory.fromJsonStringSync(json, key).value }.getOrNull()

    private fun acquire(key: String, composition: LottieComposition, px: Int): Entry {
        val entry = synchronized(entries) {
            entries.getOrPut("$key@$px") { Entry(composition, px) }.also { it.users++ }
        }
        if (entry.front < 0) worker.post { render(entry, SystemClock.uptimeMillis()); main.post { tick.intValue++ } }
        synchronized(entries) {
            if (!running) {
                running = true
                worker.post(loop)
            }
        }
        return entry
    }

    private fun release(key: String, px: Int) {
        synchronized(entries) {
            val id = "$key@$px"
            val entry = entries[id] ?: return
            entry.users--
            if (entry.users <= 0 && entries.size > MaxIdle) entries.remove(id)
        }
    }

    private fun bucket(px: Int): Int = Sizes.firstOrNull { it >= px } ?: Sizes.last()

    @Composable
    fun Frame(key: String, composition: LottieComposition, modifier: Modifier, tint: Color?) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val px = if (size.width > 0) bucket(maxOf(size.width, size.height)) else 0
        var entry by remember { mutableStateOf<Entry?>(null) }
        if (px > 0) {
            DisposableEffect(key, px) {
                entry = acquire(key, composition, px)
                onDispose {
                    entry = null
                    release(key, px)
                }
            }
        }
        val filter = remember(tint) { tint?.let { ColorFilter.tint(it, BlendMode.SrcIn) } }
        Spacer(
            modifier.onSizeChanged { size = it }.drawBehind {
                tick.intValue
                val current = entry ?: return@drawBehind
                val index = current.front
                if (index < 0) return@drawBehind
                drawImage(
                    current.buffers[index].asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(current.px, current.px),
                    dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
                    colorFilter = filter,
                )
            },
        )
    }

    private const val Buffers = 3
    private const val FrameMs = 33L
    private const val MaxIdle = 96
    private val Sizes = intArrayOf(32, 48, 64, 96, 128, 192, 256, 384, 512)
}
