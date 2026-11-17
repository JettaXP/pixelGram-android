package com.pixelgram.android.feature.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import java.io.File
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Adjust(
    val enhance: Float = 0f,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val fade: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val sharpen: Float = 0f,
    val shadowsTint: Int = 0,
    val highlightsTint: Int = 0,
) {
    val neutral get() = this == Adjust()
}

enum class BlurMode { Off, Radial, Linear }

data class BlurSettings(val mode: BlurMode = BlurMode.Off, val x: Float = 0.5f, val y: Float = 0.5f, val size: Float = 0.25f, val angle: Float = 0f)

data class Curves(
    val all: List<Float> = Identity,
    val red: List<Float> = Identity,
    val green: List<Float> = Identity,
    val blue: List<Float> = Identity,
) {
    val neutral get() = all == Identity && red == Identity && green == Identity && blue == Identity

    companion object {
        val Identity = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    }
}

enum class PaintTool { Pen, Arrow, Marker, Neon, Blur, Mosaic, Eraser }

enum class ShapeKind { Circle, Rectangle, Star, Bubble, Arrow }

sealed interface PaintItem {
    data class Stroke(val points: List<Pair<Float, Float>>, val color: Int, val width: Float, val tool: PaintTool) : PaintItem
    data class Shape(val kind: ShapeKind, val left: Float, val top: Float, val right: Float, val bottom: Float, val color: Int, val width: Float) : PaintItem
    data class Label(val text: String, val x: Float, val y: Float, val color: Int, val size: Float, val align: Int = 1, val style: Int = 0, val font: Int = 0, val rotation: Float = 0f) : PaintItem
    data class Sticker(val path: String, val x: Float, val y: Float, val size: Float, val rotation: Float = 0f) : PaintItem
}

object StickerBitmaps {
    private val cache = object : android.util.LruCache<String, Bitmap>(12) {}

    fun get(path: String): Bitmap? {
        cache.get(path)?.let { return it }
        val bitmap = runCatching {
            if (path.endsWith(".tgs", ignoreCase = true)) {
                val bytes = File(path).readBytes()
                val gzip = bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
                val json = if (gzip) java.util.zip.GZIPInputStream(bytes.inputStream()).bufferedReader().use { it.readText() } else String(bytes)
                val composition = com.airbnb.lottie.LottieCompositionFactory.fromJsonStringSync(json, null).value ?: return null
                val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                com.airbnb.lottie.LottieDrawable().apply {
                    this.composition = composition
                    setBounds(0, 0, 512, 512)
                    progress = 0f
                    draw(Canvas(out))
                }
                out
            } else {
                BitmapFactory.decodeFile(path)
            }
        }.getOrNull() ?: return null
        cache.put(path, bitmap)
        return bitmap
    }
}

data class PhotoEdit(
    val rotation: Int = 0,
    val flip: Boolean = false,
    val straighten: Float = 0f,
    val crop: RectF = RectF(0f, 0f, 1f, 1f),
    val adjust: Adjust = Adjust(),
    val blur: BlurSettings = BlurSettings(),
    val curves: Curves = Curves(),
    val paint: List<PaintItem> = emptyList(),
) {
    val untouched get() = rotation == 0 && !flip && straighten == 0f && crop == RectF(0f, 0f, 1f, 1f) && adjust.neutral && blur.mode == BlurMode.Off && curves.neutral && paint.isEmpty()
}

object PhotoRenderer {
    const val SdSide = 1280
    const val HdSide = 2560
    const val PreviewSide = 1080

    fun decode(path: String, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val raw = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }) ?: return null
        val orientation = runCatching { ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        val scale = min(1f, maxSide.toFloat() / max(raw.width, raw.height))
        if (scale < 1f) matrix.postScale(scale, scale)
        if (matrix.isIdentity) return raw
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also { if (it !== raw) raw.recycle() }
    }

    fun geometry(source: Bitmap, edit: PhotoEdit): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(edit.rotation.toFloat())
        if (edit.flip) matrix.postScale(-1f, 1f)
        var rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (edit.straighten != 0f) {
            val radians = Math.toRadians(edit.straighten.toDouble())
            val w = rotated.width.toDouble()
            val h = rotated.height.toDouble()
            val scale = (abs(cos(radians)) + abs(sin(radians)) * max(w / h, h / w)).toFloat()
            val out = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            canvas.translate(out.width / 2f, out.height / 2f)
            canvas.rotate(edit.straighten)
            canvas.scale(scale, scale)
            canvas.drawBitmap(rotated, -rotated.width / 2f, -rotated.height / 2f, Paint(Paint.FILTER_BITMAP_FLAG))
            rotated.recycle()
            rotated = out
        }
        val crop = edit.crop
        if (crop == RectF(0f, 0f, 1f, 1f)) return rotated
        val left = (crop.left * rotated.width).toInt().coerceIn(0, rotated.width - 1)
        val top = (crop.top * rotated.height).toInt().coerceIn(0, rotated.height - 1)
        val width = ((crop.right - crop.left) * rotated.width).toInt().coerceIn(1, rotated.width - left)
        val height = ((crop.bottom - crop.top) * rotated.height).toInt().coerceIn(1, rotated.height - top)
        return Bitmap.createBitmap(rotated, left, top, width, height).also { if (it !== rotated) rotated.recycle() }
    }

    private fun abs(value: Double) = kotlin.math.abs(value)

    fun curveLut(points: List<Float>): FloatArray {
        val lut = FloatArray(256)
        val xs = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        for (i in 0 until 256) {
            val x = i / 255f
            val segment = min(3, (x * 4).toInt())
            val t = (x - xs[segment]) / 0.25f
            val p0 = points[max(0, segment - 1)]
            val p1 = points[segment]
            val p2 = points[segment + 1]
            val p3 = points[min(4, segment + 2)]
            val t2 = t * t
            val t3 = t2 * t
            val v = 0.5f * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3)
            lut[i] = v.coerceIn(0f, 1f)
        }
        return lut
    }

    fun colors(bitmap: Bitmap, adjust: Adjust, curves: Curves) {
        if (adjust.neutral && curves.neutral) return
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val exposure = Math.pow(2.0, (adjust.exposure + adjust.enhance * 0.15f).toDouble()).toFloat()
        val contrast = 1f + adjust.contrast * 0.6f + adjust.enhance * 0.25f
        val saturation = 1f + adjust.saturation + adjust.enhance * 0.3f
        val fade = adjust.fade * 0.35f
        val all = if (curves.all == Curves.Identity) null else curveLut(curves.all)
        val red = if (curves.red == Curves.Identity) null else curveLut(curves.red)
        val green = if (curves.green == Curves.Identity) null else curveLut(curves.green)
        val blue = if (curves.blue == Curves.Identity) null else curveLut(curves.blue)
        val tone = FloatArray(256) { i ->
            var v = i / 255f
            v *= exposure
            v = (v - 0.5f) * contrast + 0.5f
            val shadowWeight = (1f - v).coerceIn(0f, 1f).let { it * it }
            val highlightWeight = v.coerceIn(0f, 1f).let { it * it }
            v += adjust.shadows * 0.35f * shadowWeight
            v += adjust.highlights * 0.35f * highlightWeight
            v = v * (1f - fade) + fade * 0.5f + fade * 0.15f
            v = v.coerceIn(0f, 1f)
            if (all != null) v = all[(v * 255).toInt()]
            v
        }
        val warm = adjust.warmth * 0.12f
        val shadowTint = adjust.shadowsTint
        val highlightTint = adjust.highlightsTint
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24
            var r = tone[(c shr 16) and 0xFF]
            var g = tone[(c shr 8) and 0xFF]
            var b = tone[c and 0xFF]
            if (red != null) r = red[(r * 255).toInt()]
            if (green != null) g = green[(g * 255).toInt()]
            if (blue != null) b = blue[(b * 255).toInt()]
            r += warm
            b -= warm
            val luma = 0.299f * r + 0.587f * g + 0.114f * b
            r = luma + (r - luma) * saturation
            g = luma + (g - luma) * saturation
            b = luma + (b - luma) * saturation
            if (shadowTint != 0) {
                val k = (1f - luma).coerceIn(0f, 1f) * 0.25f
                r += (((shadowTint shr 16) and 0xFF) / 255f - r) * k
                g += (((shadowTint shr 8) and 0xFF) / 255f - g) * k
                b += ((shadowTint and 0xFF) / 255f - b) * k
            }
            if (highlightTint != 0) {
                val k = luma.coerceIn(0f, 1f) * 0.25f
                r += (((highlightTint shr 16) and 0xFF) / 255f - r) * k
                g += (((highlightTint shr 8) and 0xFF) / 255f - g) * k
                b += ((highlightTint and 0xFF) / 255f - b) * k
            }
            pixels[i] = (a shl 24) or ((r.coerceIn(0f, 1f) * 255).toInt() shl 16) or ((g.coerceIn(0f, 1f) * 255).toInt() shl 8) or (b.coerceIn(0f, 1f) * 255).toInt()
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    fun sharpen(bitmap: Bitmap, amount: Float) {
        if (amount <= 0f) return
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val out = src.copyOf()
        val k = amount * 0.8f
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val c = src[i]
                var r = 0f
                var g = 0f
                var b = 0f
                for (n in intArrayOf(i - 1, i + 1, i - w, i + w)) {
                    val p = src[n]
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                }
                val cr = (c shr 16) and 0xFF
                val cg = (c shr 8) and 0xFF
                val cb = c and 0xFF
                val nr = (cr + k * (cr * 4 - r) / 4).toInt().coerceIn(0, 255)
                val ng = (cg + k * (cg * 4 - g) / 4).toInt().coerceIn(0, 255)
                val nb = (cb + k * (cb * 4 - b) / 4).toInt().coerceIn(0, 255)
                out[i] = (c and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        bitmap.setPixels(out, 0, w, 0, 0, w, h)
    }

    fun vignetteAndGrain(bitmap: Bitmap, vignette: Float, grain: Float) {
        val canvas = Canvas(bitmap)
        if (vignette > 0f) {
            val radius = hypot(bitmap.width / 2f, bitmap.height / 2f)
            val paint = Paint().apply {
                shader = android.graphics.RadialGradient(
                    bitmap.width / 2f, bitmap.height / 2f, radius,
                    intArrayOf(0, 0, android.graphics.Color.argb((vignette * 200).toInt(), 0, 0, 0)),
                    floatArrayOf(0f, 0.55f, 1f),
                    android.graphics.Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        }
        if (grain > 0f) {
            val random = java.util.Random(7)
            val paint = Paint()
            val count = (bitmap.width * bitmap.height / 18 * grain).toInt()
            repeat(count) {
                val light = random.nextBoolean()
                paint.color = if (light) android.graphics.Color.argb((grain * 60).toInt(), 255, 255, 255) else android.graphics.Color.argb((grain * 60).toInt(), 0, 0, 0)
                canvas.drawPoint(random.nextFloat() * bitmap.width, random.nextFloat() * bitmap.height, paint)
            }
        }
    }

    fun blur(bitmap: Bitmap, blur: BlurSettings) {
        if (blur.mode == BlurMode.Off) return
        val small = Bitmap.createScaledBitmap(bitmap, max(1, bitmap.width / 12), max(1, bitmap.height / 12), true)
        val blurred = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        small.recycle()
        val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(mask)
        maskCanvas.drawColor(android.graphics.Color.BLACK)
        val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        val side = min(bitmap.width, bitmap.height).toFloat()
        val cx = blur.x * bitmap.width
        val cy = blur.y * bitmap.height
        val feather = side * blur.size * 0.5f
        clear.maskFilter = BlurMaskFilter(max(1f, feather), BlurMaskFilter.Blur.NORMAL)
        if (blur.mode == BlurMode.Radial) {
            maskCanvas.drawCircle(cx, cy, side * blur.size, clear)
        } else {
            maskCanvas.save()
            maskCanvas.rotate(blur.angle, cx, cy)
            val half = side * blur.size * 0.6f
            maskCanvas.drawRect(-bitmap.width * 2f, cy - half, bitmap.width * 3f, cy + half, clear)
            maskCanvas.restore()
        }
        val composed = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(composed)
        c.drawBitmap(mask, 0f, 0f, Paint())
        c.drawBitmap(blurred, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) })
        Canvas(bitmap).drawBitmap(composed, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        blurred.recycle()
        mask.recycle()
        composed.recycle()
    }

    fun paint(bitmap: Bitmap, items: List<PaintItem>) {
        if (items.isEmpty()) return
        val source = if (items.any { it is PaintItem.Stroke && (it.tool == PaintTool.Blur || it.tool == PaintTool.Mosaic) }) bitmap.copy(Bitmap.Config.ARGB_8888, false) else null
        val canvas = Canvas(bitmap)
        drawPaint(canvas, items, bitmap.width.toFloat(), bitmap.height.toFloat(), source)
        source?.recycle()
    }

    val LabelFonts = listOf("sans-serif", "serif", "monospace", "sans-serif-condensed", "cursive", "sans-serif-black")

    fun labelTypeface(font: Int): Typeface = Typeface.create(LabelFonts.getOrElse(font) { LabelFonts[0] }, Typeface.BOLD)

    private val effects = HashMap<Pair<Int, PaintTool>, Bitmap>()

    private fun effect(source: Bitmap, tool: PaintTool): Bitmap {
        val key = System.identityHashCode(source) to tool
        effects[key]?.takeIf { !it.isRecycled }?.let { return it }
        if (effects.size > 4) effects.clear()
        val divisor = if (tool == PaintTool.Mosaic) max(8, max(source.width, source.height) / 48) else 14
        val small = Bitmap.createScaledBitmap(source, max(1, source.width / divisor), max(1, source.height / divisor), tool != PaintTool.Mosaic)
        val out = Bitmap.createScaledBitmap(small, source.width, source.height, tool != PaintTool.Mosaic)
        if (small !== out) small.recycle()
        effects[key] = out
        return out
    }

    fun drawPaint(canvas: Canvas, items: List<PaintItem>, width: Float, height: Float, source: Bitmap? = null) {
        val unit = min(width, height) / 400f
        for (item in items) {
            when (item) {
                is PaintItem.Stroke -> if ((item.tool == PaintTool.Blur || item.tool == PaintTool.Mosaic)) {
                    if (source != null && item.points.isNotEmpty()) {
                        val shader = android.graphics.BitmapShader(effect(source, item.tool), android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
                        shader.setLocalMatrix(android.graphics.Matrix().apply { setScale(width / source.width, height / source.height) })
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            strokeWidth = item.width * unit * 3f
                            this.shader = shader
                        }
                        val path = strokePath(item.points, width, height)
                        if (item.points.size == 1) path.lineTo(item.points[0].first * width + 0.1f, item.points[0].second * height)
                        canvas.drawPath(path, paint)
                    }
                } else drawStroke(canvas, item, width, height, unit)
                is PaintItem.Shape -> drawShape(canvas, item, width, height, unit)
                is PaintItem.Sticker -> {
                    val bitmap = StickerBitmaps.get(item.path)
                    if (bitmap != null) {
                        val side = min(width, height) * item.size
                        val ratio = bitmap.width.toFloat() / bitmap.height
                        val w = if (ratio >= 1f) side else side * ratio
                        val h = if (ratio >= 1f) side / ratio else side
                        val cx = item.x * width
                        val cy = item.y * height
                        canvas.save()
                        canvas.rotate(item.rotation, cx, cy)
                        canvas.drawBitmap(bitmap, null, RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                        canvas.restore()
                    }
                }
                is PaintItem.Label -> {
                    canvas.save()
                    canvas.rotate(item.rotation, item.x * width, item.y * height)
                    drawLabel(canvas, item, width, height, unit)
                    canvas.restore()
                }
            }
        }
    }

    fun itemSize(item: PaintItem, width: Float, height: Float): Pair<Float, Float>? = when (item) {
        is PaintItem.Sticker -> {
            val side = min(width, height) * item.size
            val bitmap = StickerBitmaps.get(item.path)
            val ratio = bitmap?.let { it.width.toFloat() / it.height } ?: 1f
            if (ratio >= 1f) side to side / ratio else side * ratio to side
        }
        is PaintItem.Label -> {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = item.size * min(width, height) / 400f
                typeface = labelTypeface(item.font)
            }
            val metrics = paint.fontMetrics
            val lines = item.text.split('\n')
            val w = lines.maxOf { paint.measureText(it) } + paint.textSize * 0.6f
            w to (metrics.descent - metrics.ascent) * 1.12f * lines.size + paint.textSize * 0.3f
        }
        else -> null
    }

    fun drawLabel(canvas: Canvas, item: PaintItem.Label, width: Float, height: Float, unit: Float) {
        val lines = item.text.split('\n')
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = item.size * unit
            typeface = labelTypeface(item.font)
            textAlign = Paint.Align.LEFT
        }
        val metrics = paint.fontMetrics
        val lineHeight = (metrics.descent - metrics.ascent) * 1.12f
        val widths = lines.map { paint.measureText(it) }
        val block = widths.maxOrNull() ?: 0f
        val pad = paint.textSize * 0.3f
        val cx = item.x * width
        val top = item.y * height - lineHeight * lines.size / 2f
        val light = android.graphics.Color.luminance(item.color) > 0.6f
        lines.forEachIndexed { i, line ->
            val w = widths[i]
            val x = when (item.align) {
                0 -> cx - block / 2f
                2 -> cx + block / 2f - w
                else -> cx - w / 2f
            }
            val baseline = top + lineHeight * i - metrics.ascent
            if (item.style == 2 && line.isNotEmpty()) {
                val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = item.color }
                canvas.drawRoundRect(RectF(x - pad, baseline + metrics.ascent - pad * 0.4f, x + w + pad, baseline + metrics.descent + pad * 0.4f), pad, pad, bg)
                paint.color = if (light) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                canvas.drawText(line, x, baseline, paint)
            } else if (item.style == 1) {
                val stroke = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = paint.textSize * 0.14f
                    strokeJoin = Paint.Join.ROUND
                    color = if (light) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
                canvas.drawText(line, x, baseline, stroke)
                paint.color = item.color
                canvas.drawText(line, x, baseline, paint)
            } else {
                paint.color = item.color
                paint.setShadowLayer(4f * unit, 0f, unit, android.graphics.Color.argb(120, 0, 0, 0))
                canvas.drawText(line, x, baseline, paint)
            }
        }
    }

    private fun strokePath(points: List<Pair<Float, Float>>, width: Float, height: Float): Path {
        val path = Path()
        points.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x * width, y * height) else path.lineTo(x * width, y * height) }
        return path
    }

    private fun drawStroke(canvas: Canvas, item: PaintItem.Stroke, width: Float, height: Float, unit: Float) {
        if (item.points.isEmpty()) return
        val path = strokePath(item.points, width, height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = item.color
            strokeWidth = item.width * unit
        }
        when (item.tool) {
            PaintTool.Marker -> {
                paint.alpha = 110
                paint.strokeWidth = item.width * unit * 2.2f
                paint.strokeCap = Paint.Cap.SQUARE
                canvas.drawPath(path, paint)
            }
            PaintTool.Neon -> {
                val glow = Paint(paint).apply { maskFilter = BlurMaskFilter(item.width * unit * 1.2f, BlurMaskFilter.Blur.NORMAL); strokeWidth = item.width * unit * 1.6f }
                canvas.drawPath(path, glow)
                canvas.drawPath(path, Paint(paint).apply { color = android.graphics.Color.WHITE; strokeWidth = item.width * unit * 0.45f })
            }
            PaintTool.Arrow -> {
                canvas.drawPath(path, paint)
                if (item.points.size >= 2) {
                    val (x2, y2) = item.points.last()
                    val (x1, y1) = item.points[max(0, item.points.size - 6)]
                    drawArrowHead(canvas, x1 * width, y1 * height, x2 * width, y2 * height, paint)
                }
            }
            else -> canvas.drawPath(path, paint)
        }
    }

    private fun drawArrowHead(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {
        val angle = Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        val length = paint.strokeWidth * 3.5f
        val spread = Math.toRadians(28.0)
        val head = Path().apply {
            moveTo((x2 - length * cos(angle - spread)).toFloat(), (y2 - length * sin(angle - spread)).toFloat())
            lineTo(x2, y2)
            lineTo((x2 - length * cos(angle + spread)).toFloat(), (y2 - length * sin(angle + spread)).toFloat())
        }
        canvas.drawPath(head, paint)
    }

    private fun drawShape(canvas: Canvas, item: PaintItem.Shape, width: Float, height: Float, unit: Float) {
        val rect = RectF(min(item.left, item.right) * width, min(item.top, item.bottom) * height, max(item.left, item.right) * width, max(item.top, item.bottom) * height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            color = item.color
            strokeWidth = item.width * unit
        }
        when (item.kind) {
            ShapeKind.Circle -> canvas.drawOval(rect, paint)
            ShapeKind.Rectangle -> canvas.drawRoundRect(rect, 6 * unit, 6 * unit, paint)
            ShapeKind.Star -> {
                val path = Path()
                val cx = rect.centerX()
                val cy = rect.centerY()
                val outer = min(rect.width(), rect.height()) / 2f
                val inner = outer * 0.45f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outer else inner
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val x = (cx + r * cos(a)).toFloat()
                    val y = (cy + r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeKind.Bubble -> {
                val body = RectF(rect.left, rect.top, rect.right, rect.bottom - rect.height() * 0.2f)
                val path = Path()
                path.addRoundRect(body, body.height() / 2, body.height() / 2, Path.Direction.CW)
                val tail = Path().apply {
                    moveTo(body.left + body.width() * 0.25f, body.bottom - paint.strokeWidth)
                    lineTo(body.left + body.width() * 0.15f, rect.bottom)
                    lineTo(body.left + body.width() * 0.42f, body.bottom - paint.strokeWidth)
                }
                canvas.drawPath(path, paint)
                canvas.drawPath(tail, paint)
            }
            ShapeKind.Arrow -> {
                canvas.drawLine(item.left * width, item.top * height, item.right * width, item.bottom * height, paint)
                drawArrowHead(canvas, item.left * width, item.top * height, item.right * width, item.bottom * height, paint)
            }
        }
    }

    private val decoded = android.util.LruCache<String, Bitmap>(3)

    private fun cachedDecode(path: String, maxSide: Int): Bitmap? {
        val key = "$path#$maxSide"
        val cached = decoded.get(key)?.takeIf { !it.isRecycled } ?: decode(path, maxSide)?.also { decoded.put(key, it) } ?: return null
        return cached.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun render(path: String, edit: PhotoEdit, maxSide: Int): Bitmap? {
        val source = (if (maxSide <= PreviewSide) cachedDecode(path, maxSide) else decode(path, maxSide)) ?: return null
        val shaped = geometry(source, edit)
        if (shaped !== source) source.recycle()
        val bitmap = if (shaped.isMutable) shaped else shaped.copy(Bitmap.Config.ARGB_8888, true).also { shaped.recycle() }
        colors(bitmap, edit.adjust, edit.curves)
        sharpen(bitmap, edit.adjust.sharpen)
        vignetteAndGrain(bitmap, edit.adjust.vignette, edit.adjust.grain)
        blur(bitmap, edit.blur)
        paint(bitmap, edit.paint)
        return bitmap
    }

    fun export(context: android.content.Context, path: String, edit: PhotoEdit, hd: Boolean): String {
        if (edit.untouched && !hd) return path
        val bitmap = render(path, edit, if (hd) HdSide else SdSide) ?: return path
        val target = File(context.cacheDir, "edited-${System.currentTimeMillis()}-${path.hashCode().toUInt()}.jpg")
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, if (hd) 95 else 87, it) }
        bitmap.recycle()
        return target.absolutePath
    }

    fun distance(a: Pair<Float, Float>, b: Pair<Float, Float>) = sqrt((a.first - b.first) * (a.first - b.first) + (a.second - b.second) * (a.second - b.second))
}
