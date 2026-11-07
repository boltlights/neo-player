import re

with open('app/src/main/java/com/example/util/MetadataExtractor.kt', 'r') as f:
    content = f.read()

new_extract = """    fun extract(context: Context, uriString: String, fallbackPath: String?): ExtractedMetadata {
        val targetPath = fallbackPath?.takeIf { it.isNotEmpty() } ?: uriString.removePrefix("file://")
        var file = File(targetPath)
        var tempFile: File? = null
        
        if (!file.exists() || !file.canRead()) {
            try {
                if (uriString.startsWith("content://")) {
                    val uri = Uri.parse(uriString)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        tempFile = File.createTempFile("metadata_temp", ".tmp", context.cacheDir)
                        tempFile.outputStream().use { out ->
                            inputStream.copyTo(out)
                        }
                        file = tempFile
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (file.exists() && file.isFile) {
            try {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tag
                val header = audioFile.audioHeader
                
                var title = tag?.getFirst(FieldKey.TITLE)
                var artist = tag?.getFirst(FieldKey.ARTIST)
                val album = tag?.getFirst(FieldKey.ALBUM)
                var albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)
                albumArtist = normalizeMultiValue(albumArtist)
                val genre = tag?.getFirst(FieldKey.GENRE)
                val durationMs = header?.trackLength?.toLong()?.times(1000L)
                
                val discNumberStr = tag?.getFirst(FieldKey.DISC_NO)
                var discNumber = discNumberStr?.split("/")?.get(0)?.toIntOrNull()
                
                val trackNumberStr = tag?.getFirst(FieldKey.TRACK)
                var trackNumber = trackNumberStr?.split("/")?.get(0)?.toIntOrNull()
                
                val musicBrainzId = tag?.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID)
                val lyrics = tag?.getFirst(FieldKey.LYRICS)
                artist = normalizeMultiValue(artist)
                
                if (title.isNullOrEmpty() || artist.isNullOrEmpty()) {
                    val fileName = File(targetPath).nameWithoutExtension
                    if (title.isNullOrEmpty()) {
                        if (fileName.contains(" - ")) {
                            title = fileName.substringAfter(" - ").trim()
                            if (artist.isNullOrEmpty()) {
                                artist = fileName.substringBefore(" - ").trim()
                            }
                        } else {
                            title = fileName
                        }
                    }
                }
                
                tempFile?.delete()
                return ExtractedMetadata(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    genre = genre,
                    durationMs = durationMs,
                    discNumber = discNumber,
                    trackNumber = trackNumber,
                    musicBrainzId = musicBrainzId,
                    lyrics = lyrics
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to MediaMetadataRetriever
            } finally {
                tempFile?.delete()
            }
        }
        
        return extractWithRetriever(context, uriString, fallbackPath)
    }"""

content = re.sub(r'    fun extract\(context: Context, uriString: String, fallbackPath: String\?\): ExtractedMetadata \{.*?return extractWithRetriever\(context, uriString, fallbackPath\)\n    \}', new_extract, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/util/MetadataExtractor.kt', 'w') as f:
    f.write(content)
