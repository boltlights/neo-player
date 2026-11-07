with open('app/src/main/java/com/example/data/MusicDao.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    @Transaction
    suspend fun insertPlaybackHistory(history: PlaybackHistoryEntity) {
        insertPlaybackHistoryInternal(history)
        trimPlaybackHistory()
    }''',
'''    @Transaction
    suspend fun insertPlaybackHistory(history: PlaybackHistoryEntity) {
        insertPlaybackHistoryInternal(history)
        trimPlaybackHistory()
    }
    
    @Query("SELECT COUNT(*) FROM playback_history WHERE trackId = :trackId")
    suspend fun getPlayCountForTrack(trackId: Int): Int'''
)

with open('app/src/main/java/com/example/data/MusicDao.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/data/MusicRepository.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    suspend fun insertPlaybackHistory(history: PlaybackHistoryEntity) {
        musicDao.insertPlaybackHistory(history)
    }''',
'''    suspend fun insertPlaybackHistory(history: PlaybackHistoryEntity) {
        musicDao.insertPlaybackHistory(history)
    }
    
    suspend fun getPlayCountForTrack(trackId: Int): Int {
        return musicDao.getPlayCountForTrack(trackId)
    }'''
)
with open('app/src/main/java/com/example/data/MusicRepository.kt', 'w') as f:
    f.write(content)

