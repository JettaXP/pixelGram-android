package com.pixelgram.android.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import java.io.File

@Composable
fun CameraCapture(onDismiss: () -> Unit, onDone: (List<String>) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) granted = true else onDismiss() }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    val shots = remember { mutableStateListOf<String>() }
    var reviewing by remember { mutableStateOf<Int?>(null) }
    var flash by remember { mutableStateOf(false) }
    var front by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }
    LaunchedEffect(granted) { if (granted) controller.bindToLifecycle(lifecycle) }
    LaunchedEffect(front) { controller.cameraSelector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA }
    LaunchedEffect(flash) { controller.imageCaptureFlashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF }
    DisposableEffect(Unit) { onDispose { controller.unbind() } }
    val capture = {
        if (!busy && shots.size < MaxShots) {
            busy = true
            val target = File(context.cacheDir, "camera-${System.currentTimeMillis()}.jpg")
            controller.takePicture(
                ImageCapture.OutputFileOptions.Builder(target).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        busy = false
                        shots += target.absolutePath
                        reviewing = shots.lastIndex
                    }

                    override fun onError(exception: ImageCaptureException) {
                        busy = false
                    }
                },
            )
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (granted) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AnimatedContent(
                reviewing?.let { shots.getOrNull(it) },
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
                label = "review",
                modifier = Modifier.fillMaxSize(),
            ) { shown ->
                if (shown != null) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        AsyncImage(File(shown), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            ) {
                RoundAction(Icons.Rounded.Close, stringResource(R.string.action_close)) { if (reviewing != null) reviewing = null else onDismiss() }
                Spacer(Modifier.weight(1f))
                val review = reviewing
                if (review != null) {
                    RoundAction(Icons.Rounded.Delete, stringResource(R.string.action_delete)) {
                        if (review in shots.indices) shots.removeAt(review)
                        reviewing = null
                    }
                } else {
                    RoundAction(if (flash) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, null) { flash = !flash }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 18.dp),
            ) {
                AnimatedVisibility(shots.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        itemsIndexed(shots, key = { _, path -> path }) { index, path ->
                            AsyncImage(
                                File(path),
                                null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(if (reviewing == index) 2.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                                    .clickable { reviewing = index }
                                    .animateItem(),
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        if (reviewing == null) RoundAction(Icons.Rounded.Cameraswitch, null) { front = !front }
                    }
                    if (reviewing == null) {
                        Box(
                            Modifier.size(78.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).padding(8.dp).clip(CircleShape)
                                .background(if (busy) Color.White.copy(alpha = 0.5f) else Color.White)
                                .clickable(enabled = shots.size < MaxShots) { capture() },
                        )
                    } else {
                        Surface(
                            onClick = { reviewing = null },
                            enabled = shots.size < MaxShots,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White,
                            modifier = Modifier.size(78.dp),
                        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null, Modifier.size(36.dp)) } }
                    }
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(shots.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                            Surface(
                                onClick = { onDone(shots.toList()) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(56.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Check, stringResource(R.string.action_done))
                                    Text(
                                        shots.size.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 8.dp).graphicsLayer { alpha = if (shots.size > 1) 1f else 0f },
                                    )
                                }
                            }
                        }
                    }
                }
                Text("${shots.size} / $MaxShots", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String?, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))) {
        Icon(icon, label, tint = Color.White)
    }
}

private const val MaxShots = 10
