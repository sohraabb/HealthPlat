package com.bonyad.healthplat.logging

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.File

/**
 * Locates the log files written by [FileLoggingTree] and exports them to the
 * public Downloads folder so a user can find and send them — no in-app UI.
 */
object LogFiles {

    private const val DIR_NAME = "logs"
    private const val CURRENT_NAME = "healthplat.txt"
    private const val PREVIOUS_NAME = "healthplat.prev.txt"
    private const val EXPORT_NAME = "healthplat_log.txt"

    private fun dir(context: Context): File =
        File(context.getExternalFilesDir(null), DIR_NAME).apply { mkdirs() }

    fun currentFile(context: Context): File = File(dir(context), CURRENT_NAME)

    fun previousFile(context: Context): File = File(dir(context), PREVIOUS_NAME)

    /**
     * Copies the accumulated log into the public Downloads folder as [EXPORT_NAME]
     * so the user can open Downloads and send the file. Silent and best-effort:
     * runs off the caller's thread and swallows any failure.
     *
     * API 29+ uses MediaStore (no storage permission needed). On older versions it
     * writes directly to Downloads, which only succeeds if WRITE_EXTERNAL_STORAGE
     * was already granted — otherwise it is silently skipped (we never prompt).
     */
    fun exportToDownloads(context: Context) {
        val appContext = context.applicationContext
        Thread {
            try {
                val source = currentFile(appContext)
                if (!source.exists() || source.length() == 0L) return@Thread
                val bytes = source.readBytes()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    exportViaMediaStore(appContext, bytes)
                } else {
                    exportLegacy(bytes)
                }
                Timber.i("📝 Log exported to Downloads/$EXPORT_NAME")
            } catch (t: Throwable) {
                Timber.w(t, "Failed to export log to Downloads")
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportViaMediaStore(context: Context, bytes: ByteArray) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        // Remove any previous export so copies don't pile up as "healthplat_log (1).txt".
        resolver.delete(
            collection,
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(EXPORT_NAME)
        )

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, EXPORT_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    private fun exportLegacy(bytes: ByteArray) {
        @Suppress("DEPRECATION")
        val downloads =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloads.mkdirs()
        File(downloads, EXPORT_NAME).writeBytes(bytes)
    }
}
