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
        Index(value = ["genre"]),
        Index(value = ["uid"])
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
    val customTags: String? = null,
    /**
     * Stable identity (see [TrackUid]). Filled in by [MusicRepository] on every insert/update, so it
     * never needs to be set by hand. Persisted references use this instead of [id].
     */
    val uid: String = ""
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val coverArtUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A song in a playlist, stored by stable [TrackEntity.uid] with an explicit [position].
 *
 * Deliberately *no* foreign key to `tracks`: a song whose file is temporarily missing (SD card out,
 * permission revoked) is removed from `tracks` by the scanner, and with the old cascade that silently
 * erased it from every playlist. Entries for absent songs simply stay hidden until the song returns.
 */
@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "trackUid"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["trackUid"])
    ]
)
data class PlaylistEntryEntity(
    val playlistId: Int,
    val trackUid: String,
    val position: Int
)

/**
 * A playlist membership resolved to the current session's track row ID (what the UI works with).
 * Produced by a join of [PlaylistEntryEntity] with `tracks`; only songs present in the library appear.
 */
data class PlaylistTrackCrossRef(
    val playlistId: Int,
    val trackId: Int
)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 0,
    /** Comma-separated [TrackEntity.uid]s. */
    val baseQueueTrackUids: String = "",
    val queueTrackUids: String,
    val currentTrackUid: String?,
    val currentPositionMs: Int,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false
)

@Entity(
    tableName = "playback_history",
    indices = [Index(value = ["trackUid"])]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trackUid: String,
    val timestamp: Long = System.currentTimeMillis()
)
