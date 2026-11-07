package com.example.util

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

sealed class DeleteResult {
    /** The file is gone. */
    object Deleted : DeleteResult()

    /** The system must ask the user first (scoped storage). [uri] lets callers batch several requests. */
    data class NeedsUserConfirmation(val uri: Uri, val intentSender: IntentSender) : DeleteResult()

    /** The file could not be deleted (no permission, read-only folder grant, I/O error...). */
    data class Failed(val error: Throwable? = null) : DeleteResult()
}

object FileDeleter {

    /**
     * Deletes the audio file behind a track. [target] should be the track's MediaStore/SAF content URI
     * when it has one: on Android 11+ apps cannot delete other apps' media through a raw file path, and
     * that call used to fail silently, so the song "deleted" from the library reappeared on the next scan.
     */
    fun delete(context: Context, target: String): DeleteResult {
        return try {
            if (target.startsWith("content://")) deleteContentUri(context, Uri.parse(target)) else deleteFilePath(context, target)
        } catch (e: Exception) {
            timber.log.Timber.tag("FileDeleter").w(e, "Failed to delete %s", target)
            DeleteResult.Failed(e)
        }
    }

    private fun deleteContentUri(context: Context, uri: Uri): DeleteResult {
        // Storage Access Framework document (folder-mode imports): ContentResolver.delete() is not supported
        // for these; it needs DocumentsContract and a *write* grant, which a read-only folder pick lacks.
        if (DocumentsContract.isDocumentUri(context, uri)) {
            return if (DocumentsContract.deleteDocument(context.contentResolver, uri)) DeleteResult.Deleted else DeleteResult.Failed()
        }
        return try {
            // 0 rows means the item no longer exists in MediaStore, i.e. the file is already gone.
            context.contentResolver.delete(uri, null, null)
            DeleteResult.Deleted
        } catch (e: SecurityException) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    DeleteResult.NeedsUserConfirmation(uri, MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException ->
                    DeleteResult.NeedsUserConfirmation(uri, e.userAction.actionIntent.intentSender)
                else -> DeleteResult.Failed(e)
            }
        }
    }

    private fun deleteFilePath(context: Context, path: String): DeleteResult {
        val file = File(if (path.startsWith("file://")) Uri.parse(path).path ?: path else path)
        if (!file.exists()) return DeleteResult.Deleted
        if (!file.delete()) return DeleteResult.Failed()
        try {
            context.contentResolver.delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaStore.MediaColumns.DATA + "=?",
                arrayOf(file.absolutePath)
            )
        } catch (e: Exception) {
            timber.log.Timber.tag("FileDeleter").w(e, "Failed to remove deleted file from MediaStore: %s", path)
        }
        return DeleteResult.Deleted
    }

    /** Combines several pending MediaStore confirmations into one system dialog (Android 11+). */
    fun combinedConfirmation(context: Context, uris: List<Uri>): IntentSender? {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    /** Only app-private files (e.g. generated sample-track artwork) may be removed alongside a track. */
    fun isAppPrivateFile(context: Context, path: String): Boolean {
        if (path.contains("://")) return false
        val canonical = try { File(path).canonicalPath } catch (e: Exception) { return false }
        return canonical.startsWith(context.filesDir.canonicalPath + File.separator) ||
            canonical.startsWith(context.cacheDir.canonicalPath + File.separator)
    }
}
