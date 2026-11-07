import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""            if (isCurrentTrack) {
                playbackController.stopAndClear()
            }""",
"""            if (isCurrentTrack) {
                playbackController.stopAndClear()
                _isPlayerExpanded.value = false
            }"""
)

content = content.replace(
"""            if (stopPlayer) {
                playbackController.stopAndClear()
            }""",
"""            if (stopPlayer) {
                playbackController.stopAndClear()
                _isPlayerExpanded.value = false
            }"""
)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
