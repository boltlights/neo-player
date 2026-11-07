with open('app/src/main/java/com/example/data/MusicDao.kt', 'r') as f:
    content = f.read()

import re
content = content.replace(
'''    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistTrackCrossRef(playlistId: Int, trackId: Int)''',
'''    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistTrackCrossRef(playlistId: Int, trackId: Int)

    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Int)'''
)

with open('app/src/main/java/com/example/data/MusicDao.kt', 'w') as f:
    f.write(content)
