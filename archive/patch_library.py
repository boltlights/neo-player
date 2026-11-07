import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

target1 = r'''                                                onDelete = \{ onDeleteTrack\(track\) \},
                                                onShowProperties = \{ onShowProperties\(track\) \}
                                            \)'''

repl1 = r'''                                                onDelete = { onDeleteTrack(track) },
                                                onShowProperties = { onShowProperties(track) },
                                                onPlayArtist = { activeArtistDetails = it }
                                            )'''

content = re.sub(target1, repl1, content)

target2 = r'''            onDeleteTrack = onDeleteTrack,
            onShowProperties = onShowProperties
        \)'''

repl2 = r'''            onDeleteTrack = onDeleteTrack,
            onShowProperties = onShowProperties,
            onPlayArtist = { activeArtistDetails = it }
        )'''

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
