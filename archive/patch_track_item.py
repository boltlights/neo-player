import re
with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'r') as f:
    content = f.read()

target = r'''@Composable
fun PlaylistTrackItem\(
    song: TrackEntity,
    accentColor: Color,
    onPlay: \(\) -> Unit,
    onRemove: \(\) -> Unit,
    onShowProperties: \(\) -> Unit = \{\}
\) \{.*?\}
\}'''
repl = r'''@Composable
fun PlaylistTrackItem(
    song: TrackEntity,
    accentColor: Color,
    onPlay: () -> Unit,
    onShowProperties: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtworkWidget(
            artPath = song.artPath,
            trackTitle = song.title,
            accentColor = accentColor,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp)),
            sizeLimit = 180
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // If you have explicit tag logic, place here. For now just Artist
                Text(song.artist.ifEmpty { "Unknown Artist" }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        
        IconButton(onClick = onShowProperties) {
            Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}'''
content = re.sub(target, repl, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'w') as f:
    f.write(content)
