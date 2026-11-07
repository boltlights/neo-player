package com.example.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

object DirectoryScanner {

    fun getPathFromTreeUri(uri: Uri): String? {
        val path = uri.path ?: return null
        if (path.startsWith("/tree/primary:")) {
            return "/storage/emulated/0/" + path.removePrefix("/tree/primary:")
        } else if (path.startsWith("/tree/raw:")) {
            return path.removePrefix("/tree/raw:")
        } else if (path.startsWith("/tree/")) {
            val suffix = path.removePrefix("/tree/")
            val parts = suffix.split(":", limit = 2)
            if (parts.size == 2) {
                return "/storage/" + parts[0] + "/" + parts[1]
            }
        }
        return null
    }

    private fun getDocumentLastModified(context: Context, docUri: Uri): Long {
        try {
            context.contentResolver.query(
                docUri,
                arrayOf(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0)
                }
            }
        } catch (e: Exception) {}
        return 0L
    }

    suspend fun scanFolders(
        context: Context,
        includedUris: Set<String>,
        excludedUris: Set<String>,
        useCache: Boolean = false,
        onAudioFileFound: suspend (Uri, String?, Long) -> Unit // (contentUri, filePath)
    ) {
        if (useCache) {
            FileTreeCache.load(context)
        }
        val foundFiles = mutableListOf<Triple<Uri, String?, Long>>()

        for (uriString in includedUris) {
            val uri = Uri.parse(uriString)
            
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val rootDir = File(path)
                    if (rootDir.exists() && rootDir.isDirectory && rootDir.canRead()) {
                        scanLocalFileDirRecursively(rootDir, excludedUris, foundFiles)
                        continue
                    }
                }
            }
            
            // For content:// URIs and fallbacks
            val root = try {
                DocumentFile.fromTreeUri(context, uri)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("DirectoryScanner").w(e, "Error resolving DocumentFile for %s", uri)
                null
            } ?: continue
            
            val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            scanDocumentFileFast(context, uri, docId, excludedUris, foundFiles, useCache)
        }

        if (useCache) {
            FileTreeCache.save(context)
        }

        // Sort all files naturally across folders
        val sortedFiles = foundFiles.sortedWith(
            compareBy(NaturalOrderComparator) { pair ->
                pair.second ?: pair.first.toString()
            }
        )

        for (file in sortedFiles) {
            onAudioFileFound(file.first, file.second, file.third)
        }
    }

    private fun scanLocalFileDirRecursively(
        dir: File,
        excludedUris: Set<String>,
        results: MutableList<Triple<Uri, String?, Long>>
    ) {
        if (excludedUris.contains(Uri.fromFile(dir).toString())) return
        val list = dir.listFiles() ?: return

        // Sort files and subdirectories naturally
        val sortedList = list.sortedWith(compareBy(NaturalOrderComparator) { it.name })

        // First process audio files in natural order
        for (file in sortedList) {
            if (file.isFile && isAudioFile(file.name)) {
                results.add(Triple(Uri.fromFile(file), file.absolutePath, file.lastModified()))
            }
        }

        // Then recurse into subdirectories in natural order
        for (file in sortedList) {
            if (file.isDirectory) {
                scanLocalFileDirRecursively(file, excludedUris, results)
            }
        }
    }


    private fun scanDocumentFileFast(
        context: Context,
        treeUri: Uri,
        documentId: String,
        excludedUris: Set<String>,
        results: MutableList<Triple<Uri, String?, Long>>,
        useCache: Boolean = false
    ) {
        val dirDocUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val currentDirModified = if (useCache) getDocumentLastModified(context, dirDocUri) else 0L

        if (useCache && currentDirModified > 0L) {
            val cached = FileTreeCache.get(documentId)
            if (cached != null && cached.lastModified == currentDirModified) {
                // Cache hit! Instant bypass of ContentProvider Binder calls
                for (cachedFile in cached.audioFiles) {
                    results.add(Triple(Uri.parse(cachedFile.uriString), cachedFile.filePath, cachedFile.lastModified))
                }
                for (dirId in cached.subDirIds) {
                    val childDirUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, dirId)
                    if (!excludedUris.contains(childDirUri.toString())) {
                        scanDocumentFileFast(context, treeUri, dirId, excludedUris, results, useCache)
                    }
                }
                return
            }
        }

        val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
            android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val files = mutableListOf<Triple<String, String, Pair<String, Long>>>()
        val dirs = mutableListOf<String>()

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)
                    val mime = cursor.getString(mimeIndex)

                    if (mime == android.provider.DocumentsContract.Document.MIME_TYPE_DIR) {
                        dirs.add(id)
                    } else {
                        files.add(Triple(id, name, Pair(mime, cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)))))
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            timber.log.Timber.tag("DirectoryScanner").w(e, "Error querying child documents for %s", documentId)
            return
        }

        files.sortWith(compareBy(NaturalOrderComparator) { it.second ?: "" })
        dirs.sortWith(compareBy(NaturalOrderComparator) { it ?: "" })

        val cachedAudioFiles = mutableListOf<CachedAudioFile>()
        for (file in files) {
            val name = file.second
            val mime = file.third.first
            val lastModified = file.third.second
            if ((name != null && isAudioFile(name)) || mime.startsWith("audio/") || mime == "application/ogg") {
                val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, file.first)
                results.add(Triple(docUri, null, lastModified))
                if (useCache) {
                    cachedAudioFiles.add(CachedAudioFile(docUri.toString(), null, lastModified))
                }
            }
        }

        if (useCache) {
            FileTreeCache.put(documentId, CachedDirEntry(currentDirModified, cachedAudioFiles, dirs))
        }

        for (dirId in dirs) {
            val dirUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, dirId)
            if (!excludedUris.contains(dirUri.toString())) {
                scanDocumentFileFast(context, treeUri, dirId, excludedUris, results, useCache)
            }
        }
    }

    private fun scanDocumentFileRecursively(
        dir: DocumentFile,
        excludedUris: Set<String>,
        results: MutableList<Triple<Uri, String?, Long>>
    ) {
        if (excludedUris.contains(dir.uri.toString())) return
        
        val files = dir.listFiles()
        // Sort DocumentFiles by natural order of their names
        val sortedFiles = files.sortedWith(compareBy(NaturalOrderComparator) { it.name ?: "" })

        // First process audio files in natural order
        for (file in sortedFiles) {
            if (!file.isDirectory) {
                val name = file.name
                if ((name != null && isAudioFile(name)) || file.type?.startsWith("audio/") == true || file.type == "application/ogg") {
                    results.add(Triple(file.uri, null, file.lastModified()))
                }
            }
        }

        // Then recurse into subdirectories
        for (file in sortedFiles) {
            if (file.isDirectory) {
                scanDocumentFileRecursively(file, excludedUris, results)
            }
        }
    }

    fun isAudioFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".ogg") ||
               lower.endsWith(".m4a") || lower.endsWith(".wav") || lower.endsWith(".aac") ||
               lower.endsWith(".opus") || lower.endsWith(".wma")
    }
}
