import re
with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'r') as f:
    content = f.read()

target1 = r'''    onPlayTrack: \(TrackEntity\) -> Unit,'''
repl1 = r'''    onPlayTrack: (TrackEntity) -> Unit,
    onShufflePlay: () -> Unit,'''

content = re.sub(target1, repl1, content)

target2 = r'''onClick = \{ onPlayTrack\(tracksInPlaylist\.random\(\)\) \}'''
repl2 = r'''onClick = { onShufflePlay() }'''

content = re.sub(target2, repl2, content)

target3 = r'''else if \(label == "Shuffle" && tracksInPlaylist\.isNotEmpty\(\)\) \{
                                        onPlayTrack\(tracksInPlaylist\.random\(\)\)
                                    \}'''
repl3 = r'''else if (label == "Shuffle" && tracksInPlaylist.isNotEmpty()) {
                                        onShufflePlay()
                                    }'''

content = re.sub(target3, repl3, content)

with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'w') as f:
    f.write(content)
