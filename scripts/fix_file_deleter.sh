#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/util/FileDeleter.kt
package com.example.util

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import java.io.File
import android.os.Build
import android.provider.MediaStore
import android.app.RecoverableSecurityException

object FileDeleter {
    @Throws(Exception::class)
    fun deleteSafely(context: Context, path: String): IntentSender? {
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: SecurityException) {
                    val intent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    return intent.intentSender
                }
            } else {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                        return e.userAction.actionIntent.intentSender
                    }
                    throw e
                }
            }
        } else {
            val file = File(path)
            if (file.exists()) {
                // Delete directly instead of using DATA column which is deprecated
                val deleted = file.delete()
                if (deleted) {
                    try {
                        // Attempt to remove from MediaStore if possible
                        context.contentResolver.delete(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.MediaColumns.DATA + "=?",
                            arrayOf(path)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return null
    }
}
INNER_EOF
