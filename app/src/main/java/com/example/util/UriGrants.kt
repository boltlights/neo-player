package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import timber.log.Timber

/**
 * Manages the app's persisted Storage Access Framework permissions.
 *
 * Android keeps at most [PLATFORM_LIMIT] persisted URI permissions per app and silently drops the
 * oldest when a new one is taken. Neo used to persist one permission per imported file, so after ~500
 * imports the oldest grant to go was the music *folder*: every folder sync then failed with
 * "Permission Denial". This class keeps folder (tree) grants safe by:
 *  - reading picked files through an already-granted folder instead of persisting a per-file grant;
 *  - releasing Neo's own oldest per-file grants before the platform would evict anything.
 */
object UriGrants {
    private const val PLATFORM_LIMIT = 512
    /** Free slots kept so a newly picked folder never pushes another grant out. */
    private const val HEADROOM = 24
    private const val READ = Intent.FLAG_GRANT_READ_URI_PERMISSION

    private fun persisted(context: Context) =
        context.contentResolver.persistedUriPermissions.filter { it.isReadPermission }

    private fun isTree(uri: Uri): Boolean = try {
        DocumentsContract.isTreeUri(uri)
    } catch (e: Exception) {
        false
    }

    /** True if Neo currently holds a persisted read grant for this folder. */
    fun hasFolderAccess(context: Context, treeUri: String): Boolean =
        persisted(context).any { it.uri.toString() == treeUri }

    /**
     * Returns the folder tree held by Neo that contains [docUri], or null. A plain document URI from
     * the file picker has no tree, so containment is decided by document ID on the same provider.
     */
    private fun coveringTree(context: Context, docUri: Uri): Uri? {
        if (isTree(docUri) || !DocumentsContract.isDocumentUri(context, docUri)) return null
        val docId = try { DocumentsContract.getDocumentId(docUri) } catch (e: Exception) { return null }
        return persisted(context).map { it.uri }.firstOrNull { tree ->
            isTree(tree) && tree.authority == docUri.authority && try {
                val treeId = DocumentsContract.getTreeDocumentId(tree)
                docId == treeId || docId.startsWith(treeId.trimEnd('/') + "/")
            } catch (e: Exception) {
                false
            }
        }
    }

    /** Persists a picked folder, first making room so it cannot be the grant that gets evicted. */
    fun takeFolder(context: Context, treeUri: Uri) {
        makeRoom(context)
        context.contentResolver.takePersistableUriPermission(treeUri, READ)
    }

    /**
     * Prepares a file picked with the document picker for import. Files inside a folder Neo already
     * holds are rewritten to that folder's URI (no new grant, and it matches what the folder sync
     * finds, so no duplicate row). Other files get a persisted grant, after making room.
     */
    fun prepareImportedFile(context: Context, uri: Uri): Uri {
        coveringTree(context, uri)?.let { tree ->
            val docId = DocumentsContract.getDocumentId(uri)
            return DocumentsContract.buildDocumentUriUsingTree(tree, docId)
        }
        try {
            makeRoom(context)
            context.contentResolver.takePersistableUriPermission(uri, READ)
        } catch (e: Exception) {
            Timber.tag("UriGrants").w(e, "Could not persist permission for %s", uri)
        }
        return uri
    }

    /**
     * Releases Neo's oldest per-file grants until there is headroom below the platform limit.
     * Folder grants are never released here. Files covered by a held folder go first, since they
     * stay readable through that folder.
     */
    private fun makeRoom(context: Context) {
        val all = persisted(context)
        val excess = all.size - (PLATFORM_LIMIT - HEADROOM)
        if (excess <= 0) return
        val files = all.filter { !isTree(it.uri) }
        val (covered, uncovered) = files.partition { coveringTree(context, it.uri) != null }
        val victims = (covered.sortedBy { it.persistedTime } + uncovered.sortedBy { it.persistedTime }).take(excess)
        for (p in victims) {
            try {
                context.contentResolver.releasePersistableUriPermission(p.uri, READ)
            } catch (e: Exception) {
                Timber.tag("UriGrants").w(e, "Could not release %s", p.uri)
            }
        }
        Timber.tag("UriGrants").i("Released %d old file permissions to stay under the system limit", victims.size)
    }
}
