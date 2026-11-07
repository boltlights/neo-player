with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '    onPlayNext: (TrackEntity) -> Unit,',
    '    onPlayNextTracks: (List<TrackEntity>) -> Unit = {},\n    onAddToQueueTracks: (List<TrackEntity>) -> Unit = {},\n    onPlayNext: (TrackEntity) -> Unit,'
)

batch_actions = """                                            IconButton(onClick = { showPlaylistSelectDialogForGroupAdd = true }) {
                                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = Color.White)
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (selectedTracks.isNotEmpty()) {
                                                        onPlayNextTracks(selectedTracks.toList())
                                                        isMultiSelectActive = false
                                                        selectedTracks.clear()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Play next", tint = Color.White)
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (selectedTracks.isNotEmpty()) {
                                                        onAddToQueueTracks(selectedTracks.toList())
                                                        isMultiSelectActive = false
                                                        selectedTracks.clear()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Add to queue", tint = Color.White)
                                            }"""

content = content.replace(
    '                                            IconButton(onClick = { showPlaylistSelectDialogForGroupAdd = true }) {\n                                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = Color.White)\n                                            }',
    batch_actions
)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
