import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

target = r'''    fun renamePlaylist\(playlist: PlaylistEntity, newName: String\) \{
        viewModelScope\.launch\(Dispatchers\.IO\) \{
            try \{
                repository\.updatePlaylist\(playlist\.copy\(name = newName\)\)
            \} catch \(e: Exception\) \{
                e\.printStackTrace\(\)
            \}
        \}
    \}'''

repl = r'''    fun renamePlaylist(playlist: PlaylistEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updatePlaylist(playlist.copy(name = newName))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updatePlaylistCoverArt(playlist: PlaylistEntity, uri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updatePlaylist(playlist.copy(coverArtUri = uri))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
