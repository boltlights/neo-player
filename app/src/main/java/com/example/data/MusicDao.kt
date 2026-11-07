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
    suspend fun insertTrackRaw(track: TrackEntity): Long

    @Update
    suspend fun updateTrackRaw(track: TrackEntity)

    @Update
    suspend fun updateTracksRaw(tracks: List<TrackEntity>)

    /** Inserts a track with its stable [TrackEntity.uid] filled in. */
    suspend fun insertTrack(track: TrackEntity): Long = insertTrackRaw(track.copy(uid = TrackUid.of(track)))

    /**
     * Updates a track, recomputing its UID. If re-tagging changed the UID, the song's playlist entries
     * and play history move with it (unless an identical copy still carries the old UID).
     */
    @Transaction
    suspend fun updateTrack(track: TrackEntity) {
        val newUid = TrackUid.of(track)
        val oldUid = track.uid.ifEmpty { getUidForTrack(track.id).orEmpty() }
        updateTrackRaw(track.copy(uid = newUid))
        if (oldUid.isNotEmpty() && oldUid != newUid && countTracksWithUid(oldUid) == 0) {
            rekeyUid(oldUid, newUid)
        }
    }

    suspend fun updateTracks(tracks: List<TrackEntity>) =
        updateTracksRaw(tracks.map { it.copy(uid = TrackUid.of(it)) })

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

    // Stable-identity helpers. A persisted UID resolves to the lowest row ID carrying it, so identical
    // copies of one song (same tags) resolve deterministically to a single library row.
    @Query("SELECT uid FROM tracks WHERE id = :trackId")
    suspend fun getUidForTrack(trackId: Int): String?

    @Query("SELECT * FROM tracks WHERE uid IN (:uids)")
    suspend fun getTracksByUids(uids: List<String>): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM tracks WHERE uid = :uid")
    suspend fun countTracksWithUid(uid: String): Int

    // Playlist entries (by stable UID, ordered by position)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistEntry(entry: PlaylistEntryEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun getMaxPlaylistPosition(playlistId: Int): Int

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND trackUid = :trackUid")
    suspend fun deletePlaylistEntry(playlistId: Int, trackUid: String)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Int)

    @Query("""
        SELECT t.* FROM playlist_entries e
        INNER JOIN tracks t ON t.id = (SELECT MIN(t2.id) FROM tracks t2 WHERE t2.uid = e.trackUid)
        WHERE e.playlistId = :playlistId
        ORDER BY e.position
    """)
    fun getTracksForPlaylist(playlistId: Int): Flow<List<TrackEntity>>

    @Query("""
        SELECT e.playlistId AS playlistId, t.id AS trackId FROM playlist_entries e
        INNER JOIN tracks t ON t.id = (SELECT MIN(t2.id) FROM tracks t2 WHERE t2.uid = e.trackUid)
        ORDER BY e.playlistId, e.position
    """)
    fun getAllPlaylistTrackCrossRefs(): Flow<List<PlaylistTrackCrossRef>>

    /**
     * Moves playlist entries and history from [oldUid] to [newUid] (a song was re-tagged). Entries that
     * would duplicate an existing one for [newUid] in the same playlist are dropped.
     */
    @Transaction
    suspend fun rekeyUid(oldUid: String, newUid: String) {
        rekeyPlaylistEntries(oldUid, newUid)
        deletePlaylistEntriesForUid(oldUid)
        rekeyHistory(oldUid, newUid)
    }

    @Query("UPDATE OR IGNORE playlist_entries SET trackUid = :newUid WHERE trackUid = :oldUid")
    suspend fun rekeyPlaylistEntries(oldUid: String, newUid: String)

    @Query("DELETE FROM playlist_entries WHERE trackUid = :uid")
    suspend fun deletePlaylistEntriesForUid(uid: String)

    @Query("UPDATE playback_history SET trackUid = :newUid WHERE trackUid = :oldUid")
    suspend fun rekeyHistory(oldUid: String, newUid: String)

    @Query("SELECT * FROM playback_state WHERE id = 0")
    suspend fun getPlaybackState(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackState(state: PlaybackStateEntity)

    @Query("UPDATE playback_state SET currentPositionMs = :pos, currentTrackUid = :trackUid, isShuffleEnabled = :isShuffle WHERE id = 0")
    suspend fun updatePlaybackPosition(pos: Int, trackUid: String?, isShuffle: Boolean)

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
    
    @Query("SELECT COUNT(*) FROM playback_history WHERE trackUid = (SELECT uid FROM tracks WHERE id = :trackId)")
    suspend fun getPlayCountForTrack(trackId: Int): Int
    
    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getTracksByIds(ids: List<Int>): List<TrackEntity>

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC LIMIT 30")
    fun getRecentlyAdded(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM (
            SELECT trackUid, COUNT(*) AS playCount
            FROM playback_history
            GROUP BY trackUid
        ) h
        INNER JOIN tracks t ON t.id = (SELECT MIN(t2.id) FROM tracks t2 WHERE t2.uid = h.trackUid)
        ORDER BY h.playCount DESC
        LIMIT 30
    """)
    fun getMostPlayed(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM (
            SELECT trackUid, MAX(timestamp) AS lastPlayed
            FROM playback_history
            GROUP BY trackUid
        ) h
        INNER JOIN tracks t ON t.id = (SELECT MIN(t2.id) FROM tracks t2 WHERE t2.uid = h.trackUid)
        ORDER BY h.lastPlayed DESC
        LIMIT 30
    """)
    fun getRecentlyPlayed(): Flow<List<TrackEntity>>
}
