with open('app/src/main/java/com/example/data/MusicEntities.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val trackNumber: Int? = null\n)',
    'val trackNumber: Int? = null,\n    val musicBrainzId: String? = null,\n    val lyrics: String? = null\n)'
)

with open('app/src/main/java/com/example/data/MusicEntities.kt', 'w') as f:
    f.write(content)
