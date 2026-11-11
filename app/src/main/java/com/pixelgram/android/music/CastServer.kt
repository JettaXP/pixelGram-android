package com.pixelgram.android.music

import android.content.Context
import android.net.ConnectivityManager
import java.io.File
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class CastServer(private val context: Context) {
    private var server: ServerSocket? = null
    private val files = ConcurrentHashMap<String, Pair<File, String>>()
    private val pool = Executors.newCachedThreadPool()
    private val random = SecureRandom()

    @Synchronized
    fun publish(file: File, mime: String): String? {
        val host = address() ?: return null
        val socket = server ?: ServerSocket(0).also {
            server = it
            pool.execute { accept(it) }
        }
        val token = ByteArray(12).also(random::nextBytes).joinToString("") { "%02x".format(it) }
        files[token] = file to mime
        return "http://$host:${socket.localPort}/$token/${android.net.Uri.encode(file.name)}"
    }

    @Synchronized
    fun clear() {
        files.clear()
    }

    @Synchronized
    fun close() {
        files.clear()
        runCatching { server?.close() }
        server = null
    }

    private fun address(): String? {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val links = manager.getLinkProperties(manager.activeNetwork) ?: return null
        return links.linkAddresses.map { it.address }.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
    }

    private fun accept(socket: ServerSocket) {
        while (!socket.isClosed) {
            val client = runCatching { socket.accept() }.getOrNull() ?: break
            pool.execute { runCatching { serve(client) }; runCatching { client.close() } }
        }
    }

    private fun serve(client: Socket) {
        client.soTimeout = 30_000
        val input = client.getInputStream().bufferedReader(Charsets.ISO_8859_1)
        val request = input.readLine() ?: return
        val headers = generateSequence { input.readLine()?.takeIf { it.isNotEmpty() } }
            .mapNotNull { line -> line.indexOf(':').takeIf { it > 0 }?.let { line.substring(0, it).trim().lowercase() to line.substring(it + 1).trim() } }
            .toMap()
        val parts = request.split(' ')
        val method = parts.getOrNull(0).orEmpty()
        val token = parts.getOrNull(1)?.trimStart('/')?.substringBefore('/').orEmpty()
        val out = client.getOutputStream().buffered()
        val entry = files[token]
        if (entry == null || !entry.first.exists() || (method != "GET" && method != "HEAD")) {
            out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
            out.flush()
            return
        }
        val (file, mime) = entry
        val length = file.length()
        val range = headers["range"]?.removePrefix("bytes=")?.split('-')
        val start = range?.getOrNull(0)?.toLongOrNull() ?: 0L
        val end = range?.getOrNull(1)?.toLongOrNull()?.coerceAtMost(length - 1) ?: (length - 1)
        val partial = range != null
        val size = (end - start + 1).coerceAtLeast(0)
        val head = buildString {
            append(if (partial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mime\r\n")
            append("Accept-Ranges: bytes\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Content-Length: $size\r\n")
            if (partial) append("Content-Range: bytes $start-$end/$length\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(head.toByteArray())
        if (method == "GET" && size > 0) {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buffer = ByteArray(64 * 1024)
                var left = size
                while (left > 0) {
                    val read = raf.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    left -= read
                }
            }
        }
        out.flush()
    }
}
