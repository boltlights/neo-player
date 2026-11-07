with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

idx = content.find("private fun TrackArtworkAndInfoWrapper")
if idx != -1:
    before = content[:idx]
    after = content[idx:]
    after = after.replace('updatedTrack.', 'track.')
    content = before + after

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
