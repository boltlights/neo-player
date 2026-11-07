import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = r'''                            onRemoveTrack = \{ song ->
                                viewModel\.removeTrackFromPlaylist\(currentActivePlaylist\.id, song\.id\)
                            \},
                            onShowProperties = \{ song ->
                                trackToShowProperties = song
                            \},
                            onDeletePlaylist = \{
                                viewModel\.deletePlaylist\(currentActivePlaylist\.id\)
                                viewModel\.setActivePlaylistForViewer\(null\)
                                Toast\.makeText\(context, "Playlist deleted", Toast\.LENGTH_SHORT\)\.show\(\)
                            \},
                            onRenamePlaylist = \{ newName ->
                                viewModel\.renamePlaylist\(currentActivePlaylist, newName\)
                            \}'''

repl = r'''                            onRemoveTrack = { song ->
                                viewModel.removeTrackFromPlaylist(currentActivePlaylist.id, song.id)
                            },
                            onShowProperties = { song ->
                                trackToShowProperties = song
                            },
                            onDeletePlaylist = {
                                viewModel.deletePlaylist(currentActivePlaylist.id)
                                viewModel.setActivePlaylistForViewer(null)
                                Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                            },
                            onRenamePlaylist = { newName ->
                                viewModel.renamePlaylist(currentActivePlaylist, newName)
                            },
                            onUpdateCoverArt = { uri ->
                                viewModel.updatePlaylistCoverArt(currentActivePlaylist, uri)
                            }'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
