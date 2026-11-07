package com.example.data

data class IndexedAlbum(
    val name: String,
    val artist: String,
    val tracks: List<TrackEntity>,
    val year: Int? = null,
    val artPath: String? = null
)

data class IndexedArtist(
    val name: String,
    val tracks: List<TrackEntity>,
    val albumCount: Int = 0
)

data class IndexedGenre(
    val name: String,
    val tracks: List<TrackEntity>
)
