import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

target = r'''                track.artPath\?\.let \{ art ->
                    val realArt = art\.replaceFirst\("audiocover://","content://"\)
                    try \{
                        if \(realArt\.startsWith\("content://"\)\) \{
                            builder\.setArtworkUri\(android\.net\.Uri\.parse\(realArt\)\)
                        \} else \{
                            val file = java\.io\.File\(realArt\)
                            builder\.setArtworkUri\(android\.net\.Uri\.fromFile\(file\)\)
                        \}
                    \} catch \(e: Exception\) \{
                        // ignore malformed URI
                    \}
                \}'''

replacement = r'''                track.artPath?.let { art ->
                    try {
                        if (art.startsWith("audiocover://") || art.startsWith("content://")) {
                            builder.setArtworkUri(android.net.Uri.parse(art))
                        } else {
                            val file = java.io.File(art)
                            builder.setArtworkUri(android.net.Uri.fromFile(file))
                        }
                    } catch (e: Exception) {
                        // ignore malformed URI
                    }
                }'''

new_content = re.sub(target, replacement, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(new_content)
