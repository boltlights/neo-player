with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                val artSize = remember(minDim) {
                    val safeDim = minDim / 1.05f
                    (safeDim - 16.dp).coerceAtMost(320.dp).coerceAtLeast(120.dp)
                }''',
'''                val artSize = remember(minDim) {
                    val safeDim = minDim / 1.05f
                    if (albumArtStyle == AlbumArtStyle.ROUNDED) {
                        (safeDim - 16.dp).coerceAtMost(320.dp).coerceAtLeast(120.dp)
                    } else {
                        // Square/full bleed style gets more screen real estate, up to 400dp
                        (safeDim).coerceAtMost(400.dp).coerceAtLeast(180.dp)
                    }
                }'''
)

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
