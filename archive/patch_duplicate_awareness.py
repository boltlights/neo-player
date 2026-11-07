import re
with open('app/src/main/java/com/example/ui/components/PlaylistDialogs.kt', 'r') as f:
    content = f.read()

target = r'''@Composable
fun AddToPlaylistDialog\(
    viewModel: MusicViewModel,
    track: TrackEntity,
    allPlaylists: List<PlaylistEntity>,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onDismiss: \(\) -> Unit,
    onAdded: \(String\) -> Unit
\) \{
    var showNewPlaylistDialog by remember \{ mutableStateOf\(false\) \}'''

repl = r'''@Composable
fun AddToPlaylistDialog(
    viewModel: MusicViewModel,
    track: TrackEntity,
    allPlaylists: List<PlaylistEntity>,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit
) {
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    val crossRefs by viewModel.allPlaylistTrackCrossRefs.androidx.compose.runtime.collectAsState(initial = emptyList())'''

content = re.sub(target, repl, content)

target_item = r'''                        items\(allPlaylists\) \{ plist ->
                            Box\(
                                modifier = Modifier
                                    \.fillMaxWidth\(\)
                                    \.clickable \{
                                        viewModel\.addTrackToPlaylist\(plist\.id, track\.id\)
                                        onAdded\(plist\.name\)
                                        onDismiss\(\)
                                    \}
                                    \.padding\(vertical = 12\.dp, horizontal = 16\.dp\)
                            \) \{
                                Text\(
                                    plist\.name,
                                    fontSize = 15\.sp,
                                    fontFamily = FontFamily\.Monospace,
                                    fontWeight = FontWeight\.Medium
                                \)
                            \}
                            Box\(
                                modifier = Modifier
                                    \.fillMaxWidth\(\)
                                    \.height\(1\.dp\)
                                    \.background\(onBgColor\.copy\(alpha = 0\.1f\)\)
                            \)
                        \}'''

repl_item = r'''                        items(allPlaylists) { plist ->
                            val isAlreadyInPlaylist = crossRefs.any { it.playlistId == plist.id && it.trackId == track.id }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyInPlaylist) {
                                        viewModel.addTrackToPlaylist(plist.id, track.id)
                                        onAdded(plist.name)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    plist.name,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isAlreadyInPlaylist) onBgColor.copy(alpha = 0.4f) else onBgColor
                                )
                                
                                if (isAlreadyInPlaylist) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                        contentDescription = "Already added",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(onBgColor.copy(alpha = 0.1f))
                            )
                        }'''

content = re.sub(target_item, repl_item, content)

with open('app/src/main/java/com/example/ui/components/PlaylistDialogs.kt', 'w') as f:
    f.write(content)
