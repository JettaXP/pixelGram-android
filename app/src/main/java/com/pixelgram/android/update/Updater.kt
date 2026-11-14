package com.pixelgram.android.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.pixelgram.android.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Latest : UpdateState
    data class Available(val build: Int, val notes: String, val url: String, val size: Long) : UpdateState
    data class Downloading(val build: Int, val progress: Float) : UpdateState
    data class Ready(val build: Int, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

object Updater {
    private const val Release = "https://api.github.com/repos/JettaXP/pixelGram-android/releases/tags/alpha"
    private val BuildPattern = Regex("build (\\d+)")

    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set
    private var job: Job? = null

    fun check(scope: CoroutineScope) {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return
        state = UpdateState.Checking
        job = scope.launch {
            state = withContext(Dispatchers.IO) {
                runCatching {
                    val json = JSONObject(get(Release))
                    val body = json.optString("body")
                    val build = BuildPattern.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val assets = json.getJSONArray("assets")
                    val arm64 = Build.SUPPORTED_ABIS.firstOrNull() == "arm64-v8a"
                    var url = ""
                    var size = 0L
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name")
                        val wanted = if (arm64) name.endsWith("-arm64.apk") else name == "pixelGram-android.apk"
                        if (wanted || url.isEmpty()) {
                            url = asset.getString("browser_download_url")
                            size = asset.optLong("size")
                            if (wanted) break
                        }
                    }
                    val notes = body.substringAfter("\n", "").trim()
                    if (build > BuildConfig.VERSION_CODE && url.isNotEmpty()) UpdateState.Available(build, notes, url, size) else UpdateState.Latest
                }.getOrElse { UpdateState.Failed(it.message ?: "error") }
            }
        }
    }

    fun download(context: Context, scope: CoroutineScope) {
        val available = state as? UpdateState.Available ?: return
        state = UpdateState.Downloading(available.build, 0f)
        job = scope.launch {
            state = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.filesDir, "updates").apply { mkdirs() }
                    dir.listFiles()?.forEach { it.delete() }
                    val target = File(dir, "pixelGram-${available.build}.apk")
                    var conn = URL(available.url).openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 30_000
                    conn.readTimeout = 60_000
                    var hops = 0
                    while (conn.responseCode in 300..399 && hops++ < 5) {
                        val next = conn.getHeaderField("Location")
                        conn.disconnect()
                        conn = URL(next).openConnection() as HttpURLConnection
                    }
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: available.size
                    conn.inputStream.use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var done = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                done += read
                                if (total > 0) state = UpdateState.Downloading(available.build, (done.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                    conn.disconnect()
                    UpdateState.Ready(available.build, target)
                }.getOrElse { UpdateState.Failed(it.message ?: "error") }
            }
            (state as? UpdateState.Ready)?.let { install(context, it.file) }
        }
    }

    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun cancel() {
        job?.cancel()
        state = UpdateState.Idle
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.connectTimeout = 20_000
        conn.readTimeout = 20_000
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
