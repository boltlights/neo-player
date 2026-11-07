import re

with open('app/src/main/java/com/example/ui/screens/LibraryCategoryDetailView.kt', 'r') as f:
    content = f.read()

target1 = r'onShowProperties: \(TrackEntity\) -> Unit\n\)'
repl1 = r'onShowProperties: (TrackEntity) -> Unit,\n    onPlayArtist: (String) -> Unit = {}\n)'
content = re.sub(target1, repl1, content)

target2 = r'''                    onDelete = \{ onDeleteTrack\(track\) \},
                    onShowProperties = \{ onShowProperties\(track\) \}
                \)'''
repl2 = r'''                    onDelete = { onDeleteTrack(track) },
                    onShowProperties = { onShowProperties(track) },
                    onPlayArtist = onPlayArtist
                )'''
content = re.sub(target2, repl2, content)

with open('app/src/main/java/com/example/ui/screens/LibraryCategoryDetailView.kt', 'w') as f:
    f.write(content)
