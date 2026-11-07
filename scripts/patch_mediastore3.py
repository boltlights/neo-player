with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'trackNumber = extracted.trackNumber\n                            )',
    'trackNumber = extracted.trackNumber,\n                                musicBrainzId = extracted.musicBrainzId,\n                                lyrics = extracted.lyrics\n                            )'
)

content = content.replace(
    'trackNumber = extracted.trackNumber\n                                    )',
    'trackNumber = extracted.trackNumber,\n                                        musicBrainzId = extracted.musicBrainzId,\n                                        lyrics = extracted.lyrics\n                                    )'
)

content = content.replace(
    'trackNumber = trackNumber,\n                            discNumber = discNumber,\n                            genre = genre,\n                            filePath = "",\n                            artPath = artUri.toString()\n                        )',
    'trackNumber = trackNumber,\n                            discNumber = discNumber,\n                            genre = genre,\n                            filePath = "",\n                            artPath = artUri.toString(),\n                            musicBrainzId = extracted.musicBrainzId,\n                            lyrics = extracted.lyrics\n                        )'
)

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
