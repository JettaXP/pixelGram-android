package com.pixelgram.android.feature.chat

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.North
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
private fun rememberRendered(path: String, edit: PhotoEdit): Bitmap? {
    val bitmap by produceState<Bitmap?>(null, path, edit) {
        delay(30)
        value = withContext(Dispatchers.Default) { runCatching { PhotoRenderer.render(path, edit, PhotoRenderer.PreviewSide) }.getOrNull() } ?: value
    }
    return bitmap
}

@Composable
private fun FittedImage(bitmap: Bitmap?, modifier: Modifier = Modifier, overlay: @Composable (IntSize) -> Unit = {}) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            var size by remember { mutableStateOf(IntSize.Zero) }
            Box(Modifier.aspectRatio(bitmap.width.toFloat() / bitmap.height).onSizeChanged { size = it }) {
                Image(bitmap.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                overlay(size)
            }
        }
    }
}

private enum class EditorPage { Main, Crop, Draw, Adjust }

@Composable
fun MediaEditScreen(
    path: String,
    edit: PhotoEdit,
    hd: Boolean,
    caption: String,
    onCaption: (String) -> Unit,
    onHd: () -> Unit,
    onApply: (PhotoEdit) -> Unit,
    onClose: () -> Unit,
    onSend: () -> Unit,
) {
    var page by remember { mutableStateOf(EditorPage.Main) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AnimatedContent(page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "editor-page") { current ->
                when (current) {
                    EditorPage.Main -> MainPage(path, edit, hd, caption, onCaption, onHd, onClose, onSend) { page = it }
                    EditorPage.Crop -> CropPage(path, edit, onCancel = { page = EditorPage.Main }) { onApply(it); page = EditorPage.Main }
                    EditorPage.Draw -> DrawPage(path, edit, onCancel = { page = EditorPage.Main }) { onApply(it); page = EditorPage.Main }
                    EditorPage.Adjust -> AdjustPage(path, edit, onCancel = { page = EditorPage.Main }) { onApply(it); page = EditorPage.Main }
                }
            }
        }
    }
}

@Composable
private fun MainPage(
    path: String,
    edit: PhotoEdit,
    hd: Boolean,
    caption: String,
    onCaption: (String) -> Unit,
    onHd: () -> Unit,
    onClose: () -> Unit,
    onSend: () -> Unit,
    open: (EditorPage) -> Unit,
) {
    val bitmap = rememberRendered(path, edit)
    Box(Modifier.fillMaxSize()) {
        FittedImage(bitmap, Modifier.fillMaxSize().padding(bottom = 140.dp))
        TextButtonIcon(Icons.AutoMirrored.Rounded.ArrowBack, Modifier.statusBarsPadding().padding(8.dp)) { onClose() }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            OutlinedTextField(
                caption,
                onCaption,
                placeholder = { Text(stringResource(R.string.send_caption_hint), color = Color.White.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(28.dp),
                maxLines = 3,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f), contentColor = Color.White, modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        TextButtonIcon(Icons.Rounded.Crop) { open(EditorPage.Crop) }
                        TextButtonIcon(Icons.Rounded.Brush) { open(EditorPage.Draw) }
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).border(2.dp, if (hd) MaterialTheme.colorScheme.primary else Color.White, RoundedCornerShape(8.dp)).clickable(onClick = onHd).padding(horizontal = 6.dp, vertical = 2.dp),
                        ) { Text(if (hd) "HD" else "SD", color = if (hd) MaterialTheme.colorScheme.primary else Color.White, fontWeight = FontWeight.Bold) }
                        TextButtonIcon(Icons.Rounded.Tune) { open(EditorPage.Adjust) }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Surface(onClick = onSend, shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.action_send)) }
                }
            }
        }
    }
}

@Composable
private fun TextButtonIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, tint: Color = Color.White, onClick: () -> Unit) {
    Box(modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
}

@Composable
private fun BottomActions(cancel: String, done: String, onCancel: () -> Unit, onDone: () -> Unit, middle: @Composable () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        TextButton(onClick = onCancel) { Text(cancel.uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) { middle() }
        TextButton(onClick = onDone) { Text(done.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun CropPage(path: String, initial: PhotoEdit, onCancel: () -> Unit, onDone: (PhotoEdit) -> Unit) {
    var edit by remember { mutableStateOf(initial) }
    val base = rememberRendered(path, edit.copy(crop = RectF(0f, 0f, 1f, 1f), paint = emptyList()))
    var crop by remember { mutableStateOf(RectF(initial.crop)) }
    Column(Modifier.fillMaxSize()) {
        FittedImage(base, Modifier.weight(1f).fillMaxWidth().padding(24.dp)) { size ->
            val latest by rememberUpdatedState(crop)
            Canvas(
                Modifier.fillMaxSize().pointerInput(size) {
                    var mode = 0
                    detectDragGestures(
                        onDragStart = { at ->
                            val r = latest
                            val x = at.x / size.width
                            val y = at.y / size.height
                            val near = 0.08f
                            mode = when {
                                kotlin.math.abs(x - r.left) < near && kotlin.math.abs(y - r.top) < near -> 1
                                kotlin.math.abs(x - r.right) < near && kotlin.math.abs(y - r.top) < near -> 2
                                kotlin.math.abs(x - r.left) < near && kotlin.math.abs(y - r.bottom) < near -> 3
                                kotlin.math.abs(x - r.right) < near && kotlin.math.abs(y - r.bottom) < near -> 4
                                r.contains(x, y) -> 5
                                else -> 0
                            }
                        },
                    ) { change, drag ->
                        change.consume()
                        val dx = drag.x / size.width
                        val dy = drag.y / size.height
                        val r = RectF(latest)
                        val min = 0.1f
                        when (mode) {
                            1 -> { r.left = (r.left + dx).coerceIn(0f, r.right - min); r.top = (r.top + dy).coerceIn(0f, r.bottom - min) }
                            2 -> { r.right = (r.right + dx).coerceIn(r.left + min, 1f); r.top = (r.top + dy).coerceIn(0f, r.bottom - min) }
                            3 -> { r.left = (r.left + dx).coerceIn(0f, r.right - min); r.bottom = (r.bottom + dy).coerceIn(r.top + min, 1f) }
                            4 -> { r.right = (r.right + dx).coerceIn(r.left + min, 1f); r.bottom = (r.bottom + dy).coerceIn(r.top + min, 1f) }
                            5 -> {
                                val w = r.width()
                                val h = r.height()
                                val left = (r.left + dx).coerceIn(0f, 1f - w)
                                val top = (r.top + dy).coerceIn(0f, 1f - h)
                                r.set(left, top, left + w, top + h)
                            }
                        }
                        crop = r
                    }
                },
            ) {
                val r = crop
                val rect = androidx.compose.ui.geometry.Rect(r.left * size.width, r.top * size.height, r.right * size.width, r.bottom * size.height)
                val dim = Color.Black.copy(alpha = 0.55f)
                drawRect(dim, size = androidx.compose.ui.geometry.Size(this.size.width, rect.top))
                drawRect(dim, topLeft = Offset(0f, rect.bottom), size = androidx.compose.ui.geometry.Size(this.size.width, this.size.height - rect.bottom))
                drawRect(dim, topLeft = Offset(0f, rect.top), size = androidx.compose.ui.geometry.Size(rect.left, rect.height))
                drawRect(dim, topLeft = Offset(rect.right, rect.top), size = androidx.compose.ui.geometry.Size(this.size.width - rect.right, rect.height))
                drawRect(Color.White, topLeft = rect.topLeft, size = rect.size, style = Stroke(1.5.dp.toPx()))
                for (i in 1..2) {
                    drawLine(Color.White.copy(alpha = 0.4f), Offset(rect.left + rect.width * i / 3, rect.top), Offset(rect.left + rect.width * i / 3, rect.bottom))
                    drawLine(Color.White.copy(alpha = 0.4f), Offset(rect.left, rect.top + rect.height * i / 3), Offset(rect.right, rect.top + rect.height * i / 3))
                }
                val corner = 18.dp.toPx()
                val thick = 4.dp.toPx()
                listOf(rect.topLeft to Offset(1f, 1f), rect.topRight to Offset(-1f, 1f), rect.bottomLeft to Offset(1f, -1f), rect.bottomRight to Offset(-1f, -1f)).forEach { (p, d) ->
                    drawLine(Color.White, p, Offset(p.x + corner * d.x, p.y), thick)
                    drawLine(Color.White, p, Offset(p.x, p.y + corner * d.y), thick)
                }
            }
        }
        Text("%.1f°".format(edit.straighten), color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            TextButtonIcon(Icons.Rounded.Flip) { edit = edit.copy(flip = !edit.flip) }
            Slider(edit.straighten, { edit = edit.copy(straighten = it) }, valueRange = -45f..45f, modifier = Modifier.weight(1f))
            TextButtonIcon(Icons.Rounded.RotateLeft) {
                edit = edit.copy(rotation = (edit.rotation + 270) % 360)
                crop = RectF(0f, 0f, 1f, 1f)
            }
        }
        BottomActions(stringResource(R.string.action_cancel), stringResource(R.string.editor_crop), onCancel, { onDone(edit.copy(crop = crop)) })
    }
}

private val Palette = listOf(0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF34C759, 0xFF5AC8FA, 0xFF007AFF, 0xFFAF52DE, 0xFFFF2D55, 0xFFFFFFFF, 0xFF000000).map { Color(it) }

@Composable
private fun DrawPage(path: String, initial: PhotoEdit, onCancel: () -> Unit, onDone: (PhotoEdit) -> Unit) {
    val base = rememberRendered(path, initial.copy(paint = emptyList()))
    val items = remember { mutableStateListOf<PaintItem>().apply { addAll(initial.paint) } }
    var tool by remember { mutableStateOf(PaintTool.Pen) }
    var shape by remember { mutableStateOf<ShapeKind?>(null) }
    var color by remember { mutableStateOf(Palette.first()) }
    var width by remember { mutableStateOf(8f) }
    var shapesMenu by remember { mutableStateOf(false) }
    var palette by remember { mutableStateOf(false) }
    var texting by remember { mutableStateOf(false) }
    var stickering by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(-1) }
    var live by remember { mutableStateOf<PaintItem?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp)) {
            TextButtonIcon(Icons.AutoMirrored.Rounded.Undo) { if (items.isNotEmpty()) items.removeAt(items.lastIndex) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { items.clear() }) { Text(stringResource(R.string.editor_clear_all), color = Color.White) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            FittedImage(base, Modifier.fillMaxSize().padding(16.dp)) { size ->
                Canvas(
                    Modifier.fillMaxSize().pointerInput(tool, shape, color, width, size) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val start = down.position.x / size.width to down.position.y / size.height
                            val hit = items.indexOfLast { item ->
                                when (item) {
                                    is PaintItem.Sticker -> PhotoRenderer.distance(item.x to item.y, start) < item.size * 0.6f
                                    is PaintItem.Label -> PhotoRenderer.distance(item.x to item.y, start) < 0.08f
                                    else -> false
                                }
                            }
                            if (hit >= 0) {
                                selected = hit
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    change.consume()
                                    val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                    items[hit] = when (val item = items[hit]) {
                                        is PaintItem.Sticker -> item.copy(x = nx, y = ny)
                                        is PaintItem.Label -> item.copy(x = nx, y = ny)
                                        else -> item
                                    }
                                    if (!change.pressed) break
                                }
                                return@awaitEachGesture
                            }
                            selected = -1
                            val points = mutableListOf(start)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                val point = (change.position.x / size.width).coerceIn(0f, 1f) to (change.position.y / size.height).coerceIn(0f, 1f)
                                val kind = shape
                                if (kind != null) {
                                    live = PaintItem.Shape(kind, start.first, start.second, point.first, point.second, color.toArgb(), width)
                                } else if (tool == PaintTool.Eraser) {
                                    items.removeAll { item -> item is PaintItem.Stroke && item.points.any { PhotoRenderer.distance(it, point) < 0.03f } }
                                } else {
                                    points += point
                                    live = PaintItem.Stroke(points.toList(), color.toArgb(), width, tool)
                                }
                                if (!change.pressed) break
                            }
                            live?.let { items += it }
                            live = null
                        }
                    },
                ) {
                    drawIntoCanvas { canvas ->
                        PhotoRenderer.drawPaint(canvas.nativeCanvas, items.toList() + listOfNotNull(live), this.size.width, this.size.height)
                    }
                }
            }
            Box(Modifier.align(Alignment.CenterStart).padding(start = 4.dp).width(36.dp).fillMaxHeight(0.5f)) {
                Slider(
                    width,
                    { value ->
                        width = value
                        val item = items.getOrNull(selected)
                        if (item is PaintItem.Sticker) items[selected] = item.copy(size = (value / 40f).coerceIn(0.08f, 1f))
                        if (item is PaintItem.Label) items[selected] = item.copy(size = value * 4f)
                    },
                    valueRange = 2f..40f,
                    modifier = Modifier.align(Alignment.Center).width(220.dp).graphicsLayerRotate(),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            Box {
                Box(Modifier.size(36.dp).clip(CircleShape).background(color).border(2.dp, Color.White, CircleShape).clickable { palette = true })
                DropdownMenu(expanded = palette, onDismissRequest = { palette = false }) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Palette.forEach { c -> Box(Modifier.size(28.dp).clip(CircleShape).background(c).border(1.dp, Color.Gray, CircleShape).clickable { color = c; palette = false }) }
                    }
                }
            }
            PaintTool.entries.forEach { t ->
                val active = shape == null && tool == t
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(if (active) Color.White.copy(alpha = 0.25f) else Color.Transparent).clickable { tool = t; shape = null },
                    contentAlignment = Alignment.Center,
                ) { Text(toolGlyph(t), color = Color.White, fontWeight = FontWeight.Bold) }
            }
            Box {
                TextButtonIcon(Icons.Rounded.Add, tint = if (shape != null) MaterialTheme.colorScheme.primary else Color.White) { shapesMenu = true }
                DropdownMenu(expanded = shapesMenu, onDismissRequest = { shapesMenu = false }, shape = RoundedCornerShape(16.dp)) {
                    ShapeKind.entries.forEach { kind ->
                        DropdownMenuItem(text = { Text(stringResource(shapeLabel(kind))) }, onClick = { shape = kind; shapesMenu = false })
                    }
                }
            }
        }
        BottomActions(stringResource(R.string.action_cancel), stringResource(R.string.action_done), onCancel, { onDone(initial.copy(paint = items.toList())) }) {
            TextButton(onClick = { stickering = true }) { Text(stringResource(R.string.editor_sticker).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
            TextButton(onClick = { texting = true }) { Text(stringResource(R.string.editor_text).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
    if (stickering) StickerPickerSheet(onDismiss = { stickering = false }) { path ->
        stickering = false
        items += PaintItem.Sticker(path, 0.5f, 0.5f, 0.35f)
        selected = items.lastIndex
    }
    if (texting) {
        var value by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { texting = false },
            title = { Text(stringResource(R.string.editor_text)) },
            text = { OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = {
                    if (value.isNotBlank()) items += PaintItem.Label(value.trim(), 0.5f, 0.5f, color.toArgb(), width * 4f)
                    texting = false
                }) { Text(stringResource(R.string.action_done)) }
            },
        )
    }
}

private fun Modifier.graphicsLayerRotate(): Modifier = this.then(androidx.compose.ui.Modifier.graphicsLayer { rotationZ = -90f })

private fun toolGlyph(tool: PaintTool): String = when (tool) {
    PaintTool.Pen -> "✎"
    PaintTool.Arrow -> "➚"
    PaintTool.Marker -> "▮"
    PaintTool.Neon -> "✦"
    PaintTool.Eraser -> "⌫"
}

private fun shapeLabel(kind: ShapeKind): Int = when (kind) {
    ShapeKind.Circle -> R.string.shape_circle
    ShapeKind.Rectangle -> R.string.shape_rectangle
    ShapeKind.Star -> R.string.shape_star
    ShapeKind.Bubble -> R.string.shape_bubble
    ShapeKind.Arrow -> R.string.shape_arrow
}

private enum class AdjustTab { Adjust, Blur, Curves }

private val TintColors = listOf(0, 0xFFE53935.toInt(), 0xFFFB8C00.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(), 0xFF26C6DA.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt())

@Composable
private fun AdjustPage(path: String, initial: PhotoEdit, onCancel: () -> Unit, onDone: (PhotoEdit) -> Unit) {
    var edit by remember { mutableStateOf(initial) }
    var tab by remember { mutableStateOf(AdjustTab.Adjust) }
    var channel by remember { mutableStateOf(0) }
    val preview = rememberRendered(path, edit.copy(paint = emptyList()))
    Column(Modifier.fillMaxSize()) {
        FittedImage(preview, Modifier.weight(1f).fillMaxWidth().statusBarsPadding().padding(16.dp)) { size ->
            if (tab == AdjustTab.Blur && edit.blur.mode != BlurMode.Off) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(size) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            edit = edit.copy(blur = edit.blur.copy(x = (change.position.x / size.width).coerceIn(0f, 1f), y = (change.position.y / size.height).coerceIn(0f, 1f)))
                        }
                    },
                ) {
                    val c = Offset(edit.blur.x * this.size.width, edit.blur.y * this.size.height)
                    val r = kotlin.math.min(this.size.width, this.size.height) * edit.blur.size
                    if (edit.blur.mode == BlurMode.Radial) drawCircle(Color.White, r, c, style = Stroke(1.5.dp.toPx()))
                    else {
                        drawLine(Color.White, Offset(0f, c.y - r * 0.6f), Offset(this.size.width, c.y - r * 0.6f), 1.5.dp.toPx())
                        drawLine(Color.White, Offset(0f, c.y + r * 0.6f), Offset(this.size.width, c.y + r * 0.6f), 1.5.dp.toPx())
                    }
                }
            }
            if (tab == AdjustTab.Curves) {
                val points = when (channel) { 1 -> edit.curves.red; 2 -> edit.curves.green; 3 -> edit.curves.blue; else -> edit.curves.all }
                val lineColor = when (channel) { 1 -> Color(0xFFE53935); 2 -> Color(0xFF43A047); 3 -> Color(0xFF1E88E5); else -> Color.White }
                Canvas(
                    Modifier.fillMaxSize().pointerInput(channel, size) {
                        var index = 0
                        detectDragGestures(onDragStart = { at -> index = ((at.x / size.width) * 4).roundToInt().coerceIn(0, 4) }) { change, drag ->
                            change.consume()
                            val current = when (channel) { 1 -> edit.curves.red; 2 -> edit.curves.green; 3 -> edit.curves.blue; else -> edit.curves.all }.toMutableList()
                            current[index] = (current[index] - drag.y / size.height).coerceIn(0f, 1f)
                            edit = edit.copy(curves = when (channel) {
                                1 -> edit.curves.copy(red = current)
                                2 -> edit.curves.copy(green = current)
                                3 -> edit.curves.copy(blue = current)
                                else -> edit.curves.copy(all = current)
                            })
                        }
                    },
                ) {
                    for (i in 1..3) drawLine(Color.White.copy(alpha = 0.35f), Offset(this.size.width * i / 4, 0f), Offset(this.size.width * i / 4, this.size.height))
                    val path = Path()
                    points.forEachIndexed { i, v ->
                        val p = Offset(this.size.width * i / 4, this.size.height * (1f - v))
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                        drawCircle(lineColor, 6.dp.toPx(), p)
                    }
                    drawPath(path, lineColor, style = Stroke(2.dp.toPx()))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(260.dp)) {
            when (tab) {
                AdjustTab.Adjust -> Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    val a = edit.adjust
                    AdjustSlider(stringResource(R.string.adjust_enhance), a.enhance, 0f..1f) { edit = edit.copy(adjust = a.copy(enhance = it)) }
                    AdjustSlider(stringResource(R.string.adjust_exposure), a.exposure, -1f..1f) { edit = edit.copy(adjust = a.copy(exposure = it)) }
                    AdjustSlider(stringResource(R.string.adjust_contrast), a.contrast, -1f..1f) { edit = edit.copy(adjust = a.copy(contrast = it)) }
                    AdjustSlider(stringResource(R.string.adjust_saturation), a.saturation, -1f..1f) { edit = edit.copy(adjust = a.copy(saturation = it)) }
                    AdjustSlider(stringResource(R.string.adjust_warmth), a.warmth, -1f..1f) { edit = edit.copy(adjust = a.copy(warmth = it)) }
                    AdjustSlider(stringResource(R.string.adjust_fade), a.fade, 0f..1f) { edit = edit.copy(adjust = a.copy(fade = it)) }
                    AdjustSlider(stringResource(R.string.adjust_highlights), a.highlights, -1f..1f) { edit = edit.copy(adjust = a.copy(highlights = it)) }
                    AdjustSlider(stringResource(R.string.adjust_shadows), a.shadows, -1f..1f) { edit = edit.copy(adjust = a.copy(shadows = it)) }
                    AdjustSlider(stringResource(R.string.adjust_vignette), a.vignette, 0f..1f) { edit = edit.copy(adjust = a.copy(vignette = it)) }
                    AdjustSlider(stringResource(R.string.adjust_grain), a.grain, 0f..1f) { edit = edit.copy(adjust = a.copy(grain = it)) }
                    AdjustSlider(stringResource(R.string.adjust_sharpen), a.sharpen, 0f..1f) { edit = edit.copy(adjust = a.copy(sharpen = it)) }
                    TintRow(stringResource(R.string.adjust_shadows), a.shadowsTint) { edit = edit.copy(adjust = a.copy(shadowsTint = it)) }
                    TintRow(stringResource(R.string.adjust_highlights), a.highlightsTint) { edit = edit.copy(adjust = a.copy(highlightsTint = it)) }
                }
                AdjustTab.Blur -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        listOf(BlurMode.Off to R.string.blur_off, BlurMode.Radial to R.string.blur_radial, BlurMode.Linear to R.string.blur_linear).forEach { (mode, label) ->
                            TextButton(onClick = { edit = edit.copy(blur = edit.blur.copy(mode = mode)) }) {
                                Text(stringResource(label), color = if (edit.blur.mode == mode) MaterialTheme.colorScheme.primary else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (edit.blur.mode != BlurMode.Off) {
                        AdjustSlider(stringResource(R.string.blur_size), edit.blur.size, 0.05f..0.6f) { edit = edit.copy(blur = edit.blur.copy(size = it)) }
                        if (edit.blur.mode == BlurMode.Linear) AdjustSlider(stringResource(R.string.blur_angle), edit.blur.angle, -90f..90f) { edit = edit.copy(blur = edit.blur.copy(angle = it)) }
                    }
                }
                AdjustTab.Curves -> Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    listOf(R.string.curves_all to Color.White, R.string.curves_red to Color(0xFFE53935), R.string.curves_green to Color(0xFF43A047), R.string.curves_blue to Color(0xFF1E88E5)).forEachIndexed { i, (label, c) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { channel = i }.padding(8.dp)) {
                            Box(Modifier.size(28.dp).border(if (channel == i) 6.dp else 2.dp, c, CircleShape))
                            Text(stringResource(label), color = c, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        BottomActions(stringResource(R.string.action_cancel), stringResource(R.string.action_done), onCancel, { onDone(edit) }) {
            TextButtonIcon(Icons.Rounded.Tune, tint = if (tab == AdjustTab.Adjust) MaterialTheme.colorScheme.primary else Color.White) { tab = AdjustTab.Adjust }
            TextButtonIcon(Icons.Rounded.BlurOn, tint = if (tab == AdjustTab.Blur) MaterialTheme.colorScheme.primary else Color.White) { tab = AdjustTab.Blur }
            TextButtonIcon(Icons.Rounded.ShowChart, tint = if (tab == AdjustTab.Curves) MaterialTheme.colorScheme.primary else Color.White) { tab = AdjustTab.Curves }
        }
    }
}

@Composable
private fun AdjustSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(104.dp))
        Slider(value, onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TintRow(label: String, selected: Int, onPick: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(104.dp))
        TintColors.forEach { c ->
            val color = if (c == 0) Color.White else Color(c)
            Box(
                Modifier.padding(horizontal = 3.dp).size(26.dp).clip(CircleShape).border(if (selected == c) 7.dp else 2.dp, color, CircleShape).clickable { onPick(c) },
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StickerPickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val graph = com.pixelgram.android.LocalGraph.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val snapshot by graph.store.snapshot.collectAsState()
    val stickers by produceState(emptyList<org.drinkless.tdlib.TdApi.Sticker>()) {
        val recent = runCatching { graph.td.send(org.drinkless.tdlib.TdApi.GetRecentStickers(false)).stickers.toList() }.getOrDefault(emptyList())
        value = recent
        val sets = runCatching { graph.td.send(org.drinkless.tdlib.TdApi.GetInstalledStickerSets(org.drinkless.tdlib.TdApi.StickerTypeRegular())).sets.toList() }.getOrDefault(emptyList())
        val all = recent.toMutableList()
        sets.take(12).forEach { info ->
            all += runCatching { graph.td.send(org.drinkless.tdlib.TdApi.GetStickerSet(info.id)).stickers.toList() }.getOrDefault(emptyList())
            value = all.distinctBy { it.sticker.id }
        }
    }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
            Modifier.fillMaxWidth().height(420.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(stickers.size, key = { stickers[it].sticker.id }) { index ->
                val sticker = stickers[index]
                Box(
                    Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).clickable {
                        scope.launch {
                            val file = if (sticker.format is org.drinkless.tdlib.TdApi.StickerFormatWebm) sticker.thumbnail?.file else sticker.sticker
                            val id = file?.id ?: return@launch
                            val done = runCatching { graph.td.send(org.drinkless.tdlib.TdApi.DownloadFile(id, 32, 0, 0, true)) }.getOrNull()
                            val path = done?.local?.path?.takeIf { done.local.isDownloadingCompleted && it.isNotEmpty() } ?: return@launch
                            onPick(path)
                        }
                    }.padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) { StickerView(sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.fillMaxSize(), video = false) }
            }
        }
    }
}
