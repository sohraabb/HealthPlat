package com.bonyad.healthplat.logging

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Timber tree that appends logs to a plain-text file so testers can send their
 * logs back without needing logcat/adb.
 *
 * File location (app-private external storage — no runtime permission needed):
 *   Android/data/com.bonyad.healthplat/files/logs/healthplat.txt
 *
 * Use [LogFiles] to locate/share the file. Writes run on a single background
 * thread, and the file is rotated once it passes [MAX_FILE_BYTES] so it can
 * never grow without bound. Logging failures are swallowed — never crash the app.
 */
class FileLoggingTree(context: Context) : Timber.Tree() {

    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val timestampFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Format off the executor thread is fine — message/tag are immutable here.
        val line = buildString {
            append(timestampFormat.format(Date()))
            append(' ')
            append(priorityChar(priority))
            append('/')
            append(tag ?: "App")
            append(": ")
            append(message)
            if (t != null) {
                append('\n')
                append(Log.getStackTraceString(t))
            }
            append('\n')
        }
        executor.execute {
            try {
                val file = LogFiles.currentFile(appContext)
                rotateIfNeeded(file)
                file.appendText(line)
            } catch (_: Throwable) {
                // Never let logging crash the app.
            }
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_FILE_BYTES) {
            val backup = LogFiles.previousFile(appContext)
            if (backup.exists()) backup.delete()
            file.renameTo(backup)
        }
    }

    private fun priorityChar(priority: Int) = when (priority) {
        Log.VERBOSE -> 'V'
        Log.DEBUG -> 'D'
        Log.INFO -> 'I'
        Log.WARN -> 'W'
        Log.ERROR -> 'E'
        Log.ASSERT -> 'A'
        else -> '?'
    }

    companion object {
        private const val MAX_FILE_BYTES = 5L * 1024 * 1024 // 5 MB
    }
}
