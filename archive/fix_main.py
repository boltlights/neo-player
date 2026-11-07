with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace('viewModel.playbackController.playNext(tracks)', 'viewModel.playbackController.playTracksNext(tracks)')
content = content.replace('viewModel.playbackController.playNext(track)', 'viewModel.playbackController.playTrackNext(track)')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
