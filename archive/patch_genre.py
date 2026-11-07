import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('onPlayArtist = { activeArtistDetails = it }', 'onPlayArtist = { activeArtistDetails = it },\n                                                onPlayGenre = { activeGenreDetails = it }')

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/screens/LibraryCategoryDetailView.kt', 'r') as f:
    content2 = f.read()

content2 = content2.replace('onPlayArtist: (String) -> Unit = {}', 'onPlayArtist: (String) -> Unit = {},\n    onPlayGenre: (String) -> Unit = {}')
content2 = content2.replace('onPlayArtist = onPlayArtist', 'onPlayArtist = onPlayArtist,\n                    onPlayGenre = onPlayGenre')

with open('app/src/main/java/com/example/ui/screens/LibraryCategoryDetailView.kt', 'w') as f:
    f.write(content2)

