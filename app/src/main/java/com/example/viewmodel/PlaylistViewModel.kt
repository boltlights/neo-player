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
                            writer.write("${track.filePath}\n")
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

    fun importPlaylistFromM3u(
        context: android.content.Context,
        uri: android.net.Uri,
        onResult: (Boolean, String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val lines = mutableListOf<String>()
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    java.io.BufferedReader(java.io.InputStreamReader(inStream, java.nio.charset.StandardCharsets.UTF_8)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val l = line?.trim() ?: continue
                            if (l.isNotEmpty()) {
                                lines.add(l)
                            }
                        }
                    }
                }

                if (lines.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "M3U file is empty")
                    }
                    return@launch
                }

                var playlistName: String? = null
                for (l in lines) {
                    if (l.startsWith("#PLAYLIST:", ignoreCase = true)) {
                        playlistName = l.substringAfter(":").trim()
                        break
                    }
                }
                if (playlistName.isNullOrBlank()) {
                    val docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                    playlistName = docFile?.name?.substringBeforeLast(".") ?: "Imported Playlist"
                }

                val trackPaths = mutableListOf<String>()
                for (l in lines) {
                    if (!l.startsWith("#")) {
                        trackPaths.add(l)
                    }
                }

                val allTracks = repository.allTracks.first()
                val matchedTracks = mutableListOf<TrackEntity>()

                for (path in trackPaths) {
                    val fileName = java.io.File(path).name
                    val match = allTracks.firstOrNull { 
                        it.filePath.equals(path, ignoreCase = true) || 
                        java.io.File(it.filePath).name.equals(fileName, ignoreCase = true) 
                    }
                    if (match != null) {
                        matchedTracks.add(match)
                    }
                }

                if (matchedTracks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "No matching audio files from M3U found in library")
                    }
                    return@launch
                }

                val newPlaylistId = repository.insertPlaylist(PlaylistEntity(name = playlistName)).toInt()
                matchedTracks.forEach { track ->
                    repository.addTrackToPlaylist(newPlaylistId, track.id)
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Imported '$playlistName' with ${matchedTracks.size} tracks")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                timber.log.Timber.tag("PlaylistViewModel").e(e, "Failed to import M3U")
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Import failed")
                }
            }
        }
    }
}
