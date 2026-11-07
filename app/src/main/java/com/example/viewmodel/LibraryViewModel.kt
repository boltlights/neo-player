package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import com.example.data.IndexingState
import com.example.data.MusicDatabase
import com.example.data.MusicRepository
import com.example.data.SettingsRepository
import com.example.data.TrackEntity
import com.example.PlaybackController
import com.example.util.DeleteResult
import com.example.util.FileDeleter
import com.example.util.MediaStoreManager
import com.example.util.MetadataExtractor
import com.example.util.MusicGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.util.NaturalOrderComparator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Domain-specific ViewModel handling tracks browsing, search, sorting, deletion,
 * metadata extraction, and audio import/scanning.
 */
class LibraryViewModel(
    private val application: Application,
    private val repository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val mediaStoreManager: MediaStoreManager,
    private val playbackController: PlaybackController,
    private val scope: CoroutineScope
) {
    val allTracks: StateFlow<List<TrackEntity>> = repository.allTracks.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val sortOption: StateFlow<String> = settingsRepository.sortOption
    val libraryTabs: StateFlow<List<String>> = settingsRepository.libraryTabs

    val recentlyAdded: StateFlow<List<TrackEntity>> = repository.recentlyAdded.stateIn(
        scope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val mostPlayed: StateFlow<List<TrackEntity>> = repository.mostPlayed.stateIn(
        scope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val recentlyPlayed: StateFlow<List<TrackEntity>> = repository.recentlyPlayed.stateIn(
        scope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val sortedTracks: StateFlow<List<TrackEntity>> = combine(allTracks, sortOption, settingsRepository.intelligentSorting) { tracks, sort, intelligent ->
        if (intelligent) {
            when (sort) {
                "Filename" -> tracks.sortedWith(NaturalOrderComparator.FILENAME_COMPARATOR)
                "Title (A-Z)", "Title", "Title A-Z" -> tracks.sortedWith(NaturalOrderComparator.TITLE_ASC_COMPARATOR)
                "Title (Z-A)", "Title Z-A" -> tracks.sortedWith(NaturalOrderComparator.TITLE_DESC_COMPARATOR)
                "Artist" -> tracks.sortedWith(NaturalOrderComparator.ARTIST_COMPARATOR)
                "Album" -> tracks.sortedWith(NaturalOrderComparator.ALBUM_COMPARATOR)
                "Date Added (Newest)", "Date Added", "Newest" -> tracks.sortedByDescending { it.addedAt }
                "Date Added (Oldest)", "Oldest" -> tracks.sortedBy { it.addedAt }
                "Duration" -> tracks.sortedByDescending { it.durationMs }
                "Track Number" -> tracks.sortedWith(NaturalOrderComparator.TRACK_NUMBER_COMPARATOR)
                else -> tracks
            }
        } else {
            when (sort) {
                "Filename" -> tracks.sortedBy { it.filePath.substringAfterLast('/') }
                "Title (A-Z)", "Title", "Title A-Z" -> tracks.sortedBy { it.title.lowercase() }
                "Title (Z-A)", "Title Z-A" -> tracks.sortedByDescending { it.title.lowercase() }
                "Artist" -> tracks.sortedBy { it.artist.lowercase() }
                "Album" -> tracks.sortedBy { it.album.lowercase() }
                "Date Added (Newest)", "Date Added", "Newest" -> tracks.sortedByDescending { it.addedAt }
                "Date Added (Oldest)", "Oldest" -> tracks.sortedBy { it.addedAt }
                "Duration" -> tracks.sortedByDescending { it.durationMs }
                "Track Number" -> tracks.sortedBy { it.trackNumber ?: 0 }
                else -> tracks
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultTracks: StateFlow<List<TrackEntity>> = combine(sortedTracks, searchQuery) { tracks, query ->
        if (query.isBlank()) {
            tracks
        } else {
            val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (terms.isEmpty()) {
                tracks
            } else {
                tracks.filter { track ->
                    val title = track.title.lowercase()
                    val artist = track.artist.lowercase()
                    val album = track.album.lowercase()
                    val albumArtist = track.albumArtist?.lowercase() ?: ""
                    val genre = track.genre?.lowercase() ?: ""
                    val yearStr = track.year?.toString() ?: ""
                    val customTags = track.customTags?.lowercase() ?: ""
                    val lyrics = track.lyrics?.lowercase() ?: ""
                    val fileName = track.filePath.substringAfterLast('/').lowercase()

                    terms.all { rawTerm ->
                        val term = rawTerm.removePrefix("#").removePrefix("tag:").removePrefix("genre:").removePrefix("year:")
                        if (term.isEmpty()) return@all true

                        when {
                            rawTerm.startsWith("year:") -> yearStr.contains(term)
                            rawTerm.startsWith("genre:") -> genre.contains(term)
                            rawTerm.startsWith("tag:") -> customTags.contains(term)
                            else -> {
                                title.contains(term) ||
                                artist.contains(term) ||
                                album.contains(term) ||
                                albumArtist.contains(term) ||
                                genre.contains(term) ||
                                yearStr.contains(term) ||
                                customTags.contains(term) ||
                                lyrics.contains(term) ||
                                fileName.contains(term)
                            }
                        }
                    }
                }
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val indexedAlbums: StateFlow<List<com.example.data.IndexedAlbum>> = searchResultTracks.map { tracks ->
        tracks.groupBy { it.album.ifBlank { "Unknown Album" } }
            .map { (albumName, albumTracks) ->
                val primaryArtist = albumTracks.firstOrNull { !it.albumArtist.isNullOrBlank() }?.albumArtist
                    ?: albumTracks.firstOrNull()?.artist
                    ?: "Unknown Artist"
                com.example.data.IndexedAlbum(
                    name = albumName,
                    artist = primaryArtist,
                    tracks = albumTracks,
                    year = null,
                    artPath = albumTracks.firstOrNull()?.artPath
                )
            }
            .sortedWith(compareBy(com.example.util.NaturalOrderComparator) { it.name })
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val indexedArtists: StateFlow<List<com.example.data.IndexedArtist>> = searchResultTracks.map { tracks ->
        tracks.groupBy {
            val effective = if (!it.albumArtist.isNullOrBlank()) it.albumArtist!! else it.artist
            effective.ifBlank { "Unknown Artist" }
        }
            .map { (artistName, artistTracks) ->
                val distinctAlbums = artistTracks.map { it.album }.distinct().size
                com.example.data.IndexedArtist(
                    name = artistName,
                    tracks = artistTracks,
                    albumCount = distinctAlbums
                )
            }
            .sortedWith(compareBy(com.example.util.NaturalOrderComparator) { it.name })
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val indexedGenres: StateFlow<List<com.example.data.IndexedGenre>> = searchResultTracks.map { tracks ->
        tracks.groupBy { com.example.ui.screens.getGenreForTrack(it) }
            .map { (genreName, genreTracks) ->
                com.example.data.IndexedGenre(
                    name = genreName,
                    tracks = genreTracks
                )
            }
            .sortedWith(compareBy(com.example.util.NaturalOrderComparator) { it.name })
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isImporting: StateFlow<Boolean> = repository.indexingState.map { it is IndexingState.Indexing }
        .stateIn(scope, SharingStarted.Lazily, false)

    val importProgress: StateFlow<Float> = repository.indexingState.map { (it as? IndexingState.Indexing)?.progress ?: 0f }
        .stateIn(scope, SharingStarted.Lazily, 0f)

    private val _deleteIntentSender = MutableSharedFlow<IntentSender>()
    val deleteIntentSender = _deleteIntentSender.asSharedFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOption(option: String) {
        scope.launch { settingsRepository.setSortOption(option) }
    }

    fun updateLibraryTabs(tabs: List<String>) {
        scope.launch { settingsRepository.setLibraryTabs(tabs) }
    }

    fun updateLibraryOrder(orderedTracks: List<TrackEntity>) {
        scope.launch {
            val baseTime = System.currentTimeMillis()
            val updatedTracks = orderedTracks.mapIndexed { index, track ->
                track.copy(addedAt = baseTime - (index * 1000L))
            }
            repository.updateTracks(updatedTracks)
        }
    }

    fun fetchLyricsIfMissing(track: TrackEntity) {
        if (track.lyrics != null) return
        if (track.isMetadataPending) return

        scope.launch(Dispatchers.IO) {
            try {
                val extracted = MetadataExtractor.extract(application, track.sourceUri ?: track.filePath, track.filePath)
                val lyricsToSave = extracted.lyrics ?: ""
                val updatedTrack = track.copy(lyrics = lyricsToSave)
                repository.updateTrack(updatedTrack)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    /** Tracks whose files await the system "allow deletion?" dialog; their rows are removed only on OK. */
    private val pendingDeletionTracks = mutableListOf<TrackEntity>()

    fun deleteTrack(context: Context, track: TrackEntity, onStopPlayer: () -> Unit = {}) =
        deleteTracks(context, listOf(track), onStopPlayer)

    /**
     * Deletes the files first and removes a track from the library (which cascades to playlists) only
     * once its file is really gone. Previously the row was removed immediately, even when the system
     * dialog was cancelled or the delete silently failed, so songs vanished from playlists while their
     * files stayed on disk and came back on the next scan.
     */
    fun deleteTracks(context: Context, tracks: List<TrackEntity>, onStopPlayer: () -> Unit = {}) {
        if (tracks.isEmpty()) return
        scope.launch {
            val currentTrackId = playbackController.currentTrack.value?.id
            var stopPlayer = false
            for (track in tracks) {
                if (track.id == currentTrackId) {
                    stopPlayer = true
                } else {
                    playbackController.removeTrackFromQueue(track.id)
                }
            }
            if (stopPlayer) {
                playbackController.stopAndClear()
                onStopPlayer()
            }

            val needsConfirmation = mutableListOf<Pair<TrackEntity, DeleteResult.NeedsUserConfirmation>>()
            val failed = mutableListOf<TrackEntity>()
            withContext(Dispatchers.IO) {
                for (track in tracks) {
                    // Prefer the content URI: raw-path deletes of shared media fail on Android 11+.
                    val target = track.sourceUri?.takeIf { it.startsWith("content://") } ?: track.filePath
                    when (val result = FileDeleter.delete(context, target)) {
                        is DeleteResult.Deleted -> {
                            deleteAppPrivateArt(context, track)
                            repository.deleteTrack(track.id)
                        }
                        is DeleteResult.NeedsUserConfirmation -> needsConfirmation.add(track to result)
                        is DeleteResult.Failed -> failed.add(track)
                    }
                }
            }

            if (needsConfirmation.isNotEmpty()) {
                // One system dialog for the whole selection instead of one per song (Android 11+).
                val combined = if (needsConfirmation.size > 1) {
                    FileDeleter.combinedConfirmation(context, needsConfirmation.map { it.second.uri })
                } else null
                pendingDeletionTracks.clear()
                if (combined != null) {
                    pendingDeletionTracks.addAll(needsConfirmation.map { it.first })
                    _deleteIntentSender.emit(combined)
                } else {
                    // Android 10 can only confirm one item at a time; the rest stay in the library.
                    pendingDeletionTracks.add(needsConfirmation.first().first)
                    _deleteIntentSender.emit(needsConfirmation.first().second.intentSender)
                }
            }
            if (failed.isNotEmpty()) {
                val msg = if (failed.size == 1) "Couldn't delete \"${failed.first().title}\"" else "Couldn't delete ${failed.size} songs"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Called with the result of the system delete-confirmation dialog. */
    fun onDeleteConfirmationResult(context: Context, granted: Boolean) {
        val tracks = pendingDeletionTracks.toList()
        pendingDeletionTracks.clear()
        if (!granted || tracks.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            for (track in tracks) {
                deleteAppPrivateArt(context, track)
                repository.deleteTrack(track.id)
            }
        }
    }

    private fun deleteAppPrivateArt(context: Context, track: TrackEntity) {
        val art = track.artPath ?: return
        // Never touch shared files here: only app-generated artwork (sample tracks) is ours to remove.
        if (FileDeleter.isAppPrivateFile(context, art)) {
            try { java.io.File(art).delete() } catch (_: Exception) {}
        }
    }

    fun scanMediaStore(context: Context, onCompleted: (Int) -> Unit) {
        scope.launch {
            val included = settingsRepository.includedFolders.value
            val excluded = settingsRepository.excludedFolders.value
            val existing = repository.getAllTracksSync()
            val count = mediaStoreManager.scanMediaStore(context, included, excluded, existing)
            onCompleted(count)
        }
    }

    fun importAudioFiles(context: Context, uris: List<Uri>, onCompleted: (Int) -> Unit) {
        scope.launch {
            val existing = repository.getAllTracksSync()
            val count = mediaStoreManager.importAudioFiles(context, uris, existing)
            onCompleted(count)
        }
    }

    fun importAudioFolder(context: Context, folderUri: Uri, onCompleted: (Int) -> Unit) {
        val uriStr = folderUri.toString()
        if (!settingsRepository.includedFolders.value.contains(uriStr)) {
            scope.launch {
                settingsRepository.addIncludedFolder(uriStr)
                scanMediaStore(context, onCompleted)
            }
            return
        }
        scanMediaStore(context, onCompleted)
    }

    fun readdSampleTracks(context: Context, onComplete: () -> Unit = {}) {
        scope.launch {
            val database = MusicDatabase.getDatabase(context)
            MusicGenerator.readdSampleTracks(context, database.musicDao())
            onComplete()
        }
    }
}
