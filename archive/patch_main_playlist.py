import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = r'''                            onPlayTrack = \{ song ->
                                viewModel\.playbackController\.setPlaybackList\(tracksInPlaylist, song\)
                                viewModel\.setPlayerExpanded\(true\)
                            \},
                            onRemoveTrack = \{ song ->'''

repl = r'''                            onPlayTrack = { song ->
                                viewModel.playbackController.setPlaybackList(tracksInPlaylist, song)
                                viewModel.setPlayerExpanded(true)
                            },
                            onShufflePlay = {
                                viewModel.playbackController.playShuffledList(tracksInPlaylist)
                                viewModel.setPlayerExpanded(true)
                            },
                            onRemoveTrack = { song ->'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
