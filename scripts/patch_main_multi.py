with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '                    onPlayNext = { track ->',
    '                    onPlayNextTracks = { tracks ->\n                        viewModel.playbackController.playNext(tracks)\n                        Toast.makeText(context, "Playing ${tracks.size} tracks next", Toast.LENGTH_SHORT).show()\n                    },\n                    onAddToQueueTracks = { tracks ->\n                        viewModel.playbackController.addToQueue(tracks)\n                        Toast.makeText(context, "Added ${tracks.size} tracks to queue", Toast.LENGTH_SHORT).show()\n                    },\n                    onPlayNext = { track ->'
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
