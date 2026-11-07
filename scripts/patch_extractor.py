with open('app/src/main/java/com/example/util/MetadataExtractor.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val trackNumber: Int?',
    'val trackNumber: Int?,\n    val musicBrainzId: String?,\n    val lyrics: String?'
)

content = content.replace(
"""                val trackNumberStr = tag?.getFirst(FieldKey.TRACK)
                var trackNumber = trackNumberStr?.split("/")?.get(0)?.toIntOrNull()""",
"""                val trackNumberStr = tag?.getFirst(FieldKey.TRACK)
                var trackNumber = trackNumberStr?.split("/")?.get(0)?.toIntOrNull()
                
                val musicBrainzId = tag?.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID)
                val lyrics = tag?.getFirst(FieldKey.LYRICS)"""
)

content = content.replace(
"""                    durationMs = durationMs,
                    discNumber = discNumber,
                    trackNumber = trackNumber""",
"""                    durationMs = durationMs,
                    discNumber = discNumber,
                    trackNumber = trackNumber,
                    musicBrainzId = musicBrainzId,
                    lyrics = lyrics"""
)

content = content.replace(
"""            var trackNumber = trackNumberStr?.split("/")?.get(0)?.toIntOrNull()
            
            artist = normalizeMultiValue(artist)""",
"""            var trackNumber = trackNumberStr?.split("/")?.get(0)?.toIntOrNull()
            
            val musicBrainzId: String? = null
            val lyrics: String? = null
            
            artist = normalizeMultiValue(artist)"""
)

content = content.replace(
"""                durationMs = durationMs,
                discNumber = discNumber,
                trackNumber = trackNumber
            )""",
"""                durationMs = durationMs,
                discNumber = discNumber,
                trackNumber = trackNumber,
                musicBrainzId = musicBrainzId,
                lyrics = lyrics
            )"""
)

content = content.replace(
    'return ExtractedMetadata(null, null, null, null, null, null, null, null)',
    'return ExtractedMetadata(null, null, null, null, null, null, null, null, null, null)'
)

with open('app/src/main/java/com/example/util/MetadataExtractor.kt', 'w') as f:
    f.write(content)
