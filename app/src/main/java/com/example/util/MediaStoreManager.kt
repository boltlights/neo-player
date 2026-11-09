package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.MusicRepository
import com.example.data.TrackEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

class MediaStoreManager(
    private val repository: MusicRepository,
    private val settingsRepository: com.example.data.SettingsRepository? = null
) {

    private fun updateProgress(progress: Float) {
        if (progress >= 1f) {
            repository.updateIndexingState(com.example.data.IndexingState.Completed(null))
        } else {
            repository.updateIndexingState(com.example.data.IndexingState.Indexing(progress))
        }
    }
    
    private fun setImporting(importing: Boolean) {
        if (importing) {
            repository.updateIndexingState(com.example.data.IndexingState.Indexing(0f))
        } else {
            repository.updateIndexingState(com.example.data.IndexingState.Completed(null))
        }
    }

    /**
     * Verifies if an audio track's underlying file still exists and can be accessed.
     */
    private fun isTrackAccessible(context: Context, track: TrackEntity): Boolean {
        // 1. Direct file path check
        if (track.filePath.isNotEmpty() && !track.filePath.startsWith("content://")) {
            val file = File(track.filePath)
            if (file.exists() && file.canRead() && file.length() > 0) {
                return true
            }
        }

        // 2. Content URI check (MediaStore or Storage Access Framework)
        val uriStr = track.sourceUri ?: if (track.filePath.startsWith("content://")) track.filePath else null
        if (uriStr != null && uriStr.startsWith("content://")) {
            try {
                val uri = Uri.parse(uriStr)
                // use{}: the descriptor was leaked if reading its length threw.
                val len = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                if (len != null && (len > 0 || len == android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH)) {
                    return true
                }
            } catch (e: SecurityException) {
                // No permission is not the same as deleted: a lost folder grant used to make every
                // sync delete the folder's songs (and remove them from playlists). Keep the track;
                // Music Sources tells the user to grant the folder again.
                return true
            } catch (e: Exception) {
                return false
            }
        }

        // 3. Fallback file:// URI check
        if (uriStr != null && uriStr.startsWith("file://")) {
            val path = Uri.parse(uriStr).path
            if (path != null) {
                val file = File(path)
                return file.exists() && file.canRead() && file.length() > 0
            }
        }

        return false
    }

    /**
     * Finds an existing track in database that corresponds to the same audio file.
     */

    /**
     * Serializes all scans/imports. Without this, the MediaStore observer, auto-refresh and a manual
     * scan could run concurrently against the same stale snapshot and insert the same files twice.
     */
    private val scanMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * True when a file-path track lives on a storage volume that is currently not mounted (e.g. an SD
     * card or USB drive that was removed). Such tracks are temporarily unreachable, not deleted.
     */
    private fun isOnUnavailableVolume(track: TrackEntity): Boolean {
        val path = track.filePath
        if (path.isEmpty() || path.startsWith("content://") || path.startsWith("file://")) return false
        if (!path.startsWith("/storage/") && !path.startsWith("/mnt/")) return false
        return try {
            val state = android.os.Environment.getExternalStorageState(File(path))
            state != android.os.Environment.MEDIA_MOUNTED && state != android.os.Environment.MEDIA_MOUNTED_READ_ONLY
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Removes tracks whose files are gone. Deleting a track cascades to every playlist that contains it,
     * so this must never fire because storage is temporarily unavailable: tracks on an unmounted volume
     * are kept, and if *every* track looks inaccessible (permission revoked, storage not ready yet at
     * boot) the cleanup is skipped entirely instead of wiping the library and all playlists.
     */
    private suspend fun pruneInaccessibleTracks(context: Context, existingTracks: List<TrackEntity>): MutableList<TrackEntity> {
        val accessible = mutableListOf<TrackEntity>()
        val dead = mutableListOf<TrackEntity>()
        for (track in existingTracks) {
            if (isTrackAccessible(context, track) || isOnUnavailableVolume(track)) accessible.add(track) else dead.add(track)
        }
        if (dead.isNotEmpty() && accessible.isEmpty()) {
            timber.log.Timber.tag("MediaStoreManager").w(
                "All %d tracks appear inaccessible; storage or permission is likely unavailable. Skipping cleanup.", dead.size
            )
            return existingTracks.toMutableList()
        }
        for (track in dead) repository.deleteTrack(track.id)
        return accessible
    }

    private suspend fun removeDuplicateRows(tracks: MutableList<TrackEntity>) {
        val seen = HashSet<String>()
        val duplicates = tracks.filter { track ->
            val key = track.sourceUri?.takeIf { it.isNotEmpty() } ?: track.filePath
            key.isNotEmpty() && !seen.add(key)
        }
        for (dup in duplicates) {
            repository.deleteTrack(dup.id)
            tracks.remove(dup)
        }
    }

    /**
     * Cached metadata is reused only if the earlier read actually worked. A row saved after a failed
     * read (no duration, placeholder title) would otherwise stay blank forever, because the file's
     * modified date never changes; re-reading it lets a fixed reader repair it on the next sync.
     */
    private fun hasUsableMetadata(track: TrackEntity): Boolean =
        track.durationMs > 0L && track.title != "Unknown Title"

    private fun isPathUnder(path: String, folder: String): Boolean {
        val base = folder.trimEnd('/')
        return base.isNotEmpty() && (path == base || path.startsWith("$base/"))
    }

    class TrackMatcher(existingTracks: List<TrackEntity>) {
        private val uriMap = HashMap<String, TrackEntity>()
        private val pathMap = HashMap<String, TrackEntity>()
        private val metadataMap = HashMap<String, TrackEntity>()
        /** Track IDs already matched to a file in this scan; a row can only ever represent one file. */
        private val claimed = HashSet<Int>()

        fun claim(track: TrackEntity) {
            claimed.add(track.id)
        }

        init {
            for (track in existingTracks) {
                if (!track.sourceUri.isNullOrEmpty()) {
                    uriMap[track.sourceUri] = track
                }
                if (track.filePath.isNotEmpty()) {
                    pathMap[track.filePath] = track
                }
                val key = "${track.title.trim()}|${track.artist.trim()}|${track.album.trim()}"
                metadataMap[key] = track
            }
        }

        
        fun add(track: TrackEntity) {
            if (!track.sourceUri.isNullOrEmpty()) {
                uriMap[track.sourceUri] = track
            }
            if (track.filePath.isNotEmpty()) {
                pathMap[track.filePath] = track
            }
            val key = "${track.title.trim()}|${track.artist.trim()}|${track.album.trim()}"
            metadataMap[key] = track
        }

        /**
         * Finds the DB row for a scanned file and claims it. URI/path matches are exact. The metadata
         * fallback (used to carry a track across a URI change, e.g. MediaStore -> folder mode) only
         * matches rows not already claimed in this scan; previously two different files with the same
         * tags (e.g. an MP3 and a FLAC copy) collapsed into one row and the second file vanished.
         */
        fun find(uriString: String, filePath: String?, title: String, artist: String, album: String): TrackEntity? {
            val exact = uriMap[uriString] ?: filePath?.takeIf { it.isNotEmpty() }?.let { pathMap[it] }
            if (exact != null && exact.id !in claimed) {
                claimed.add(exact.id)
                return exact
            }
            val key = "${title.trim()}|${artist.trim()}|${album.trim()}"
            val byMetadata = metadataMap[key]
            if (byMetadata != null && byMetadata.id !in claimed) {
                claimed.add(byMetadata.id)
                return byMetadata
            }
            return null
        }
    }

    private fun findMatchingExistingTrack(
        existingTracks: List<TrackEntity>,
        uriString: String,
        filePath: String?,
        title: String,
        artist: String,
        album: String,
        durationMs: Long
    ): TrackEntity? {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        val cleanAlbum = album.trim()

        return existingTracks.firstOrNull { existing ->
            // Match by identical sourceUri
            if (!existing.sourceUri.isNullOrEmpty() && existing.sourceUri == uriString) {
                return@firstOrNull true
            }
            // Match by non-empty file path
            if (!filePath.isNullOrEmpty() && existing.filePath.isNotEmpty() && existing.filePath == filePath) {
                return@firstOrNull true
            }
            // Match by canonical file name if available
            if (!filePath.isNullOrEmpty() && existing.filePath.isNotEmpty()) {
                val name1 = File(filePath).name
                val name2 = File(existing.filePath).name
                if (name1.isNotEmpty() && name1 == name2 && name1.contains(".")) {
                    return@firstOrNull true
                }
            }
            // Match by metadata signature (title + artist + album + approximate duration)
            if (cleanTitle.isNotEmpty() && cleanTitle != "Unknown Title" &&
                existing.title.equals(cleanTitle, ignoreCase = true) &&
                existing.artist.equals(cleanArtist, ignoreCase = true) &&
                existing.album.equals(cleanAlbum, ignoreCase = true) &&
                abs(existing.durationMs - durationMs) <= 3000
            ) {
                return@firstOrNull true
            }
            false
        }
    }

    suspend fun scanMediaStore(
        context: Context,
        includedFolders: Set<String>,
        excludedFolders: Set<String>,
        @Suppress("UNUSED_PARAMETER") existingTracks: List<TrackEntity>
    ): Int = scanMutex.withLock {
        setImporting(true)
        updateProgress(0f)
        var count = 0

        try {
            withContext(Dispatchers.IO) {
                // Re-read inside the lock: a snapshot taken by the caller before waiting would be stale.
                val existingTracks = repository.getAllTracksSync()
                // 1. Clean up dead / deleted tracks from database (with storage-unavailable safeguards)
                val activeExistingTracks = pruneInaccessibleTracks(context, existingTracks)
                removeDuplicateRows(activeExistingTracks)

                val trackMatcher = TrackMatcher(activeExistingTracks)
                val existingBySource = activeExistingTracks.filter { !it.sourceUri.isNullOrEmpty() }.associateBy { it.sourceUri!! }
                val existingByPath = activeExistingTracks.filter { it.filePath.isNotEmpty() }.associateBy { it.filePath }

                if (includedFolders.isNotEmpty()) {
                    val foundFiles = mutableListOf<Triple<Uri, String?, Long>>()
                    val useCache = settingsRepository?.useFileTreeCache?.value ?: false
                    try {
                        DirectoryScanner.scanFolders(
                            context = context,
                            includedUris = includedFolders,
                            excludedUris = excludedFolders,
                            useCache = useCache
                        ) { contentUri, filePath, lastModified ->
                            foundFiles.add(Triple(contentUri, filePath, lastModified))
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        timber.log.Timber.tag("MediaStoreManager").e(e, "Error scanning folders")
                    }

                    // Pre-sort discovered files in natural alphanumeric order
                    val sortedFiles = foundFiles.sortedWith(
                        compareBy(NaturalOrderComparator) { triple ->
                            triple.second ?: triple.first.toString()
                        }
                    )

                    val total = sortedFiles.size
                    val baseAddedTime = System.currentTimeMillis() - (total * 50L)

                    data class PendingImport(
                        val uriString: String,
                        val filePath: String?,
                        val extracted: ExtractedMetadata,
                        val artUri: Uri,
                        val lastModified: Long = 0L
                    )

                    val pendingList = mutableListOf<PendingImport>()

                    for ((index, triple) in sortedFiles.withIndex()) {
                        currentCoroutineContext().ensureActive()
                        val contentUri = triple.first
                        val filePath = triple.second
                        val lastModified = triple.third
                        val uriString = contentUri.toString()
                        
                        // Check if file is already in the database and unmodified
                        val existingTrack = existingBySource[uriString] ?: filePath?.let { existingByPath[it] }

                        val extracted: ExtractedMetadata
                        if (existingTrack != null && existingTrack.lastModified == lastModified && lastModified > 0L && hasUsableMetadata(existingTrack)) {
                            // Re-use cached metadata
                            extracted = ExtractedMetadata(
                                title = existingTrack.title,
                                artist = existingTrack.artist,
                                album = existingTrack.album,
                                albumArtist = existingTrack.albumArtist,
                                genre = existingTrack.genre,
                                durationMs = existingTrack.durationMs,
                                discNumber = existingTrack.discNumber,
                                trackNumber = existingTrack.trackNumber,
                                musicBrainzId = existingTrack.musicBrainzId,
                                lyrics = existingTrack.lyrics,
                                year = existingTrack.year,
                                customTags = existingTrack.customTags
                            )
                        } else {
                            extracted = MetadataExtractor.extract(context, uriString, filePath)
                        }
                        
                        val artUri = Uri.parse(uriString.replaceFirst("content://", "audiocover://"))
                        pendingList.add(PendingImport(uriString, filePath, extracted, artUri, lastModified))

                        if (index % 10 == 0 && total > 0) {
                            updateProgress((index.toFloat() / total.toFloat()) * 0.5f)
                        }
                    }

                    // Sort pending imports by disc number, track number, and natural title/filename
                    val orderedPendingList = pendingList.sortedWith(
                        compareBy<PendingImport> { it.extracted.discNumber ?: 1 }
                            .thenBy {
                                it.extracted.trackNumber
                                    ?: NaturalOrderComparator.extractLeadingNumber(it.extracted.title)
                                    ?: (it.filePath?.let { p -> NaturalOrderComparator.extractLeadingNumber(File(p).nameWithoutExtension) })
                                    ?: 9999
                            }
                            .thenBy(NaturalOrderComparator) {
                                it.filePath?.let { p -> File(p).name } ?: (it.extracted.title ?: "")
                            }
                    )

                    for ((index, item) in orderedPendingList.withIndex()) {
                        val title = item.extracted.title ?: "Unknown Title"
                        val artist = item.extracted.artist ?: "Unknown Artist"
                        val album = item.extracted.album ?: "Unknown Album"
                        val duration = item.extracted.durationMs ?: 0L

                        val matched = trackMatcher.find(item.uriString, item.filePath, title, artist, album)

                        if (matched != null) {
                            val updated = matched.copy(
                                // Re-tagged files must show their new tags (these were never updated before).
                                title = item.extracted.title ?: matched.title,
                                artist = item.extracted.artist ?: matched.artist,
                                album = item.extracted.album ?: matched.album,
                                artPath = item.artUri.toString(),
                                sourceUri = item.uriString,
                                filePath = item.filePath ?: matched.filePath,
                                durationMs = if (duration > 0) duration else matched.durationMs,
                                trackNumber = item.extracted.trackNumber ?: matched.trackNumber,
                                discNumber = item.extracted.discNumber ?: matched.discNumber,
                                albumArtist = item.extracted.albumArtist ?: matched.albumArtist,
                                lyrics = item.extracted.lyrics ?: matched.lyrics,
                                lastModified = item.lastModified,
                                genre = if (!item.extracted.genre.isNullOrBlank() && item.extracted.genre != "Unknown") item.extracted.genre else matched.genre,
                                year = item.extracted.year ?: matched.year,
                                customTags = item.extracted.customTags ?: matched.customTags
                            )
                            repository.updateTrack(updated)
                        } else {
                            val newTrack = TrackEntity(
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                filePath = item.filePath ?: item.uriString,
                                artPath = item.artUri.toString(),
                                addedAt = baseAddedTime + (index * 50L),
                                genre = item.extracted.genre ?: "Unknown",
                                isMetadataPending = false,
                                sourceUri = item.uriString,
                                albumArtist = item.extracted.albumArtist,
                                discNumber = item.extracted.discNumber,
                                trackNumber = item.extracted.trackNumber,
                                musicBrainzId = item.extracted.musicBrainzId,
                                lyrics = item.extracted.lyrics,
                                lastModified = item.lastModified,
                                year = item.extracted.year,
                                customTags = item.extracted.customTags
                            )
                            // Keep the generated row ID: the matcher previously held id=0 copies, so a later
                            // match "updated" a non-existent row 0 and that file was silently never imported.
                            val saved = newTrack.copy(id = repository.insertTrack(newTrack).toInt())
                            trackMatcher.add(saved)
                            trackMatcher.claim(saved)
                            count++
                        }

                        if (index % 5 == 0 && total > 0) {
                            updateProgress(0.5f + ((index.toFloat() / total.toFloat()) * 0.5f))
                        }
                    }

                    if (total > 0) {
                        updateProgress(1f)
                    }
                } else {
                    // System MediaStore scan
                    val resolver = context.contentResolver
                    val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    val projectionList = mutableListOf(
                        android.provider.MediaStore.Audio.Media._ID,
                        android.provider.MediaStore.Audio.Media.TITLE,
                        android.provider.MediaStore.Audio.Media.ARTIST,
                        android.provider.MediaStore.Audio.Media.ALBUM,
                        android.provider.MediaStore.Audio.Media.ALBUM_ID,
                        android.provider.MediaStore.Audio.Media.DURATION,
                        android.provider.MediaStore.Audio.Media.DATA,
                        android.provider.MediaStore.Audio.Media.TRACK,
                        android.provider.MediaStore.Audio.Media.YEAR
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        projectionList.add(android.provider.MediaStore.Audio.Media.ALBUM_ARTIST)
                        projectionList.add(android.provider.MediaStore.Audio.Media.DISC_NUMBER)
                        projectionList.add(android.provider.MediaStore.Audio.Media.GENRE)
                    }
                    val projection = projectionList.toTypedArray()
                    val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"

                    val excludedPaths = excludedFolders.mapNotNull { DirectoryScanner.getPathFromTreeUri(Uri.parse(it)) }.toSet()

                    try {
                        val cursor = resolver.query(uri, projection, selection, null, null)
                        cursor?.use { c ->
                            val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                            val titleCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                            val artistCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                            val albumCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
                            val durationCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                            val dataCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                            val trackCol = c.getColumnIndex(android.provider.MediaStore.Audio.Media.TRACK)
                            val yearCol = c.getColumnIndex(android.provider.MediaStore.Audio.Media.YEAR)
                            val discCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                c.getColumnIndex(android.provider.MediaStore.Audio.Media.DISC_NUMBER)
                            } else -1
                            val albumArtistCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                c.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM_ARTIST)
                            } else -1
                            val genreCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                c.getColumnIndex(android.provider.MediaStore.Audio.Media.GENRE)
                            } else -1

                            data class MediaStoreRow(
                                val id: Long,
                                val title: String?,
                                val artist: String?,
                                val album: String?,
                                val duration: Long,
                                val data: String?,
                                val trackNum: Int?,
                                val discNum: Int?,
                                val albumArtist: String?,
                                val year: Int?,
                                val genre: String?
                            )

                            val rows = mutableListOf<MediaStoreRow>()
                            while (c.moveToNext()) {
                                val id = c.getLong(idCol)
                                val mediaStoreTitle = c.getString(titleCol)
                                val mediaStoreArtist = c.getString(artistCol)
                                val mediaStoreAlbum = c.getString(albumCol)
                                val mediaStoreDuration = c.getLong(durationCol)
                                val data = c.getString(dataCol)
                                val trackNum = if (trackCol != -1) c.getInt(trackCol) else null
                                val discNum = if (discCol != -1) c.getInt(discCol) else null
                                val mediaStoreAlbumArtist = if (albumArtistCol != -1) c.getString(albumArtistCol) else null
                                val mediaStoreYear = if (yearCol != -1 && !c.isNull(yearCol)) {
                                    c.getInt(yearCol).takeIf { it in 1900..2099 }
                                } else null
                                val mediaStoreGenre = if (genreCol != -1 && !c.isNull(genreCol)) c.getString(genreCol)?.trim() else null

                                val dataPath = data ?: ""
                                // Match on path segments: excluding ".../Music/Rap" must not also exclude ".../Music/Rapture".
                                val isExcluded = excludedPaths.any { isPathUnder(dataPath, it) } || excludedFolders.any { isPathUnder(dataPath, it) }

                                if (!isExcluded) {
                                    rows.add(
                                        MediaStoreRow(
                                            id = id,
                                            title = mediaStoreTitle,
                                            artist = mediaStoreArtist,
                                            album = mediaStoreAlbum,
                                            duration = mediaStoreDuration,
                                            data = data,
                                            trackNum = if (trackNum != null && trackNum > 0) trackNum % 1000 else null,
                                            discNum = discNum,
                                            albumArtist = mediaStoreAlbumArtist,
                                            year = mediaStoreYear,
                                            genre = mediaStoreGenre
                                        )
                                    )
                                }
                            }

                            // Sort rows in natural alphanumeric order by file path / title
                            val sortedRows = rows.sortedWith(
                                compareBy<MediaStoreRow> { it.discNum ?: 1 }
                                    .thenBy {
                                        it.trackNum
                                            ?: NaturalOrderComparator.extractLeadingNumber(it.title)
                                            ?: (it.data?.let { p -> NaturalOrderComparator.extractLeadingNumber(File(p).nameWithoutExtension) })
                                            ?: 9999
                                    }
                                    .thenBy(NaturalOrderComparator) {
                                        it.data?.let { p -> File(p).name } ?: (it.title ?: "")
                                    }
                            )

                            val total = sortedRows.size
                            val baseAddedTime = System.currentTimeMillis() - (total * 50L)

                            for ((i, row) in sortedRows.withIndex()) {
                                val contentUri = android.content.ContentUris.withAppendedId(
                                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, row.id
                                )
                                val artUri = Uri.parse(contentUri.toString().replaceFirst("content://", "audiocover://"))
                                val uriString = contentUri.toString()

                                val title = row.title ?: "Unknown Title"
                                val artist = row.artist ?: "Unknown Artist"
                                val album = row.album ?: "Unknown Album"
                                val duration = row.duration
                                val albumArtist = row.albumArtist
                                val trackNumber = row.trackNum
                                val discNumber = row.discNum
                                val genre = row.genre?.takeIf { it.isNotBlank() } ?: "Unknown"
                                val year = row.year ?: Regex("""\b(19\d{2}|20\d{2})\b""").find(row.data ?: "")?.value?.toIntOrNull()
                                val musicBrainzId = null
                                val lyrics = null
                                
                                
                                
                                
                                
                                
                                

                                val matched = trackMatcher.find(uriString, row.data, title, artist, album)

                                if (matched != null) {
                                    val updated = matched.copy(
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        artPath = artUri.toString(),
                                        sourceUri = uriString,
                                        filePath = row.data ?: matched.filePath,
                                        durationMs = if (duration > 0) duration else matched.durationMs,
                                        trackNumber = trackNumber ?: matched.trackNumber,
                                        discNumber = discNumber ?: matched.discNumber,
                                        albumArtist = albumArtist ?: matched.albumArtist,
                                        lyrics = lyrics ?: matched.lyrics,
                                        genre = if (genre != "Unknown") genre else matched.genre,
                                        year = year ?: matched.year
                                    )
                                    repository.updateTrack(updated)
                                } else {
                                    val newTrack = TrackEntity(
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        durationMs = duration,
                                        filePath = row.data ?: uriString,
                                        artPath = artUri.toString(),
                                        addedAt = baseAddedTime + (i * 50L),
                                        genre = genre,
                                        isMetadataPending = false,
                                        lastModified = 0L,
                                        sourceUri = uriString,
                                        albumArtist = albumArtist,
                                        discNumber = discNumber,
                                        trackNumber = trackNumber,
                                        musicBrainzId = musicBrainzId,
                                        lyrics = lyrics,
                                        year = year,
                                        customTags = null
                                    )
                                    val saved = newTrack.copy(id = repository.insertTrack(newTrack).toInt())
                                    trackMatcher.add(saved)
                                    trackMatcher.claim(saved)
                                    count++
                                }

                                if (i % 10 == 0 && total > 0) {
                                    updateProgress(i.toFloat() / total.toFloat())
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        timber.log.Timber.tag("MediaStoreManager").e(e, "Error processing audio files during sync")
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            timber.log.Timber.tag("MediaStoreManager").e(e, "Error syncing audio from MediaStore")
        } finally {
            setImporting(false)
        }
        count
    }

    suspend fun importAudioFiles(
        context: Context,
        uris: List<Uri>,
        @Suppress("UNUSED_PARAMETER") existingTracks: List<TrackEntity>
    ): Int = scanMutex.withLock {
        setImporting(true)
        updateProgress(0f)
        var count = 0

        try {
            withContext(Dispatchers.IO) {
                val existingTracks = repository.getAllTracksSync()
                // 1. Clean up dead tracks (with storage-unavailable safeguards)
                val activeExistingTracks = pruneInaccessibleTracks(context, existingTracks)
                removeDuplicateRows(activeExistingTracks)

                val trackMatcher = TrackMatcher(activeExistingTracks)

                // 2. Query display name for each URI to allow natural sorting
                fun getDetails(uri: Uri): Pair<String, Long> {
                    var name = uri.lastPathSegment ?: uri.toString()
                    var lastMod = 0L
                    try {
                        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameCol != -1) {
                                    val n = cursor.getString(nameCol)
                                    if (!n.isNullOrBlank()) name = n
                                }
                                val modCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                                if (modCol != -1) {
                                    lastMod = cursor.getLong(modCol)
                                }
                            }
                        }
                    } catch (e: Exception) {}
                    return Pair(name, lastMod)
                }

                data class UriWithDetails(val uri: Uri, val displayName: String, val lastModified: Long)
                val namedUris = uris.map { 
                    val details = getDetails(it)
                    UriWithDetails(it, details.first, details.second) 
                }

                val sortedUris = namedUris.sortedWith(compareBy(NaturalOrderComparator) { it.displayName })

                val total = sortedUris.size
                val baseAddedTime = System.currentTimeMillis() - (total * 50L)

                data class PendingUriImport(
                    val uriString: String,
                    val displayName: String,
                    val extracted: ExtractedMetadata,
                    val artUri: Uri,
                    val lastModified: Long
                )

                val pendingList = mutableListOf<PendingUriImport>()
                for ((index, item) in sortedUris.withIndex()) {
                    val uriString = item.uri.toString()
                    val lastModified = item.lastModified
                    
                    val existingTrack = activeExistingTracks.find { 
                        it.sourceUri == uriString || it.filePath == uriString
                    }
                    
                    val extracted: ExtractedMetadata
                    if (existingTrack != null && existingTrack.lastModified == lastModified && lastModified > 0L && hasUsableMetadata(existingTrack)) {
                        extracted = ExtractedMetadata(
                            title = existingTrack.title,
                            artist = existingTrack.artist,
                            album = existingTrack.album,
                            albumArtist = existingTrack.albumArtist,
                            genre = existingTrack.genre,
                            durationMs = existingTrack.durationMs,
                            discNumber = existingTrack.discNumber,
                            trackNumber = existingTrack.trackNumber,
                            musicBrainzId = existingTrack.musicBrainzId,
                            lyrics = existingTrack.lyrics
                        )
                    } else {
                        extracted = MetadataExtractor.extract(context, uriString, "")
                    }
                    
                    val artUri = Uri.parse(uriString.replaceFirst("content://", "audiocover://"))
                    pendingList.add(PendingUriImport(uriString, item.displayName, extracted, artUri, lastModified))

                    if (index % 5 == 0 && total > 0) {
                        updateProgress((index.toFloat() / total.toFloat()) * 0.5f)
                    }
                }

                // Sort by disc, track number, and natural name
                val orderedList = pendingList.sortedWith(
                    compareBy<PendingUriImport> { it.extracted.discNumber ?: 1 }
                        .thenBy {
                            it.extracted.trackNumber
                                ?: NaturalOrderComparator.extractLeadingNumber(it.extracted.title)
                                ?: NaturalOrderComparator.extractLeadingNumber(it.displayName)
                                ?: 9999
                        }
                        .thenBy(NaturalOrderComparator) { it.displayName }
                )

                for ((index, item) in orderedList.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    val title = item.extracted.title ?: item.displayName.substringBeforeLast('.')
                    val artist = item.extracted.artist ?: "Unknown Artist"
                    val album = item.extracted.album ?: "Unknown Album"
                    val duration = item.extracted.durationMs ?: 0L

                    val matched = trackMatcher.find(item.uriString, null, title, artist, album)

                    if (matched != null) {
                        val updated = matched.copy(
                            sourceUri = item.uriString,
                            durationMs = if (duration > 0) duration else matched.durationMs,
                            trackNumber = item.extracted.trackNumber ?: matched.trackNumber,
                            discNumber = item.extracted.discNumber ?: matched.discNumber,
                            albumArtist = item.extracted.albumArtist ?: matched.albumArtist,
                            lyrics = item.extracted.lyrics ?: matched.lyrics
                        )
                        repository.updateTrack(updated)
                    } else {
                        val track = TrackEntity(
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            sourceUri = item.uriString,
                            albumArtist = item.extracted.albumArtist,
                            trackNumber = item.extracted.trackNumber,
                            discNumber = item.extracted.discNumber,
                            genre = item.extracted.genre ?: "Unknown",
                            filePath = "",
                            artPath = item.artUri.toString(),
                            addedAt = baseAddedTime + (index * 50L),
                            musicBrainzId = item.extracted.musicBrainzId,
                            lyrics = item.extracted.lyrics
                        )
                        val saved = track.copy(id = repository.insertTrack(track).toInt())
                        trackMatcher.add(saved)
                        trackMatcher.claim(saved)
                        count++
                    }

                    if (index % 5 == 0 && total > 0) {
                        updateProgress(0.5f + ((index.toFloat() / total.toFloat()) * 0.5f))
                    }
                }

                if (uris.isNotEmpty()) {
                    updateProgress(1f)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            timber.log.Timber.tag("MediaStoreManager").e(e, "Error importing audio files")
        } finally {
            setImporting(false)
        }
        count
    }
}
