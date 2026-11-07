import re

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                val maxH = maxHeight
                val minDim = if (maxW < maxH) maxW else maxH
                val artSize = remember(minDim) {
                    val safeDim = minDim / 1.05f
                    if (albumArtStyle == AlbumArtStyle.ROUNDED) {
                        (safeDim - 16.dp).coerceAtMost(320.dp).coerceAtLeast(120.dp)
                    } else {
                        // Square/full bleed style maximizes space fully
                        (minDim).coerceAtLeast(180.dp)
                    }
                }''',
'''                val maxH = maxHeight
                val minDim = if (maxW < maxH) maxW else maxH
                val artSize = remember(minDim) {
                    if (albumArtStyle == AlbumArtStyle.ROUNDED) {
                        (minDim - 32.dp).coerceAtMost(320.dp).coerceAtLeast(120.dp)
                    } else {
                        // Square style uses Auxio spacing logic: 16dp horizontal margins
                        (minDim - 32.dp).coerceAtLeast(180.dp) 
                    }
                }'''
)

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
