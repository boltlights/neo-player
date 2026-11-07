with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertPlaylist(PlaylistEntity(name = name))
            }
        }
    }''',
'''    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createPlaylist(name, emptyList())
            }
        }
    }'''
)

content = content.replace(
'''    fun renamePlaylist(playlist: PlaylistEntity, newName: String) {
        if (playlist.id < 0) return
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.updatePlaylist(playlist.copy(name = newName))
            }
        }
    }''',
'''    fun renamePlaylist(playlist: PlaylistEntity, newName: String) {
        if (playlist.id < 0) return
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.renamePlaylist(playlist, newName)
            }
        }
    }'''
)

content = content.replace(
'''    fun deletePlaylist(playlistId: Int) {
        if (playlistId < 0) return
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }''',
'''    fun deletePlaylist(playlistId: Int) {
        if (playlistId < 0) return
        viewModelScope.launch {
            // Find playlist entity to match Auxio interface
            val playlists = repository.allPlaylists.value
            val playlist = playlists.find { it.id == playlistId }
            if (playlist != null) {
                repository.deletePlaylist(playlist)
            } else {
                repository.deletePlaylist(playlistId) // Fallback for backwards compat
            }
        }
    }'''
)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
