import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

target = r'''    fun updatePlaylistCoverArt\(playlist: PlaylistEntity, uri: String\?\) \{
        viewModelScope\.launch\(Dispatchers\.IO\) \{
            try \{
                repository\.updatePlaylist\(playlist\.copy\(coverArtUri = uri\)\)
            \} catch \(e: Exception\) \{
                e\.printStackTrace\(\)
            \}
        \}
    \}'''

repl = r'''    fun updatePlaylistCoverArt(playlist: PlaylistEntity, uri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = playlist.copy(coverArtUri = uri)
                repository.updatePlaylist(updated)
                if (_activePlaylistForViewer.value?.id == updated.id) {
                    _activePlaylistForViewer.value = updated
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
