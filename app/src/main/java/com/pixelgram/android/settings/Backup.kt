package com.pixelgram.android.settings

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

sealed interface BackupRead {
    data class Valid(val values: Map<String, String>) : BackupRead
    data object Desktop : BackupRead
    data object Invalid : BackupRead
}

object Backup {
    private const val Extension = "backup"
    private const val OsKey = "os"
    private const val Os = "android"
    private val CodeChars = ('a'..'z') + ('0'..'9')

    fun isBackup(name: String) = name.endsWith(".$Extension", ignoreCase = true)

    suspend fun export(context: Context, store: AppearanceStore): File? = runCatching {
        val values = store.exportValues()
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            val code = (1..4).map { CodeChars.random() }.joinToString("")
            File(dir, "pixelgram-$code.$Extension").also { file ->
                file.bufferedWriter().use { out ->
                    out.write("$OsKey=$Os\n")
                    val props = Properties()
                    values.forEach { (k, v) -> props.setProperty(k, v) }
                    props.store(out, "pixelGram Android settings backup")
                }
            }
        }
    }.getOrNull()

    suspend fun read(file: File): BackupRead = withContext(Dispatchers.IO) {
        runCatching {
            val props = Properties()
            file.inputStream().use(props::load)
            when (props.getProperty(OsKey)) {
                Os -> BackupRead.Valid(props.stringPropertyNames().filter { it != OsKey }.associateWith { props.getProperty(it) })
                null -> BackupRead.Desktop
                else -> BackupRead.Invalid
            }
        }.getOrDefault(BackupRead.Invalid)
    }
}
