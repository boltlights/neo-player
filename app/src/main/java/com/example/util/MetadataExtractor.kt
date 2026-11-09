package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import timber.log.Timber
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

data class ExtractedMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val durationMs: Long?,
    val discNumber: Int?,
    val trackNumber: Int?,
    val musicBrainzId: String?,
    val lyrics: String?,
    val year: Int? = null,
    val customTags: String? = null
)

object MetadataExtractor {
    init {
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    private fun getOriginalFileName(context: Context, uriString: String, fallbackPath: String?): String {
        if (!fallbackPath.isNullOrEmpty()) {
            return File(fallbackPath).name
        }
        if (uriString.startsWith("content://")) {
            try {
                context.contentResolver.query(
                    Uri.parse(uriString),
                    arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        if (name != null) return name
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.tag("MetadataExtractor").w(e, "Failed to get DISPLAY_NAME for %s", uriString)
            }
        }
        return File(uriString).name
    }

    private fun inferTitleArtist(title: String?, artist: String?, fileName: String): Pair<String?, String?> {
        var newTitle = title
        var newArtist = artist
        if (newTitle.isNullOrEmpty() || newArtist.isNullOrEmpty()) {
            if (newTitle.isNullOrEmpty()) {
                if (fileName.contains(" - ")) {
                    newTitle = fileName.substringAfter(" - ").trim()
                    if (newArtist.isNullOrEmpty()) {
                        newArtist = fileName.substringBefore(" - ").trim()
                    }
                } else {
                    newTitle = fileName
                }
            }
        }
        return Pair(newTitle, newArtist)
    }

    private fun inferTrackNumber(trackNumber: Int?, fileName: String, title: String?): Int? {
        // Track 0 is sometimes used for untracked/hidden tracks, but if it's null we infer from filename
        if (trackNumber == null) {
            val fromFile = NaturalOrderComparator.extractLeadingNumber(fileName)
            if (fromFile != null) return fromFile
            return title?.let { NaturalOrderComparator.extractLeadingNumber(it) }
        }
        return trackNumber
    }

    suspend fun extract(context: Context, uriString: String, fallbackPath: String?): ExtractedMetadata = withContext(Dispatchers.IO) {
        val originalName = getOriginalFileName(context, uriString, fallbackPath)
        val fileName = originalName.substringBeforeLast('.')
        val extension = originalName.substringAfterLast('.', "").takeIf { it.isNotEmpty() } ?: "mp3"

        val targetPath = fallbackPath?.takeIf { it.isNotEmpty() } ?: uriString.removePrefix("file://")
        var file = File(targetPath)
        var tempFile: File? = null
        
        if (!file.exists() || !file.canRead()) {
            try {
                if (uriString.startsWith("content://")) {
                    val uri = Uri.parse(uriString)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempFile = File.createTempFile("metadata_temp", ".$extension", context.cacheDir)
                        tempFile!!.outputStream().use { out ->
                            inputStream.copyTo(out)
                        }
                        file = tempFile!!
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.tag("MetadataExtractor").w(e, "Failed to create temp file for %s", uriString)
            }
        }
        
        if (file.exists() && file.isFile) {
            try {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tag
                val header = audioFile.audioHeader
                
                val rawTitle = tag?.getFirst(FieldKey.TITLE)?.trim()
                val rawArtist = tag?.getFirst(FieldKey.ARTIST)?.trim()
                val album = tag?.getFirst(FieldKey.ALBUM)?.trim()
                val albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)?.trim()
                val genre = tag?.getFirst(FieldKey.GENRE)?.trim()
                val durationMs = header?.trackLength?.toLong()?.times(1000L)
                
                val discNumberStr = tag?.getFirst(FieldKey.DISC_NO)
                val discNumber = discNumberStr?.split("/")?.get(0)?.toIntOrNull()
                
                val trackNumberStr = tag?.getFirst(FieldKey.TRACK)
                var trackNumber = trackNumberStr?.split("/")?.get(0)?.toIntOrNull()
                
                val musicBrainzId = tag?.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID)?.trim()
                val lyrics = tag?.getFirst(FieldKey.LYRICS)?.trim()
                
                val (title, artist) = inferTitleArtist(rawTitle, rawArtist, fileName)
                trackNumber = inferTrackNumber(trackNumber, fileName, title)

                try {
                    val artwork = tag?.firstArtwork?.binaryData
                    if (artwork != null) {
                        if (!AudioThumbnailCache.hasThumbnail(context, uriString)) {
                            AudioThumbnailCache.saveThumbnail(context, uriString, artwork)
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
                
                val rawYearStr = tag?.getFirst(FieldKey.YEAR)?.trim()
                val year = rawYearStr?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
                    ?: Regex("""\b(19\d{2}|20\d{2})\b""").find(fileName)?.value?.toIntOrNull()

                val comment = tag?.getFirst(FieldKey.COMMENT)?.trim()
                val tagsField = tag?.getFirst(FieldKey.TAGS)?.trim()
                val custom1 = tag?.getFirst(FieldKey.CUSTOM1)?.trim()
                val custom2 = tag?.getFirst(FieldKey.CUSTOM2)?.trim()
                val grouping = tag?.getFirst(FieldKey.GROUPING)?.trim()
                val mood = tag?.getFirst(FieldKey.MOOD)?.trim()
                val composer = tag?.getFirst(FieldKey.COMPOSER)?.trim()

                val customTagsList = listOfNotNull(
                    comment?.takeIf { it.isNotBlank() },
                    tagsField?.takeIf { it.isNotBlank() },
                    custom1?.takeIf { it.isNotBlank() },
                    custom2?.takeIf { it.isNotBlank() },
                    grouping?.takeIf { it.isNotBlank() },
                    mood?.takeIf { it.isNotBlank() },
                    composer?.takeIf { it.isNotBlank() }
                ).distinct()
                val customTags = if (customTagsList.isNotEmpty()) customTagsList.joinToString(" • ") else null

                return@withContext ExtractedMetadata(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    genre = genre,
                    durationMs = durationMs,
                    discNumber = discNumber,
                    trackNumber = trackNumber,
                    musicBrainzId = musicBrainzId,
                    lyrics = lyrics,
                    year = year,
                    customTags = customTags
                )
            } catch (e: Throwable) {
                // Throwable, not Exception: a broken or unusual file can make jaudiotagger throw an
                // Error (e.g. ExceptionInInitializerError), which must not take the whole app down.
                if (e is CancellationException || e is OutOfMemoryError) throw e
                Timber.tag("MetadataExtractor").w(e, "jaudiotagger failed to read tags for %s, falling back to MediaMetadataRetriever", uriString)
                // Fallback to MediaMetadataRetriever
            } finally {
                tempFile?.delete()
            }
        }
        
        return@withContext extractWithRetriever(context, uriString, fileName)
    }
    
    private fun extractWithRetriever(context: Context, uriString: String, fileName: String): ExtractedMetadata {
        val retriever = MediaMetadataRetriever()
        try {
            if (uriString.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(uriString))
            } else {
                retriever.setDataSource(uriString)
            }
            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.trim()
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim()
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull()
            
            val discNumberStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            val discNumber = discNumberStr?.split("/")?.get(0)?.toIntOrNull()
            
            val trackNumberStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            var trackNumber = trackNumberStr?.split("/")?.get(0)?.toIntOrNull()
            
            val musicBrainzId: String? = null
            val lyrics: String? = null
            
            val (title, artist) = inferTitleArtist(rawTitle, rawArtist, fileName)
            trackNumber = inferTrackNumber(trackNumber, fileName, title)

            try {
                val picture = retriever.embeddedPicture
                if (picture != null) {
                    if (!AudioThumbnailCache.hasThumbnail(context, uriString)) {
                        AudioThumbnailCache.saveThumbnail(context, uriString, picture)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            
            val rawYearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.trim()
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.trim()
            val year = rawYearStr?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
                ?: Regex("""\b(19\d{2}|20\d{2})\b""").find(fileName)?.value?.toIntOrNull()
            val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)?.trim()
            val customTags = composer?.takeIf { it.isNotBlank() }

            return ExtractedMetadata(
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                durationMs = durationMs,
                discNumber = discNumber,
                trackNumber = trackNumber,
                musicBrainzId = musicBrainzId,
                lyrics = lyrics,
                year = year,
                customTags = customTags
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.tag("MetadataExtractor").w(e, "MediaMetadataRetriever failed to read tags for %s", uriString)
            // Nothing could be read: still name the song after its file rather than "Unknown Title".
            val (title, artist) = inferTitleArtist(null, null, fileName)
            return ExtractedMetadata(title, artist, null, null, null, null, null, inferTrackNumber(null, fileName, title), null, null)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }
}
