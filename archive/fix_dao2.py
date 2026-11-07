with open('app/src/main/java/com/example/data/MusicDao.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    @Query("SELECT COUNT(*) FROM playback_history WHERE trackId = :trackId")
    suspend fun getPlayCountForTrack(trackId: Int): Int''',
'''    @Query("SELECT COUNT(*) FROM playback_history WHERE trackId = :trackId")
    suspend fun getPlayCountForTrack(trackId: Int): Int
    
    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getTracksByIds(ids: List<Int>): List<TrackEntity>'''
)

with open('app/src/main/java/com/example/data/MusicDao.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/data/MusicRepository.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    suspend fun getPlayCountForTrack(trackId: Int): Int {
        return musicDao.getPlayCountForTrack(trackId)
    }''',
'''    suspend fun getPlayCountForTrack(trackId: Int): Int {
        return musicDao.getPlayCountForTrack(trackId)
    }
    
    suspend fun getTracksByIds(ids: List<Int>): List<TrackEntity> {
        return musicDao.getTracksByIds(ids)
    }'''
)
with open('app/src/main/java/com/example/data/MusicRepository.kt', 'w') as f:
    f.write(content)
