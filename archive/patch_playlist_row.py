import re
with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'r') as f:
    content = f.read()

target = r'''            // Playlist Art Thumbnail
            com\.example\.ui\.components\.MosaicCoverArt\(
                imageUris = tracksInPlaylist\.map \{ it\.artPath \},
                modifier = Modifier\.size\(48\.dp\)\.clip\(RoundedCornerShape\(12\.dp\)\),
                accentColor = accentColor
            \)'''

repl = r'''            // Playlist Art Thumbnail
            if (playlist.coverArtUri != null) {
                coil.compose.AsyncImage(
                    model = playlist.coverArtUri,
                    contentDescription = "Playlist Cover",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                com.example.ui.components.MosaicCoverArt(
                    imageUris = tracksInPlaylist.map { it.artPath },
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                    accentColor = accentColor,
                    iconIfEmpty = androidx.compose.material.icons.Icons.Default.MusicNote
                )
            }'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'w') as f:
    f.write(content)
