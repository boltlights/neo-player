package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Fts4

@Fts4(contentEntity = TrackEntity::class)
@Entity(tableName = "tracks_fts")
data class TrackFtsEntity(
    val title: String,
    val artist: String,
    val album: String
)

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["genre"])
    ]
)
@androidx.compose.runtime.Immutable
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val filePath: String,
    val artPath: String?,
    val addedAt: Long = System.currentTimeMillis(),
    val genre: String? = "Unknown",
    val isMetadataPending: Boolean = false,
    val lastModified: Long = 0L,
    val sourceUri: String? = null,
    val albumArtist: String? = null,
    val discNumber: Int? = null,
    val trackNumber: Int? = null,
    val musicBrainzId: String? = null,
    val lyrics: String? = null,
    val year: Int? = null,
    val customTags: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val coverArtUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["trackId"])
    ]
)
data class PlaylistTrackCrossRef(
    val playlistId: Int,
    val trackId: Int
)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 0,
    val baseQueueTrackIds: String = "",
    val queueTrackIds: String,
    val currentTrackId: Int,
    val currentPositionMs: Int,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trackId: Int,
    val timestamp: Long = System.currentTimeMillis()
)
