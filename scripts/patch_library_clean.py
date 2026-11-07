with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

# Make sure imports are present
if "import com.example.util.NaturalOrderComparator" not in content:
    content = content.replace("package com.example.ui.screens", "package com.example.ui.screens\n\nimport com.example.util.NaturalOrderComparator", 1)

# Replace sorting logic
old_block = """    // Sorting logic (searching is now handled externally)
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

new_block = """    // Sorting logic (searching is now handled externally)
    val filteredTracks = remember(tracks, sortOption) {
        val result = when (sortOption) {
            "Track Number" -> tracks.sortedWith(NaturalOrderComparator.TRACK_NUMBER_COMPARATOR)
            "Filename" -> tracks.sortedWith(NaturalOrderComparator.FILENAME_COMPARATOR)
            "Title A-Z" -> tracks.sortedWith(NaturalOrderComparator.TITLE_ASC_COMPARATOR)
            "Title Z-A" -> tracks.sortedWith(NaturalOrderComparator.TITLE_DESC_COMPARATOR)
            "Artist" -> tracks.sortedWith(NaturalOrderComparator.ARTIST_COMPARATOR)
            "Album" -> tracks.sortedWith(NaturalOrderComparator.ALBUM_COMPARATOR)
            "Newest" -> tracks.sortedWith(compareByDescending<TrackEntity> { it.addedAt }.thenByDescending { it.id })
            "Oldest" -> tracks.sortedWith(compareBy<TrackEntity> { it.addedAt }.thenBy { it.id })
            "Custom" -> tracks
            else -> tracks.sortedWith(NaturalOrderComparator.TRACK_NUMBER_COMPARATOR)
        }
        result
    }"""

content = content.replace(old_block, new_block)

old_cat = """    if (activeAlbumDetails != null) {
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

new_cat = """    if (activeAlbumDetails != null) {
        val albumTracks = remember(activeAlbumDetails, tracks) {
            tracks.filter { it.album == activeAlbumDetails }.sortedWith(NaturalOrderComparator.ALBUM_TRACKS_COMPARATOR)
        }"""

content = content.replace(old_cat, new_cat)

old_art = """    } else if (activeArtistDetails != null) {
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

new_art = """    } else if (activeArtistDetails != null) {
        val artistTracks = remember(activeArtistDetails, tracks) {
            tracks.filter { 
                val effectiveArtist = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                effectiveArtist == activeArtistDetails 
            }.sortedWith(NaturalOrderComparator.ARTIST_TRACKS_COMPARATOR)
        }"""

content = content.replace(old_art, new_art)

old_gen = """    } else if (activeGenreDetails != null) {
        val genreTracks = remember(activeGenreDetails, tracks) {
            tracks.filter { getGenreForTrack(it) == activeGenreDetails }.sortedWith(
                compareBy(NaturalOrderComparator) { it.artist }
                    .thenBy(NaturalOrderComparator) { it.album }
                    .thenBy { it.trackNumber ?: 9999 }
                    .thenWith(NaturalOrderComparator) { it.title }
            )
        }"""

new_gen = """    } else if (activeGenreDetails != null) {
        val genreTracks = remember(activeGenreDetails, tracks) {
            tracks.filter { getGenreForTrack(it) == activeGenreDetails }.sortedWith(NaturalOrderComparator.GENRE_TRACKS_COMPARATOR)
        }"""

content = content.replace(old_gen, new_gen)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
print("Updated successfully")
