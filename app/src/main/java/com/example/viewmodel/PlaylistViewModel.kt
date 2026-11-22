package com.example.viewmodel

import android.app.Application
import com.example.data.MusicRepository
import com.example.data.PlaylistEntity
import com.example.data.PlaylistTrackCrossRef
import com.example.data.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Domain-specific ViewModel handling playlist CRUD operations and track associations.
 */
class PlaylistViewModel(
    private val application: Application,
    private val repository: MusicRepository,
    private val scope: CoroutineScope
) {
    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPlaylistTrackCrossRefs: StateFlow<List<PlaylistTrackCrossRef>> = repository.allPlaylistTrackCrossRefs.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedPlaylistId = MutableStateFlow<Int?>(null)
    val selectedPlaylistId: StateFlow<Int?> = _selectedPlaylistId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistTracks: StateFlow<List<TrackEntity>> = _selectedPlaylistId
        .flatMapLatest { id ->
            when (id) {
                null -> flowOf(emptyList())
                -1 -> repository.recentlyPlayed
                -2 -> repository.mostPlayed
                -3 -> repository.recentlyAdded
                else -> repository.getTracksForPlaylist(id)
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activePlaylistForViewer = MutableStateFlow<PlaylistEntity?>(null)
    val activePlaylistForViewer: StateFlow<PlaylistEntity?> = _activePlaylistForViewer.asStateFlow()

    fun selectPlaylist(id: Int?) {
        _selectedPlaylistId.value = id
    }

    fun setActivePlaylistForViewer(playlist: PlaylistEntity?) {
        _activePlaylistForViewer.value = playlist
    }

    fun createPlaylist(name: String) {
        scope.launch(Dispatchers.IO) {
            try {
                repository.insertPlaylist(PlaylistEntity(name = name))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to create playlist: %s", name)
            }
        }
    }

    fun renamePlaylist(playlist: PlaylistEntity, newName: String) {
        scope.launch(Dispatchers.IO) {
            try {
                repository.updatePlaylist(playlist.copy(name = newName))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to rename playlist: %s", playlist.name)
            }
        }
    }

    fun updatePlaylistCoverArt(playlist: PlaylistEntity, uri: String?) {
        scope.launch(Dispatchers.IO) {
            try {
                val updated = playlist.copy(coverArtUri = uri)
                repository.updatePlaylist(updated)
                if (_activePlaylistForViewer.value?.id == updated.id) {
                    _activePlaylistForViewer.value = updated
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to update cover art for: %s", playlist.name)
            }
        }
    }

    fun deletePlaylist(playlistId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                repository.deletePlaylist(playlistId)
                if (_selectedPlaylistId.value == playlistId) {
                    _selectedPlaylistId.value = null
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to delete playlist: %s", playlistId)
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Int, trackId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                repository.addTrackToPlaylist(playlistId, trackId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to add track %s to playlist %s", trackId, playlistId)
            }
        }
    }

    fun addTracksToPlaylist(playlistId: Int, trackIds: List<Int>) {
        scope.launch(Dispatchers.IO) {
            try {
                trackIds.forEach { trackId ->
                    repository.addTrackToPlaylist(playlistId, trackId)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to add %s tracks to playlist %s", trackIds.size, playlistId)
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, trackId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                repository.removeTrackFromPlaylist(playlistId, trackId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to remove track %s from playlist %s", trackId, playlistId)
            }
        }
    }

    fun getDuplicateTrackCount(playlistId: Int, newTrackIds: List<Int>): Int {
        val existing = allPlaylistTrackCrossRefs.value
            .filter { it.playlistId == playlistId }
            .map { it.trackId }
            .toSet()
        return newTrackIds.count { it in existing }
    }

    fun exportPlaylistToM3u(
        context: android.content.Context,
        playlist: PlaylistEntity,
        tracks: List<TrackEntity>,
        targetUri: android.net.Uri,
        onResult: (Boolean, String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                    java.io.OutputStreamWriter(outStream, java.nio.charset.StandardCharsets.UTF_8).use { writer ->
                        writer.write("#EXTM3U\n")
                        writer.write("#PLAYLIST:${playlist.name}\n\n")
                        for (track in tracks) {
                            val durationSec = (track.durationMs / 1000).coerceAtLeast(0)
                            writer.write("#EXTINF:$durationSec,${track.artist} - ${track.title}\n")
                            // Folder-imported tracks have no file path; writing it produced blank entries.
                            writer.write("${track.filePath.ifBlank { track.sourceUri ?: "" }}\n")
                        }
                        writer.flush()
                    }
                }
                withContext(Dispatchers.Main) {
                    onResult(true, "Exported ${tracks.size} tracks to ${playlist.name}.m3u8")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to export playlist %s", playlist.name)
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Export failed")
                }
            }
        }
    }

    /** What importing one playlist file did. */
    private data class ImportOutcome(val name: String, val matched: Int, val missing: Int, val updated: Boolean)

    /**
     * Imports one .m3u/.m3u8. A playlist with the same name (case-insensitive) is updated in
     * place — its songs and order replaced — instead of a second copy being made, so importing
     * the same file again after reordering on the PC just brings the new order over.
     * Returns null when nothing in the file matched (an existing playlist is then left alone).
     */
    private suspend fun importOne(
        context: android.content.Context,
        uri: android.net.Uri,
        fallbackName: String?,
        allTracks: List<TrackEntity>
    ): ImportOutcome? {
        val lines = context.contentResolver.openInputStream(uri)?.use { s ->
            java.io.BufferedReader(java.io.InputStreamReader(s, java.nio.charset.StandardCharsets.UTF_8)).readLines()
        } ?: return null
        val parsed = com.example.util.M3uPlaylist.parse(lines)
        if (parsed.entries.isEmpty()) return null
        val name = parsed.name
            ?: fallbackName
            ?: androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)?.name?.substringBeforeLast(".")
            ?: "Imported Playlist"
        val matched = com.example.util.M3uPlaylist.match(parsed.entries, allTracks) { listOf(it.filePath, it.sourceUri) }
        if (matched.isEmpty()) return null
        val existing = repository.allPlaylists.first().firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            repository.rewritePlaylist(existing, matched)
        } else {
            val id = repository.insertPlaylist(PlaylistEntity(name = name)).toInt()
            matched.forEach { repository.addTrackToPlaylist(id, it.id) }
        }
        return ImportOutcome(name, matched.size, parsed.entries.size - matched.size, existing != null)
    }

    fun importPlaylistFromM3u(
        context: android.content.Context,
        uri: android.net.Uri,
        onResult: (Boolean, String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val r = importOne(context, uri, null, repository.allTracks.first())
                val msg = when {
                    r == null -> "No songs from this playlist were found in your library"
                    r.updated -> "Updated '${r.name}': ${r.matched} songs" + if (r.missing > 0) " (${r.missing} not found)" else ""
                    else -> "Imported '${r.name}' with ${r.matched} songs" + if (r.missing > 0) " (${r.missing} not found)" else ""
                }
                withContext(Dispatchers.Main) { onResult(r != null, msg) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to import M3U")
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Import failed")
                }
            }
        }
    }

    // ── Phone sync folder ─────────────────────────────────────────────────────
    // The folder Neo for Windows keeps in sync ("Phone sync": Music\… + Playlists\*.m3u8),
    // chosen once; "Refresh from folder" re-imports every playlist file in it.

    private val syncPrefs by lazy { application.getSharedPreferences("neo_phone_sync", android.content.Context.MODE_PRIVATE) }

    private val _syncFolder = MutableStateFlow<String?>(null)
    /** The chosen sync folder's tree URI, or null. */
    val syncFolder: StateFlow<String?> = _syncFolder.asStateFlow()

    init {
        _syncFolder.value = runCatching { syncPrefs.getString("folder", null) }.getOrNull()
    }

    fun setSyncFolder(treeUri: android.net.Uri) {
        syncPrefs.edit().putString("folder", treeUri.toString()).apply()
        _syncFolder.value = treeUri.toString()
    }

    /** Playlist files in the sync folder: its `Playlists` subfolder if there is one, else the folder itself. */
    private fun playlistFiles(context: android.content.Context, treeUri: android.net.Uri): List<androidx.documentfile.provider.DocumentFile> {
        val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val isPlaylist = { f: androidx.documentfile.provider.DocumentFile ->
            f.isFile && (f.name?.lowercase()?.let { it.endsWith(".m3u8") || it.endsWith(".m3u") } == true)
        }
        val sub = root.listFiles().firstOrNull { it.isDirectory && it.name.equals("Playlists", ignoreCase = true) }
        return (sub?.listFiles()?.filter(isPlaylist) ?: emptyList()) + root.listFiles().filter(isPlaylist)
    }

    fun refreshFromSyncFolder(context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        val folder = _syncFolder.value ?: return onResult(false, "Choose the sync folder first")
        scope.launch(Dispatchers.IO) {
            try {
                val files = playlistFiles(context, android.net.Uri.parse(folder))
                if (files.isEmpty()) {
                    withContext(Dispatchers.Main) { onResult(false, "No playlist files (.m3u8) in the sync folder") }
                    return@launch
                }
                val allTracks = repository.allTracks.first()
                var updated = 0
                var added = 0
                var missing = 0
                val empty = mutableListOf<String>()
                for (f in files.sortedBy { it.name?.lowercase() }) {
                    val r = importOne(context, f.uri, f.name?.substringBeforeLast("."), allTracks)
                    when {
                        r == null -> empty += f.name.orEmpty()
                        r.updated -> updated++
                        else -> added++
                    }
                    missing += r?.missing ?: 0
                }
                val parts = buildList {
                    if (updated > 0) add("$updated updated")
                    if (added > 0) add("$added new")
                    if (missing > 0) add("$missing songs not found — scan your music first")
                    if (empty.isNotEmpty()) add("${empty.size} with no songs found")
                }
                withContext(Dispatchers.Main) {
                    onResult(updated + added > 0, "Playlists: " + parts.joinToString(", ").ifEmpty { "nothing to do" })
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to refresh from sync folder")
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "Refresh failed") }
            }
        }
    }
}
