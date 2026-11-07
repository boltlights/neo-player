package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicRepository(private val musicDao: MusicDao) {

    private val _indexingState = MutableStateFlow<IndexingState?>(null)
    val indexingState: StateFlow<IndexingState?> = _indexingState.asStateFlow()

    fun updateIndexingState(state: IndexingState?) {
        _indexingState.value = state
    }

    val allSmartVolumeZones: kotlinx.coroutines.flow.Flow<List<SmartVolumeZoneEntity>> = musicDao.getAllSmartVolumeZones()
    suspend fun insertSmartVolumeZone(zone: SmartVolumeZoneEntity) = musicDao.insertSmartVolumeZone(zone)
    suspend fun updateSmartVolumeZone(zone: SmartVolumeZoneEntity) = musicDao.updateSmartVolumeZone(zone)
    suspend fun deleteSmartVolumeZone(zone: SmartVolumeZoneEntity) = musicDao.deleteSmartVolumeZone(zone)
    suspend fun getSmartVolumeZone(id: Int) = musicDao.getSmartVolumeZone(id)
    val allTracks: Flow<List<TrackEntity>> = musicDao.getAllTracks()

    suspend fun getAllTracksSync(): List<TrackEntity> {
        return musicDao.getAllTracksSync()
    }

    val allPlaylists: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()
    val allPlaylistTrackCrossRefs: Flow<List<PlaylistTrackCrossRef>> = musicDao.getAllPlaylistTrackCrossRefs()
    val recentlyAdded: Flow<List<TrackEntity>> = musicDao.getRecentlyAdded()
    val mostPlayed: Flow<List<TrackEntity>> = musicDao.getMostPlayed()
    val recentlyPlayed: Flow<List<TrackEntity>> = musicDao.getRecentlyPlayed()

    suspend fun insertPlaybackHistory(history: PlaybackHistoryEntity) {
        musicDao.insertPlaybackHistory(history)
    }
    
    suspend fun getPlayCountForTrack(trackId: Int): Int {
        return musicDao.getPlayCountForTrack(trackId)
    }
    
    /**
     * Chunked: SQLite on Android 10 and below allows at most 999 bound variables per statement, so a
     * single IN (...) with a large restored queue (e.g. "shuffle all" on a big library) crashed on launch.
     */
    suspend fun getTracksByIds(ids: List<Int>): List<TrackEntity> {
        if (ids.size <= MAX_SQL_VARIABLES) return musicDao.getTracksByIds(ids)
        return ids.chunked(MAX_SQL_VARIABLES).flatMap { musicDao.getTracksByIds(it) }
    }

    private companion object {
        const val MAX_SQL_VARIABLES = 900
    }

    suspend fun getTrackById(id: Int): TrackEntity? {
        return musicDao.getTrackById(id)
    }
    
    suspend fun findTrack(id: Int): TrackEntity? {
        return getTrackById(id)
    }

    fun searchTracks(query: String): Flow<List<TrackEntity>> {
        val ftsQuery = "$query*"
        return musicDao.searchTracks(ftsQuery)
    }

    private fun Int.coerceFuzzyRange(min: Int, max: Int) = coerceIn(min, max)

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = Array(lhsLength + 1) { it }
        var newCost = Array(lhsLength + 1) { 0 }
        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = kotlin.math.min(kotlin.math.min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    suspend fun fuzzySearchLocalTracks(allTracksList: List<TrackEntity>, query: String, limit: Int = 20): List<TrackEntity> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        allTracksList.map {
            val titleScore = levenshteinDistance(query.lowercase(), it.title.lowercase())
            val artistScore = levenshteinDistance(query.lowercase(), it.artist.lowercase())
            it to kotlin.math.min(titleScore, artistScore)
        }.sortedBy { it.second }.take(limit).map { it.first }
    }

    suspend fun insertTrack(track: TrackEntity): Long {
        return musicDao.insertTrack(track)
    }

    suspend fun updateTrack(track: TrackEntity) {
        musicDao.updateTrack(track)
    }

    suspend fun updateTracks(tracks: List<TrackEntity>) {
        musicDao.updateTracks(tracks)
    }

    suspend fun deleteTrack(trackId: Int) {
        musicDao.deleteTrack(trackId)
    }

    suspend fun insertPlaylist(playlist: PlaylistEntity): Long {
        return musicDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) {
        musicDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlistId: Int) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int) {
        musicDao.insertPlaylistTrackCrossRef(PlaylistTrackCrossRef(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Int) {
        musicDao.deletePlaylistTrackCrossRef(playlistId, trackId)
    }

    suspend fun createPlaylist(name: String, songs: List<TrackEntity>) {
        val playlistId = insertPlaylist(PlaylistEntity(name = name)).toInt()
        songs.forEach { song ->
            addTrackToPlaylist(playlistId, song.id)
        }
    }

    suspend fun renamePlaylist(playlist: PlaylistEntity, name: String) {
        updatePlaylist(playlist.copy(name = name))
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        deletePlaylist(playlist.id)
    }

    suspend fun addToPlaylist(songs: List<TrackEntity>, playlist: PlaylistEntity) {
        songs.forEach { song ->
            addTrackToPlaylist(playlist.id, song.id)
        }
    }

    suspend fun rewritePlaylist(playlist: PlaylistEntity, songs: List<TrackEntity>) {
        musicDao.clearPlaylistTracks(playlist.id)
        songs.forEach { song ->
            addTrackToPlaylist(playlist.id, song.id)
        }
    }

    fun getTracksForPlaylist(playlistId: Int): Flow<List<TrackEntity>> {
        return musicDao.getTracksForPlaylist(playlistId)
    }

    suspend fun getPlaybackState(): PlaybackStateEntity? {
        return musicDao.getPlaybackState()
    }

    suspend fun savePlaybackState(state: PlaybackStateEntity) {
        musicDao.savePlaybackState(state)
    }

    suspend fun updatePlaybackPosition(pos: Int, trackId: Int, isShuffle: Boolean) {
        musicDao.updatePlaybackPosition(pos, trackId, isShuffle)
    }
}
