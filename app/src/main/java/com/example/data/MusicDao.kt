package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @androidx.room.Query("SELECT * FROM smart_volume_zones")
    fun getAllSmartVolumeZones(): kotlinx.coroutines.flow.Flow<List<SmartVolumeZoneEntity>>
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertSmartVolumeZone(zone: SmartVolumeZoneEntity): Long
    @androidx.room.Update
    suspend fun updateSmartVolumeZone(zone: SmartVolumeZoneEntity)
    @androidx.room.Delete
    suspend fun deleteSmartVolumeZone(zone: SmartVolumeZoneEntity)
    @androidx.room.Query("SELECT * FROM smart_volume_zones WHERE id = :id")
    suspend fun getSmartVolumeZone(id: Int): SmartVolumeZoneEntity?
    // Track Queries
    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksSync(): List<TrackEntity>

    @Query("""
        SELECT tracks.* FROM tracks 
        JOIN tracks_fts ON tracks.id = tracks_fts.rowid 
        WHERE tracks_fts MATCH :query
    """)
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: Int): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Update
    suspend fun updateTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: Int)

    // Playlist Queries
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    // Many-to-Many cross reference queries
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrackCrossRef(ref: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistTrackCrossRef(playlistId: Int, trackId: Int)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Int)

    @Query("""
        SELECT t.* FROM tracks t 
        INNER JOIN playlist_track_cross_ref ref ON t.id = ref.trackId 
        WHERE ref.playlistId = :playlistId 
        ORDER BY t.addedAt DESC
    """)
    fun getTracksForPlaylist(playlistId: Int): Flow<List<TrackEntity>>

    @Query("SELECT * FROM playlist_track_cross_ref")
    fun getAllPlaylistTrackCrossRefs(): Flow<List<PlaylistTrackCrossRef>>

    @Query("SELECT * FROM playback_state WHERE id = 0")
    suspend fun getPlaybackState(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackState(state: PlaybackStateEntity)

    @Query("UPDATE playback_state SET currentPositionMs = :pos, currentTrackId = :trackId, isShuffleEnabled = :isShuffle WHERE id = 0")
    suspend fun updatePlaybackPosition(pos: Int, trackId: Int, isShuffle: Boolean)

    // Playback History & Smart Playlists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackHistoryInternal(history: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE id NOT IN (SELECT id FROM playback_history ORDER BY timestamp DESC LIMIT 500)")
    suspend fun trimPlaybackHistory()

    @Transaction
    suspend fun insertPlaybackHistory(history: PlaybackHistoryEntity) {
        insertPlaybackHistoryInternal(history)
        trimPlaybackHistory()
    }
    
    @Query("SELECT COUNT(*) FROM playback_history WHERE trackId = :trackId")
    suspend fun getPlayCountForTrack(trackId: Int): Int
    
    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getTracksByIds(ids: List<Int>): List<TrackEntity>

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC LIMIT 30")
    fun getRecentlyAdded(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN (
            SELECT trackId, COUNT(*) as playCount
            FROM playback_history
            GROUP BY trackId
        ) h ON t.id = h.trackId
        ORDER BY h.playCount DESC
        LIMIT 30
    """)
    fun getMostPlayed(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN (
            SELECT trackId, MAX(timestamp) as lastPlayed
            FROM playback_history
            GROUP BY trackId
        ) h ON t.id = h.trackId
        ORDER BY h.lastPlayed DESC
        LIMIT 30
    """)
    fun getRecentlyPlayed(): Flow<List<TrackEntity>>
}
