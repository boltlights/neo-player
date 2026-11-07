import re
with open('app/src/main/java/com/example/data/MusicEntities.kt', 'r') as f:
    content = f.read()

target = r'''data class PlaybackStateEntity\(
    @PrimaryKey val id: Int = 0,
    val queueTrackIds: String,
    val currentTrackId: Int,
    val currentPositionMs: Int,
    val isShuffleEnabled: Boolean = false
\)'''

repl = r'''data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 0,
    val baseQueueTrackIds: String = "",
    val queueTrackIds: String,
    val currentTrackId: Int,
    val currentPositionMs: Int,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false
)'''

content = re.sub(target, repl, content)
with open('app/src/main/java/com/example/data/MusicEntities.kt', 'w') as f:
    f.write(content)
