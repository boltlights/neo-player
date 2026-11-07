with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """package com.example.ui.screens

import java.io.File
import com.example.util.NaturalOrderComparator"""

content = content.replace("package com.example.ui.screens", imports, 1)

# Replace sorting logic
old_sort_logic = """    // Sorting logic (searching is now handled externally)
    val filteredTracks = remember(tracks, sortOption) {
        val result = when (sortOption) {
            "Newest" -> tracks.sortedByDescending { it.id }
            "Oldest" -> tracks.sortedBy { it.id }
            "Title A-Z" -> tracks.sortedBy { it.title.lowercase() }
            "Title Z-A" -> tracks.sortedByDescending { it.title.lowercase() }
            "Artist" -> tracks.sortedBy { 
                val effective = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                effective.lowercase()
            }
            "Album" -> tracks.sortedBy { it.album.lowercase() }
            else -> tracks
        }
        result
    }"""

new_sort_logic = """    // Sorting logic (searching is now handled externally)
    val filteredTracks = remember(tracks, sortOption) {
        val result = when (sortOption) {
            "Track Number" -> tracks.sortedWith(
                compareBy<TrackEntity> { it.discNumber ?: 1 }
                    .thenBy {
                        it.trackNumber
                            ?: NaturalOrderComparator.extractLeadingNumber(it.title)
                            ?: NaturalOrderComparator.extractLeadingNumber(if (it.filePath.isNotEmpty()) File(it.filePath).nameWithoutExtension else null)
                            ?: 9999
                    }
                    .thenWith(NaturalOrderComparator) { it.title }
            )
            "Filename" -> tracks.sortedWith(
                compareBy(NaturalOrderComparator) {
                    if (it.filePath.isNotEmpty()) File(it.filePath).name else it.title
                }
            )
            "Title A-Z" -> tracks.sortedWith(compareBy(NaturalOrderComparator) { it.title })
            "Title Z-A" -> tracks.sortedWith(compareByDescending(NaturalOrderComparator) { it.title })
            "Artist" -> tracks.sortedWith(
                compareBy(NaturalOrderComparator) {
                    if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                }.thenBy(NaturalOrderComparator) { it.album }
                .thenBy { it.discNumber ?: 1 }
                .thenBy {
                    it.trackNumber
                        ?: NaturalOrderComparator.extractLeadingNumber(it.title)
                        ?: 9999
                }
                .thenWith(NaturalOrderComparator) { it.title }
            )
            "Album" -> tracks.sortedWith(
                compareBy(NaturalOrderComparator) { it.album }
                    .thenBy { it.discNumber ?: 1 }
                    .thenBy {
                        it.trackNumber
                            ?: NaturalOrderComparator.extractLeadingNumber(it.title)
                            ?: 9999
                    }
                    .thenWith(NaturalOrderComparator) { it.title }
            )
            "Newest" -> tracks.sortedWith(compareByDescending<TrackEntity> { it.addedAt }.thenByDescending { it.id })
            "Oldest" -> tracks.sortedWith(compareBy<TrackEntity> { it.addedAt }.thenBy { it.id })
            "Custom" -> tracks
            else -> tracks.sortedWith(compareBy<TrackEntity> { it.addedAt }.thenBy { it.id })
        }
        result
    }"""

content = content.replace(old_sort_logic, new_sort_logic)

# Replace album, artist, genre, playlist tracks sorting
old_category_logic = """    if (activeAlbumDetails != null) {
        val albumTracks = remember(activeAlbumDetails, tracks) {
            tracks.filter { it.album == activeAlbumDetails }
        }"""

new_category_logic = """    if (activeAlbumDetails != null) {
        val albumTracks = remember(activeAlbumDetails, tracks) {
            tracks.filter { it.album == activeAlbumDetails }.sortedWith(
                compareBy<TrackEntity> { it.discNumber ?: 1 }
                    .thenBy {
                        it.trackNumber
                            ?: NaturalOrderComparator.extractLeadingNumber(it.title)
                            ?: NaturalOrderComparator.extractLeadingNumber(if (it.filePath.isNotEmpty()) File(it.filePath).nameWithoutExtension else null)
                            ?: 9999
                    }
                    .thenWith(NaturalOrderComparator) { it.title }
            )
        }"""

content = content.replace(old_category_logic, new_category_logic)

old_artist_logic = """    } else if (activeArtistDetails != null) {
        val artistTracks = remember(activeArtistDetails, tracks) {
            tracks.filter { 
                val effectiveArtist = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                effectiveArtist == activeArtistDetails 
            }
        }"""

new_artist_logic = """    } else if (activeArtistDetails != null) {
        val artistTracks = remember(activeArtistDetails, tracks) {
            tracks.filter { 
                val effectiveArtist = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                effectiveArtist == activeArtistDetails 
            }.sortedWith(
                compareBy(NaturalOrderComparator) { it.album }
                    .thenBy { it.discNumber ?: 1 }
                    .thenBy {
                        it.trackNumber
                            ?: NaturalOrderComparator.extractLeadingNumber(it.title)
                            ?: 9999
                    }
                    .thenWith(NaturalOrderComparator) { it.title }
            )
        }"""

content = content.replace(old_artist_logic, new_artist_logic)

old_genre_logic = """    } else if (activeGenreDetails != null) {
        val genreTracks = remember(activeGenreDetails, tracks) {
            tracks.filter { getGenreForTrack(it) == activeGenreDetails }
        }"""

new_genre_logic = """    } else if (activeGenreDetails != null) {
        val genreTracks = remember(activeGenreDetails, tracks) {
            tracks.filter { getGenreForTrack(it) == activeGenreDetails }.sortedWith(
                compareBy(NaturalOrderComparator) { it.artist }
                    .thenBy(NaturalOrderComparator) { it.album }
                    .thenBy { it.trackNumber ?: 9999 }
                    .thenWith(NaturalOrderComparator) { it.title }
            )
        }"""

content = content.replace(old_genre_logic, new_genre_logic)

old_pl_filter = """                    val trackIdsInPl = playlistCrossRefs.filter { it.playlistId == pl.id }.map { it.trackId }
                    tracks.filter { trackIdsInPl.contains(it.id) }"""

new_pl_filter = """                    val trackIdsInPl = playlistCrossRefs.filter { it.playlistId == pl.id }.map { it.trackId }
                    trackIdsInPl.mapNotNull { id -> tracks.find { it.id == id } }"""

content = content.replace(old_pl_filter, new_pl_filter)

# Update sort options dropdown
old_sort_menu = 'listOf("Custom", "Newest", "Oldest", "Title A-Z", "Title Z-A", "Artist", "Album").forEach { option ->'
new_sort_menu = 'listOf("Track Number", "Filename", "Title A-Z", "Title Z-A", "Artist", "Album", "Oldest", "Newest", "Custom").forEach { option ->'

content = content.replace(old_sort_menu, new_sort_menu)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
