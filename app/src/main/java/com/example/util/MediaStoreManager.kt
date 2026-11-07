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
                val pfd = context.contentResolver.openAssetFileDescriptor(uri, "r")
                if (pfd != null) {
                    val len = pfd.length
                    pfd.close()
                    if (len > 0 || len == android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH) {
                        return true
                    }
                }
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

    class TrackMatcher(existingTracks: List<TrackEntity>) {
        private val uriMap = HashMap<String, TrackEntity>()
        private val pathMap = HashMap<String, TrackEntity>()
        private val metadataMap = HashMap<String, TrackEntity>()

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

        fun find(uriString: String, filePath: String?, title: String, artist: String, album: String): TrackEntity? {
            uriMap[uriString]?.let { return it }
            if (!filePath.isNullOrEmpty()) {
                pathMap[filePath]?.let { return it }
            }
            val key = "${title.trim()}|${artist.trim()}|${album.trim()}"
            metadataMap[key]?.let { return it }
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
        existingTracks: List<TrackEntity>
    ): Int {
        setImporting(true)
        updateProgress(0f)
        var count = 0

        try {
            withContext(Dispatchers.IO) {
                // 1. Clean up dead / deleted tracks from database
                val activeExistingTracks = mutableListOf<TrackEntity>()
                for (track in existingTracks) {
                    if (!isTrackAccessible(context, track)) {
                        repository.deleteTrack(track.id)
                    } else {
                        activeExistingTracks.add(track)
                    }
                }

                // Deduplicate tracks
                val uniqueTracks = mutableMapOf<String, TrackEntity>()
                val duplicatesToDelete = mutableListOf<TrackEntity>()
                for (track in activeExistingTracks) {
                    val key = if (!track.sourceUri.isNullOrEmpty()) track.sourceUri else track.filePath
                    if (key != null && key.isNotEmpty() && uniqueTracks.containsKey(key)) {
                        duplicatesToDelete.add(track)
                    } else if (key != null && key.isNotEmpty()) {
                        uniqueTracks[key] = track
                    }
                }
                for (dup in duplicatesToDelete) {
                    repository.deleteTrack(dup.id)
                    activeExistingTracks.remove(dup)
                }

                val currentTrackList = activeExistingTracks.toMutableList()
                val trackMatcher = TrackMatcher(currentTrackList)

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
                        val existingTrack = activeExistingTracks.find { 
                            (it.sourceUri == uriString || (filePath != null && it.filePath == filePath)) 
                        }
                        
                        val extracted: ExtractedMetadata
                        if (existingTrack != null && existingTrack.lastModified == lastModified && lastModified > 0L) {
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
                            repository.insertTrack(newTrack)
                            currentTrackList.add(newTrack)
                                    trackMatcher.add(newTrack)
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
                                val isExcluded = excludedPaths.any { dataPath.startsWith(it) } || excludedFolders.any { dataPath.startsWith(it) }

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
                                    repository.insertTrack(newTrack)
                                    currentTrackList.add(newTrack)
                                    trackMatcher.add(newTrack)
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
        return count
    }

    suspend fun importAudioFiles(
        context: Context,
        uris: List<Uri>,
        existingTracks: List<TrackEntity>
    ): Int {
        setImporting(true)
        updateProgress(0f)
        var count = 0

        try {
            withContext(Dispatchers.IO) {
                // 1. Clean up dead tracks
                val activeExistingTracks = mutableListOf<TrackEntity>()
                for (track in existingTracks) {
                    if (!isTrackAccessible(context, track)) {
                        repository.deleteTrack(track.id)
                    } else {
                        activeExistingTracks.add(track)
                    }
                }

                // Deduplicate tracks
                val uniqueTracks = mutableMapOf<String, TrackEntity>()
                val duplicatesToDelete = mutableListOf<TrackEntity>()
                for (track in activeExistingTracks) {
                    val key = if (!track.sourceUri.isNullOrEmpty()) track.sourceUri else track.filePath
                    if (key != null && key.isNotEmpty() && uniqueTracks.containsKey(key)) {
                        duplicatesToDelete.add(track)
                    } else if (key != null && key.isNotEmpty()) {
                        uniqueTracks[key] = track
                    }
                }
                for (dup in duplicatesToDelete) {
                    repository.deleteTrack(dup.id)
                    activeExistingTracks.remove(dup)
                }

                val currentTrackList = activeExistingTracks.toMutableList()
                val trackMatcher = TrackMatcher(currentTrackList)

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
                    if (existingTrack != null && existingTrack.lastModified == lastModified && lastModified > 0L) {
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
                        repository.insertTrack(track)
                        currentTrackList.add(track)
                                    trackMatcher.add(track)
                        currentTrackList.add(track)
                                    trackMatcher.add(track)
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
        return count
    }
}
