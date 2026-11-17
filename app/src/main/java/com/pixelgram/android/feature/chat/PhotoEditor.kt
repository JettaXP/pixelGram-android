@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pixelgram.android.feature.chat

import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.AutoFixNormal
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.FormatAlignCenter
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
import androidx.compose.ui.layout.layout
import androidx.compose.animation.animateContentSize
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
        delay(8)
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
        modifier = Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        androidx.compose.material3.FilledTonalIconButton(
            onClick = onCancel,
            shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
            colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White),
            modifier = Modifier.size(52.dp),
        ) { Icon(Icons.Rounded.Close, cancel) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) { middle() }
        androidx.compose.material3.FilledIconButton(
            onClick = onDone,
            shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
            modifier = Modifier.size(52.dp),
        ) { Icon(Icons.Rounded.Check, done) }
    }
}

@Composable
private fun TonalIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    androidx.compose.material3.FilledTonalIconButton(
        onClick = onClick,
        shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White),
        modifier = Modifier.size(48.dp),
    ) { Icon(icon, null) }
}

@Composable
private fun EditorPanel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.08f)).animateContentSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun CropPage(path: String, initial: PhotoEdit, onCancel: () -> Unit, onDone: (PhotoEdit) -> Unit) {
    var edit by remember { mutableStateOf(initial) }
    val base = rememberRendered(path, edit.copy(crop = RectF(0f, 0f, 1f, 1f), paint = emptyList()))
    var crop by remember { mutableStateOf(RectF(initial.crop)) }
    Column(Modifier.fillMaxSize()) {
        FittedImage(base, Modifier.weight(1f).fillMaxWidth().padding(24.dp)) { size ->
            val latest by rememberUpdatedState(crop)
            val extra = with(androidx.compose.ui.platform.LocalDensity.current) { 28.dp.roundToPx() }
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(constraints.maxWidth + extra * 2, constraints.maxHeight + extra * 2))
                        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-extra, -extra) }
                    }
                    .pointerInput(size) {
                    var mode = 0
                    detectDragGestures(
                        onDragStart = { at ->
                            val r = latest
                            val px = at.x - extra
                            val py = at.y - extra
                            val near = extra.toFloat()
                            val l = r.left * size.width
                            val t = r.top * size.height
                            val rr = r.right * size.width
                            val b = r.bottom * size.height
                            val nearL = kotlin.math.abs(px - l) < near
                            val nearR = kotlin.math.abs(px - rr) < near
                            val nearT = kotlin.math.abs(py - t) < near
                            val nearB = kotlin.math.abs(py - b) < near
                            val inX = px > l - near && px < rr + near
                            val inY = py > t - near && py < b + near
                            mode = when {
                                nearL && nearT -> 1
                                nearR && nearT -> 2
                                nearL && nearB -> 3
                                nearR && nearB -> 4
                                nearL && inY -> 6
                                nearR && inY -> 7
                                nearT && inX -> 8
                                nearB && inX -> 9
                                px > l && px < rr && py > t && py < b -> 5
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
                            6 -> r.left = (r.left + dx).coerceIn(0f, r.right - min)
                            7 -> r.right = (r.right + dx).coerceIn(r.left + min, 1f)
                            8 -> r.top = (r.top + dy).coerceIn(0f, r.bottom - min)
                            9 -> r.bottom = (r.bottom + dy).coerceIn(r.top + min, 1f)
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
            )
            Canvas(Modifier.fillMaxSize()) {
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
        EditorPanel {
            Text("%.1f°".format(edit.straighten), color = Color.White, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalIcon(Icons.Rounded.Flip) { edit = edit.copy(flip = !edit.flip) }
                Slider(edit.straighten, { edit = edit.copy(straighten = it) }, valueRange = -45f..45f, modifier = Modifier.weight(1f))
                TonalIcon(Icons.Rounded.RotateLeft) {
                    edit = edit.copy(rotation = (edit.rotation + 270) % 360)
                    crop = RectF(0f, 0f, 1f, 1f)
                }
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
    var textIndex by remember { mutableStateOf(-1) }
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
                            val px = size.width.toFloat()
                            val py = size.height.toFloat()
                            fun local(item: PaintItem, at: Offset): Offset {
                                val (cx, cy, rot) = when (item) {
                                    is PaintItem.Sticker -> Triple(item.x * px, item.y * py, item.rotation)
                                    is PaintItem.Label -> Triple(item.x * px, item.y * py, item.rotation)
                                    else -> Triple(0f, 0f, 0f)
                                }
                                val a = Math.toRadians(-rot.toDouble())
                                val dx = at.x - cx
                                val dy = at.y - cy
                                return Offset((dx * kotlin.math.cos(a) - dy * kotlin.math.sin(a)).toFloat(), (dx * kotlin.math.sin(a) + dy * kotlin.math.cos(a)).toFloat())
                            }
                            val slop = 28.dp.toPx()
                            val current = items.getOrNull(selected)
                            val currentSize = current?.let { PhotoRenderer.itemSize(it, px, py) }
                            if (current != null && currentSize != null) {
                                val l = local(current, down.position)
                                val hw = currentSize.first / 2f + FramePad.toPx()
                                val hh = currentSize.second / 2f + FramePad.toPx()
                                if ((l - Offset(-hw, -hh)).getDistance() < slop) {
                                    items.removeAt(selected)
                                    selected = -1
                                    while (awaitPointerEvent().changes.any { it.pressed }) Unit
                                    return@awaitEachGesture
                                }
                                if ((l - Offset(hw, hh)).getDistance() < slop) {
                                    val index = selected
                                    val center = when (current) {
                                        is PaintItem.Sticker -> Offset(current.x * px, current.y * py)
                                        is PaintItem.Label -> Offset(current.x * px, current.y * py)
                                        else -> Offset.Zero
                                    }
                                    val startVector = down.position - center
                                    val startAngle = Math.toDegrees(kotlin.math.atan2(startVector.y.toDouble(), startVector.x.toDouble())).toFloat()
                                    val startDistance = startVector.getDistance().coerceAtLeast(1f)
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        change.consume()
                                        if (!change.pressed) break
                                        val v = change.position - center
                                        val angle = Math.toDegrees(kotlin.math.atan2(v.y.toDouble(), v.x.toDouble())).toFloat() - startAngle
                                        val scale = v.getDistance() / startDistance
                                        items[index] = when (current) {
                                            is PaintItem.Sticker -> current.copy(size = (current.size * scale).coerceIn(0.05f, 1.5f), rotation = current.rotation + angle)
                                            is PaintItem.Label -> current.copy(size = (current.size * scale).coerceIn(8f, 400f), rotation = current.rotation + angle)
                                            else -> current
                                        }
                                    }
                                    return@awaitEachGesture
                                }
                            }
                            val hit = items.indexOfLast { item ->
                                val bounds = PhotoRenderer.itemSize(item, px, py) ?: return@indexOfLast false
                                val l = local(item, down.position)
                                kotlin.math.abs(l.x) < bounds.first / 2f + FramePad.toPx() && kotlin.math.abs(l.y) < bounds.second / 2f + FramePad.toPx()
                            }
                            if (hit >= 0) {
                                val wasSelected = selected == hit
                                selected = hit
                                var moved = false
                                var travel = Offset.Zero
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.none { it.pressed }) break
                                    val pan = event.calculatePan()
                                    val zoom = event.calculateZoom()
                                    val turn = event.calculateRotation()
                                    event.changes.forEach { it.consume() }
                                    travel += pan
                                    if (travel.getDistance() > viewConfiguration.touchSlop || event.changes.count { it.pressed } > 1) moved = true
                                    if (!moved) continue
                                    items[hit] = when (val item = items[hit]) {
                                        is PaintItem.Sticker -> item.copy(x = (item.x + pan.x / px).coerceIn(0f, 1f), y = (item.y + pan.y / py).coerceIn(0f, 1f), size = (item.size * zoom).coerceIn(0.05f, 1.5f), rotation = item.rotation + turn)
                                        is PaintItem.Label -> item.copy(x = (item.x + pan.x / px).coerceIn(0f, 1f), y = (item.y + pan.y / py).coerceIn(0f, 1f), size = (item.size * zoom).coerceIn(8f, 400f), rotation = item.rotation + turn)
                                        else -> item
                                    }
                                }
                                if (!moved && wasSelected && items.getOrNull(hit) is PaintItem.Label) {
                                    textIndex = hit
                                    texting = true
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
                        PhotoRenderer.drawPaint(canvas.nativeCanvas, items.toList() + listOfNotNull(live), this.size.width, this.size.height, base)
                    }
                    val chosen = items.getOrNull(selected)
                    val bounds = chosen?.let { PhotoRenderer.itemSize(it, this.size.width, this.size.height) }
                    if (chosen != null && bounds != null) {
                        val (cx, cy, rot) = when (chosen) {
                            is PaintItem.Sticker -> Triple(chosen.x * this.size.width, chosen.y * this.size.height, chosen.rotation)
                            is PaintItem.Label -> Triple(chosen.x * this.size.width, chosen.y * this.size.height, chosen.rotation)
                            else -> Triple(0f, 0f, 0f)
                        }
                        val hw = bounds.first / 2f + FramePad.toPx()
                        val hh = bounds.second / 2f + FramePad.toPx()
                        rotate(rot, Offset(cx, cy)) {
                            drawRect(
                                Color.White,
                                Offset(cx - hw, cy - hh),
                                androidx.compose.ui.geometry.Size(hw * 2, hh * 2),
                                style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))),
                            )
                            val del = Offset(cx - hw, cy - hh)
                            drawCircle(Color.White, 12.dp.toPx(), del)
                            val k = 4.dp.toPx()
                            drawLine(Color.Black, del + Offset(-k, -k), del + Offset(k, k), 2.dp.toPx())
                            drawLine(Color.Black, del + Offset(-k, k), del + Offset(k, -k), 2.dp.toPx())
                            val handle = Offset(cx + hw, cy + hh)
                            drawCircle(Color.White, 12.dp.toPx(), handle)
                            drawCircle(MaterialColorHandle, 6.dp.toPx(), handle)
                        }
                    }
                }
            }
            Slider(
                value = width,
                onValueChange = { value ->
                    width = value
                    val item = items.getOrNull(selected)
                    if (item is PaintItem.Sticker) items[selected] = item.copy(size = (value / 40f).coerceIn(0.08f, 1f))
                    if (item is PaintItem.Label) items[selected] = item.copy(size = value * 4f)
                },
                valueRange = 2f..40f,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).fillMaxHeight(0.55f).width(48.dp).vertical(),
            )
        }
        EditorPanel(Modifier.padding(bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(color).border(3.dp, Color.White, CircleShape).clickable { palette = !palette })
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                ) {
                    PaintTool.entries.forEach { t ->
                        androidx.compose.material3.FilledIconToggleButton(
                            checked = shape == null && tool == t,
                            onCheckedChange = { tool = t; shape = null },
                            shapes = androidx.compose.material3.IconButtonDefaults.toggleableShapes(),
                            colors = androidx.compose.material3.IconButtonDefaults.filledIconToggleButtonColors(containerColor = Color.Transparent, contentColor = Color.White),
                            modifier = Modifier.size(48.dp),
                        ) { Icon(toolIcon(t), null) }
                    }
                }
                Box {
                    androidx.compose.material3.FilledIconToggleButton(
                        checked = shape != null,
                        onCheckedChange = { shapesMenu = true },
                        shapes = androidx.compose.material3.IconButtonDefaults.toggleableShapes(),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconToggleButtonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White),
                        modifier = Modifier.size(48.dp),
                    ) { Icon(Icons.Rounded.Category, null) }
                    DropdownMenu(expanded = shapesMenu, onDismissRequest = { shapesMenu = false }, shape = RoundedCornerShape(16.dp)) {
                        ShapeKind.entries.forEach { kind ->
                            DropdownMenuItem(text = { Text(stringResource(shapeLabel(kind))) }, onClick = { shape = kind; shapesMenu = false })
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(palette) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Palette.forEach { c ->
                        val active = c == color
                        val ring by androidx.compose.animation.core.animateDpAsState(if (active) 3.dp else 1.dp, label = "swatch")
                        Box(Modifier.size(36.dp).clip(CircleShape).background(c).border(ring, if (active) Color.White else Color.White.copy(alpha = 0.3f), CircleShape).clickable { color = c })
                    }
                }
            }
        }
        BottomActions(stringResource(R.string.action_cancel), stringResource(R.string.action_done), onCancel, { onDone(initial.copy(paint = items.toList())) }) {
            androidx.compose.material3.FilledTonalButton(
                onClick = { stickering = true },
                shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White),
            ) {
                Icon(Icons.Rounded.EmojiEmotions, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.editor_sticker))
            }
            androidx.compose.material3.FilledTonalButton(
                onClick = { texting = true },
                shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White),
            ) {
                Icon(Icons.Rounded.TextFields, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.editor_text))
            }
        }
    }
    if (stickering) StickerPickerSheet(onDismiss = { stickering = false }) { path ->
        stickering = false
        items += PaintItem.Sticker(path, 0.5f, 0.5f, 0.35f)
        selected = items.lastIndex
    }
    if (texting) {
        val existing = items.getOrNull(textIndex) as? PaintItem.Label
        TextEditorOverlay(
            initial = existing ?: PaintItem.Label("", 0.5f, 0.5f, color.toArgb(), width * 4f),
            onCancel = {
                texting = false
                textIndex = -1
            },
        ) { label ->
            val index = textIndex
            if (index >= 0 && index < items.size) {
                if (label.text.isBlank()) items.removeAt(index) else items[index] = label
            } else if (label.text.isNotBlank()) {
                items += label
                selected = items.lastIndex
            }
            texting = false
            textIndex = -1
        }
    }
}

@Composable
private fun SizeWedge(value: Float, range: ClosedFloatingPointRange<Float>, modifier: Modifier, onChange: (Float) -> Unit) {
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Canvas(
        modifier.pointerInput(range) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                fun apply(y: Float) {
                    val f = (1f - y / size.height).coerceIn(0f, 1f)
                    onChange(range.start + f * (range.endInclusive - range.start))
                }
                apply(down.position.y)
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) break
                    apply(change.position.y)
                }
            }
        },
    ) {
        val cx = this.size.width / 2f
        val top = 14.dp.toPx()
        val bottom = this.size.height - 14.dp.toPx()
        val wedge = Path().apply {
            moveTo(cx - 12.dp.toPx(), top)
            lineTo(cx + 12.dp.toPx(), top)
            lineTo(cx + 2.dp.toPx(), bottom)
            lineTo(cx - 2.dp.toPx(), bottom)
            close()
        }
        drawPath(wedge, Color.White.copy(alpha = 0.45f))
        val y = bottom - (bottom - top) * fraction
        drawCircle(Color.Black.copy(alpha = 0.25f), 15.dp.toPx(), Offset(cx, y + 1.dp.toPx()))
        drawCircle(Color.White, 14.dp.toPx(), Offset(cx, y))
    }
}

@Composable
private fun TextEditorOverlay(initial: PaintItem.Label, onCancel: () -> Unit, onDone: (PaintItem.Label) -> Unit) {
    var label by remember { mutableStateOf(initial) }
    var value by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(initial.text, androidx.compose.ui.text.TextRange(initial.text.length))) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).statusBarsPadding().imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onDone(label.copy(text = value.text.trimEnd())) }) { Text(stringResource(R.string.action_done).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                val fontFamily = remember(label.font) { androidx.compose.ui.text.font.FontFamily(PhotoRenderer.labelTypeface(label.font)) }
                val light = androidx.compose.ui.graphics.Color(label.color).luminance() > 0.6f
                val contrast = if (light) Color.Black else Color.White
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (label.style == 2) contrast else Color(label.color),
                        fontSize = 34.sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        textAlign = when (label.align) { 0 -> androidx.compose.ui.text.style.TextAlign.Start; 2 -> androidx.compose.ui.text.style.TextAlign.End; else -> androidx.compose.ui.text.style.TextAlign.Center },
                        drawStyle = null,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .then(if (label.style == 2) Modifier.background(Color(label.color), RoundedCornerShape(12.dp)) else Modifier)
                                .then(if (label.style == 1) Modifier.border(2.dp, contrast.copy(alpha = 0.6f), RoundedCornerShape(12.dp)) else Modifier)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) { inner() }
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                PhotoRenderer.LabelFonts.forEachIndexed { i, name ->
                    val family = remember(i) { androidx.compose.ui.text.font.FontFamily(PhotoRenderer.labelTypeface(i)) }
                    val active = label.font == i
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(if (active) Color.White else Color.White.copy(alpha = 0.15f)).clickable { label = label.copy(font = i) }.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text(FontNames.getOrElse(i) { name }, color = if (active) Color.Black else Color.White, fontFamily = family, fontWeight = FontWeight.Bold) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                IconButton(onClick = { label = label.copy(align = (label.align + 1) % 3) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        when (label.align) { 0 -> Icons.AutoMirrored.Rounded.FormatAlignLeft; 2 -> Icons.AutoMirrored.Rounded.FormatAlignRight; else -> Icons.Rounded.FormatAlignCenter },
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable { label = label.copy(style = (label.style + 1) % 3) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .then(if (label.style == 2) Modifier.background(Color.White, RoundedCornerShape(8.dp)) else Modifier)
                            .then(if (label.style == 1) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { Text("A", color = if (label.style == 2) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
                }
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    Palette.forEach { c ->
                        val active = c.toArgb() == label.color
                        Box(Modifier.size(32.dp).clip(CircleShape).background(c).border(if (active) 3.dp else 1.dp, if (active) Color.White else Color.Gray, CircleShape).clickable { label = label.copy(color = c.toArgb()) })
                    }
                }
            }
        }
    }
}

private val FramePad = 10.dp
private val MaterialColorHandle = Color(0xFF2196F3)

private val FontNames = listOf("Roboto", "Serif", "Mono", "Condensed", "Cursive", "Black")

private fun Modifier.graphicsLayerRotate(): Modifier = this.then(androidx.compose.ui.Modifier.graphicsLayer { rotationZ = -90f })

private fun Modifier.vertical(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(androidx.compose.ui.unit.Constraints(minWidth = constraints.minHeight, maxWidth = constraints.maxHeight, minHeight = constraints.minWidth, maxHeight = constraints.maxWidth))
        layout(placeable.height, placeable.width) { placeable.place(-(placeable.width - placeable.height) / 2, (placeable.width - placeable.height) / 2) }
    }
    .graphicsLayer { rotationZ = -90f }

private fun toolIcon(tool: PaintTool): androidx.compose.ui.graphics.vector.ImageVector = when (tool) {
    PaintTool.Pen -> Icons.Rounded.Edit
    PaintTool.Arrow -> Icons.Rounded.NorthEast
    PaintTool.Marker -> Icons.Rounded.Highlight
    PaintTool.Neon -> Icons.Rounded.AutoAwesome
    PaintTool.Blur -> Icons.Rounded.BlurOn
    PaintTool.Mosaic -> Icons.Rounded.GridOn
    PaintTool.Eraser -> Icons.Rounded.AutoFixNormal
}

private fun toolGlyph(tool: PaintTool): String = when (tool) {
    PaintTool.Pen -> "✎"
    PaintTool.Arrow -> "➚"
    PaintTool.Marker -> "▮"
    PaintTool.Neon -> "✦"
    PaintTool.Blur -> "◍"
    PaintTool.Mosaic -> "▦"
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
                    val grid = Color.White.copy(alpha = 0.22f)
                    for (i in 1..3) {
                        drawLine(grid, Offset(this.size.width * i / 4, 0f), Offset(this.size.width * i / 4, this.size.height), 1.dp.toPx())
                        drawLine(grid, Offset(0f, this.size.height * i / 4), Offset(this.size.width, this.size.height * i / 4), 1.dp.toPx())
                    }
                    drawLine(grid, Offset(0f, this.size.height), Offset(this.size.width, 0f), 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())))
                    val lut = PhotoRenderer.curveLut(points)
                    val path = Path()
                    for (i in 0..255) {
                        val p = Offset(this.size.width * i / 255f, this.size.height * (1f - lut[i]))
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    drawPath(path, Color.Black.copy(alpha = 0.35f), style = Stroke(5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawPath(path, lineColor, style = Stroke(2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    points.forEachIndexed { i, v ->
                        val p = Offset(this.size.width * i / 4, this.size.height * (1f - v))
                        drawCircle(Color.Black.copy(alpha = 0.35f), 9.dp.toPx(), p)
                        drawCircle(lineColor, 7.dp.toPx(), p)
                        drawCircle(Color.Black, 3.dp.toPx(), p)
                    }
                }
            }
        }
        EditorPanel(Modifier.padding(bottom = 8.dp)) {
            AnimatedContent(tab, transitionSpec = { (fadeIn() + androidx.compose.animation.slideInVertically { it / 6 }) togetherWith fadeOut() }, label = "adjust-tab") { tab ->
            when (tab) {
                AdjustTab.Adjust -> Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
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
                AdjustTab.Blur -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val modes = listOf(BlurMode.Off, BlurMode.Radial, BlurMode.Linear)
                        ConnectedChoice(
                            listOf(R.string.blur_off to null, R.string.blur_radial to null, R.string.blur_linear to null),
                            modes.indexOf(edit.blur.mode),
                        ) { edit = edit.copy(blur = edit.blur.copy(mode = modes[it])) }
                    }
                    if (edit.blur.mode != BlurMode.Off) {
                        AdjustSlider(stringResource(R.string.blur_size), edit.blur.size, 0.05f..0.6f) { edit = edit.copy(blur = edit.blur.copy(size = it)) }
                        if (edit.blur.mode == BlurMode.Linear) AdjustSlider(stringResource(R.string.blur_angle), edit.blur.angle, -90f..90f) { edit = edit.copy(blur = edit.blur.copy(angle = it)) }
                    }
                }
                AdjustTab.Curves -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ConnectedChoice(
                        listOf(R.string.curves_all to Color.White, R.string.curves_red to Color(0xFFE53935), R.string.curves_green to Color(0xFF43A047), R.string.curves_blue to Color(0xFF1E88E5)),
                        channel,
                    ) { channel = it }
                }
            }
            }
        }
        BottomActions(stringResource(R.string.action_cancel), stringResource(R.string.action_done), onCancel, { onDone(edit) }) {
            val tabs = listOf(AdjustTab.Adjust to Icons.Rounded.Tune, AdjustTab.Blur to Icons.Rounded.BlurOn, AdjustTab.Curves to Icons.Rounded.ShowChart)
            Row(horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween)) {
                tabs.forEachIndexed { i, (t, icon) ->
                    androidx.compose.material3.ToggleButton(
                        checked = tab == t,
                        onCheckedChange = { tab = t },
                        shapes = when (i) {
                            0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                            tabs.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = darkToggleColors(),
                        modifier = Modifier.height(44.dp),
                    ) { Icon(icon, null, Modifier.size(22.dp)) }
                }
            }
        }
    }
}

@Composable
private fun darkToggleColors() = androidx.compose.material3.ToggleButtonDefaults.colors(
    containerColor = Color.White.copy(alpha = 0.12f),
    contentColor = Color.White,
    checkedContainerColor = MaterialTheme.colorScheme.primary,
    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun ConnectedChoice(options: List<Pair<Int, Color?>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, (label, dot) ->
            androidx.compose.material3.ToggleButton(
                checked = selected == i,
                onCheckedChange = { onSelect(i) },
                shapes = when (i) {
                    0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = darkToggleColors(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(48.dp),
            ) {
                if (dot != null) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(dot).border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape))
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
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
