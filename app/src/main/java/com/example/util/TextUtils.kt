package com.example.util

/** "1 song" / "2 songs" (the UI previously always said "songs", e.g. "1 songs"). */
fun songCount(count: Int): String = if (count == 1) "1 song" else "$count songs"

/** Upper-case variant used in detail headers: "1 SONG IN ALBUM", "3 SONGS IN ALBUM". */
fun songCountCaps(count: Int, suffix: String): String = "${songCount(count).uppercase()} $suffix"

/** "Artist • Album", omitting blank parts so a missing album doesn't leave a dangling bullet. */
fun artistAndAlbum(artist: String?, album: String?): String =
    listOfNotNull(artist, album).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" • ")
