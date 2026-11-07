with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

old_sort = """        val result = when (sortOption) {
            "Newest" -> tracks.sortedByDescending { it.id }
            "Oldest" -> tracks.sortedBy { it.id }
            "Title A-Z" -> tracks.sortedBy { it.title.lowercase() }
            "Title Z-A" -> tracks.sortedByDescending { it.title.lowercase() }
            "Artist" -> tracks.sortedBy { 
                val a = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                a.lowercase()
            }
            "Album" -> tracks.sortedBy { it.album.lowercase() }
            else -> tracks
        }"""

new_sort = """        val result = when (sortOption) {
            "Custom" -> tracks.sortedByDescending { it.addedAt }
            "Newest" -> tracks.sortedByDescending { it.id }
            "Oldest" -> tracks.sortedBy { it.id }
            "Title A-Z" -> tracks.sortedBy { it.title.lowercase() }
            "Title Z-A" -> tracks.sortedByDescending { it.title.lowercase() }
            "Artist" -> tracks.sortedBy { 
                val a = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                a.lowercase()
            }
            "Album" -> tracks.sortedBy { it.album.lowercase() }
            else -> tracks
        }"""

content = content.replace(old_sort, new_sort)
content = content.replace('listOf("Newest", "Oldest", "Title A-Z", "Title Z-A", "Artist", "Album")', 'listOf("Custom", "Newest", "Oldest", "Title A-Z", "Title Z-A", "Artist", "Album")')

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
