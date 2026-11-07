package com.example.data

import android.content.Context
import android.provider.MediaStore
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class ArtistEntity(
    val name: String,
    val tracks: List<TrackEntity>
)

class SongRepository(private val context: Context) {

    /**
     * Fetches tracks from MediaStore on Dispatchers.IO and returns a Flow<List<TrackEntity>>.
     * Ensures zero-lag UI thread operation by fully offloading querying to the IO dispatcher.
     */
    fun fetchSongsFromMediaStore(): Flow<List<TrackEntity>> = flow {
        val trackList = mutableListOf<TrackEntity>()
        val resolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        
        // Add Album Artist column if running on Android 11 (API 30) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projection.add(MediaStore.Audio.Media.ALBUM_ARTIST)
        }
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        try {
            val cursor = resolver.query(uri, projection.toTypedArray(), selection, null, null)
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                
                val albumArtistCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                } else {
                    -1
                }
                
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Title"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val data = c.getString(dataCol)
                    
                    val albumArtist = if (albumArtistCol != -1) {
                        c.getString(albumArtistCol)
                    } else null
                    
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val artUri = Uri.parse(contentUri.toString().replaceFirst("content://", "audiocover://"))
                    
                    val track = TrackEntity(
                        id = id.toInt(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        filePath = data ?: contentUri.toString(),
                        artPath = artUri.toString(),
                        genre = "Unknown",
                        isMetadataPending = false,
                        lastModified = 0L,
                        sourceUri = contentUri.toString(),
                        albumArtist = albumArtist
                    )
                    trackList.add(track)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            timber.log.Timber.tag("SongRepository").e(e, "Error loading audio from MediaStore flow")
        }
        emit(trackList)
    }.flowOn(Dispatchers.IO)

    /**
     * Performs Case-Insensitive Clustering to group artists while keeping the library clean.
     * Uses Album Artist tag as the primary sorting key for the Artist list.
     */
    fun clusterAndSortArtists(tracks: List<TrackEntity>): List<ArtistEntity> {
        // Group tracks by the case-insensitive effective artist name (using album artist primarily)
        val groupedByArtist = tracks.groupBy { track ->
            val effective = if (!track.albumArtist.isNullOrBlank()) track.albumArtist else track.artist
            effective.trim().lowercase()
        }

        return groupedByArtist.map { (lowercasedName, trackList) ->
            // Choose a canonical name with proper casing from the tracks
            val canonicalName = trackList.map { 
                if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist 
            }.firstOrNull { name -> name.any { it.isUpperCase() } }
                ?: trackList.firstOrNull()?.let { 
                    if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist 
                } ?: "Unknown Artist"

            ArtistEntity(
                name = canonicalName,
                tracks = trackList
            )
        }.sortedBy { it.name.lowercase() }
    }
}
