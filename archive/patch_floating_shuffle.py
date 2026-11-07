import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = r'''                            val shuffledList = allTracks\.shuffled\(\)
                            if \(shuffledList\.isNotEmpty\(\)\) \{
                                viewModel\.playbackController\.setPlaybackList\(shuffledList, shuffledList\.first\(\)\)
                                Toast\.makeText\(context, "Queue shuffled & playback started", Toast\.LENGTH_SHORT\)\.show\(\)
                            \}'''

repl = r'''                            if (allTracks.isNotEmpty()) {
                                viewModel.playbackController.playShuffledList(allTracks)
                                Toast.makeText(context, "Queue smart-shuffled & playback started", Toast.LENGTH_SHORT).show()
                                viewModel.setPlayerExpanded(true)
                            }'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
