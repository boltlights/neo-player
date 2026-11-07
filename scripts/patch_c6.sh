#!/bin/bash
sed -i 's/val extracted = MetadataExtractor.extract(context, uriString, row.data)/val title = row.title ?: "Unknown Title"\n                                val artist = row.artist ?: "Unknown Artist"\n                                val album = row.album ?: "Unknown Album"\n                                val duration = row.duration\n                                val albumArtist = row.albumArtist\n                                val trackNumber = row.trackNum\n                                val discNumber = row.discNum\n                                val genre = "Unknown"\n                                val musicBrainzId = null\n                                val lyrics = null/g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/val title = extracted.title ?: row.title ?: "Unknown Title"//g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/val artist = extracted.artist ?: row.artist ?: "Unknown Artist"//g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/val album = extracted.album ?: row.album ?: "Unknown Album"//g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/val duration = extracted.durationMs ?: row.duration//g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/val albumArtist = extracted.albumArtist ?: row.albumArtist//g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/val trackNumber = extracted.trackNumber ?: row.trackNum//g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/val discNumber = extracted.discNumber ?: row.discNum//g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/genre = extracted.genre ?: "Unknown"/genre = genre/g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/musicBrainzId = extracted.musicBrainzId/musicBrainzId = musicBrainzId/g' app/src/main/java/com/example/util/MediaStoreManager.kt
sed -i 's/lyrics = extracted.lyrics/lyrics = lyrics/g' app/src/main/java/com/example/util/MediaStoreManager.kt
