with open('app/src/main/java/com/example/data/MusicRepository.kt', 'r') as f:
    content = f.read()

import re

# Add StateFlow imports and properties
content = content.replace(
'''import kotlinx.coroutines.flow.map

class MusicRepository(private val musicDao: MusicDao) {''',
'''import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicRepository(private val musicDao: MusicDao) {

    private val _indexingState = MutableStateFlow<IndexingState?>(null)
    val indexingState: StateFlow<IndexingState?> = _indexingState.asStateFlow()

    fun updateIndexingState(state: IndexingState?) {
        _indexingState.value = state
    }
'''
)

# Replace playlist operations to match Auxio
content = content.replace(
'''    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int) {
        musicDao.insertPlaylistTrackCrossRef(PlaylistTrackCrossRef(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Int) {
        musicDao.deletePlaylistTrackCrossRef(playlistId, trackId)
    }''',
'''    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int) {
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
    }'''
)

# Implement find(uid) where uid is a track id for now (Wait, Neo Player uses Int IDs)
content = content.replace(
'''    suspend fun getTrackById(id: Int): TrackEntity? {
        return musicDao.getTrackById(id)
    }''',
'''    suspend fun getTrackById(id: Int): TrackEntity? {
        return musicDao.getTrackById(id)
    }
    
    suspend fun findTrack(id: Int): TrackEntity? {
        return getTrackById(id)
    }'''
)

with open('app/src/main/java/com/example/data/MusicRepository.kt', 'w') as f:
    f.write(content)
