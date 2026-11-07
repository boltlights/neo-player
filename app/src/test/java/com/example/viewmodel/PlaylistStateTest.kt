package com.example.viewmodel

import com.example.data.PlaylistEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistStateTest {

    @Test
    fun testPlaylistEntityCreationAndCopy() {
        val playlist = PlaylistEntity(id = 1, name = "Favorites", coverArtUri = null)
        assertEquals(1, playlist.id)
        assertEquals("Favorites", playlist.name)
        assertNull(playlist.coverArtUri)

        val updated = playlist.copy(name = "Chill Vibes", coverArtUri = "content://media/1")
        assertEquals(1, updated.id)
        assertEquals("Chill Vibes", updated.name)
        assertEquals("content://media/1", updated.coverArtUri)
    }

    @Test
    fun testPlaylistDefaults() {
        val playlist = PlaylistEntity(name = "Workout")
        assertEquals(0, playlist.id)
        assertEquals("Workout", playlist.name)
        assertNull(playlist.coverArtUri)
    }
}
