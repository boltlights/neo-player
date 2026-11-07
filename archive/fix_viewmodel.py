import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

target = r'''                            trackList,
                            baseTrackList,
                            trackList\.firstOrNull \{ it\.id == currentTrackId \},
                            savedPos,
                            isShuffleEnabled,
                            isRepeatEnabled'''
repl = r'''                            list = trackList,
                            baseList = baseTrackList,
                            activeTrack = trackList.firstOrNull { it.id == currentTrackId },
                            positionMs = savedPos,
                            isShuffleEnabled = isShuffleEnabled,
                            isRepeatEnabled = isRepeatEnabled'''
content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
