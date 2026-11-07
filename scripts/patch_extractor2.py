with open('app/src/main/java/com/example/util/MetadataExtractor.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)',
    'var albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)\n                albumArtist = normalizeMultiValue(albumArtist)'
)

with open('app/src/main/java/com/example/util/MetadataExtractor.kt', 'w') as f:
    f.write(content)
