import re
with open('app/src/main/java/com/example/data/MusicEntities.kt', 'r') as f:
    content = f.read()

target = r'''@Entity\(tableName = "playlists"\)
data class PlaylistEntity\(
    @PrimaryKey\(autoGenerate = true\) val id: Int = 0,
    val name: String,
    val createdAt: Long = System\.currentTimeMillis\(\)
\)'''

repl = r'''@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val coverArtUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/data/MusicEntities.kt', 'w') as f:
    f.write(content)
