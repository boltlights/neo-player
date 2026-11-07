package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MusicRepositoryTest {

    private lateinit var database: MusicDatabase
    private lateinit var repository: MusicRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MusicRepository(database.musicDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    private fun createSampleTrack(
        id: Int = 0,
        title: String = "Test Track",
        artist: String = "Test Artist",
        album: String = "Test Album",
        genre: String? = "Rock"
    ): TrackEntity {
        return TrackEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = 180000L,
            filePath = "/storage/emulated/0/Music/$title.mp3",
            artPath = null,
            genre = genre
        )
    }

    @Test
    fun insertAndGetTrack_persistsCorrectly() = runBlocking {
        val track = createSampleTrack(title = "Bohemian Rhapsody", artist = "Queen", album = "A Night at the Opera")
        val generatedId = repository.insertTrack(track).toInt()
        assertTrue(generatedId > 0)

        val retrieved = repository.getTrackById(generatedId)
        assertNotNull(retrieved)
        assertEquals("Bohemian Rhapsody", retrieved?.title)
        assertEquals("Queen", retrieved?.artist)
        assertEquals("A Night at the Opera", retrieved?.album)
    }

    @Test
    fun updateTrack_modifiesTrackAttributes() = runBlocking {
        val track = createSampleTrack(title = "Original Title", artist = "Original Artist")
        val id = repository.insertTrack(track).toInt()

        val inserted = repository.getTrackById(id)!!
        val updated = inserted.copy(title = "Updated Title", artist = "Updated Artist", genre = "Classical")
        repository.updateTrack(updated)

        val retrieved = repository.getTrackById(id)
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved?.title)
        assertEquals("Updated Artist", retrieved?.artist)
        assertEquals("Classical", retrieved?.genre)
    }

    @Test
    fun deleteTrack_removesTrackFromDatabase() = runBlocking {
        val track = createSampleTrack(title = "To Delete")
        val id = repository.insertTrack(track).toInt()
        assertNotNull(repository.getTrackById(id))

        repository.deleteTrack(id)
        assertNull(repository.getTrackById(id))
    }

    @Test
    fun getAllTracksSync_returnsAllInsertedTracks() = runBlocking {
        repository.insertTrack(createSampleTrack(title = "Song A"))
        repository.insertTrack(createSampleTrack(title = "Song B"))
        repository.insertTrack(createSampleTrack(title = "Song C"))

        val all = repository.getAllTracksSync()
        assertEquals(3, all.size)
        assertTrue(all.any { it.title == "Song A" })
        assertTrue(all.any { it.title == "Song B" })
        assertTrue(all.any { it.title == "Song C" })
    }

    @Test
    fun playlistOperations_createAddRemoveAndCascade() = runBlocking {
        val track1 = repository.getTrackById(repository.insertTrack(createSampleTrack(title = "Track 1")).toInt())!!
        val track2 = repository.getTrackById(repository.insertTrack(createSampleTrack(title = "Track 2")).toInt())!!

        // Create playlist with tracks
        repository.createPlaylist("Favorites", listOf(track1, track2))

        val playlists = repository.allPlaylists.first()
        assertEquals(1, playlists.size)
        val playlist = playlists.first()
        assertEquals("Favorites", playlist.name)

        val playlistTracks = database.musicDao().getTracksForPlaylist(playlist.id).first()
        assertEquals(2, playlistTracks.size)

        // Remove one track
        repository.removeTrackFromPlaylist(playlist.id, track1.id)
        val afterRemove = database.musicDao().getTracksForPlaylist(playlist.id).first()
        assertEquals(1, afterRemove.size)
        assertEquals(track2.id, afterRemove.first().id)

        // Rename playlist
        repository.renamePlaylist(playlist, "My Best Favorites")
        val renamed = repository.allPlaylists.first().first()
        assertEquals("My Best Favorites", renamed.name)

        // Delete playlist cascades cross references
        repository.deletePlaylist(renamed)
        assertTrue(repository.allPlaylists.first().isEmpty())
        assertTrue(repository.allPlaylistTrackCrossRefs.first().isEmpty())
    }

    @Test
    fun fts4Search_findsTracksByTitlePrefix() = runBlocking {
        repository.insertTrack(createSampleTrack(title = "Symphony No. 5", artist = "Ludwig van Beethoven", album = "Classical Masterpieces"))
        repository.insertTrack(createSampleTrack(title = "Stairway to Heaven", artist = "Led Zeppelin", album = "Led Zeppelin IV"))
        repository.insertTrack(createSampleTrack(title = "Hotel California", artist = "Eagles", album = "Hotel California"))

        // Search prefix "Symph"
        val results = repository.searchTracks("Symph").first()
        assertEquals(1, results.size)
        assertEquals("Symphony No. 5", results.first().title)
    }

    @Test
    fun fts4Search_findsTracksByArtist() = runBlocking {
        repository.insertTrack(createSampleTrack(title = "Song One", artist = "Radiohead", album = "OK Computer"))
        repository.insertTrack(createSampleTrack(title = "Song Two", artist = "Coldplay", album = "Parachutes"))

        val results = repository.searchTracks("Radiohead").first()
        assertEquals(1, results.size)
        assertEquals("Song One", results.first().title)
        assertEquals("Radiohead", results.first().artist)
    }

    @Test
    fun fts4Search_findsTracksByAlbum() = runBlocking {
        repository.insertTrack(createSampleTrack(title = "Breathe", artist = "Pink Floyd", album = "The Dark Side of the Moon"))
        repository.insertTrack(createSampleTrack(title = "Money", artist = "Pink Floyd", album = "The Dark Side of the Moon"))
        repository.insertTrack(createSampleTrack(title = "Yesterday", artist = "The Beatles", album = "Help!"))

        val results = repository.searchTracks("Dark").first()
        assertEquals(2, results.size)
        assertTrue(results.all { it.album == "The Dark Side of the Moon" })
    }

    @Test
    fun fts4Search_noMatchReturnsEmpty() = runBlocking {
        repository.insertTrack(createSampleTrack(title = "Thriller", artist = "Michael Jackson", album = "Thriller"))

        val results = repository.searchTracks("NonexistentTerm").first()
        assertTrue(results.isEmpty())
    }

    @Test
    fun playbackHistory_recordsPlaysAndCountsAccurately() = runBlocking {
        val trackId = repository.insertTrack(createSampleTrack(title = "Popular Song")).toInt()
        val uid = repository.getTrackById(trackId)!!.uid

        assertEquals(0, repository.getPlayCountForTrack(trackId))

        repository.insertPlaybackHistory(PlaybackHistoryEntity(trackUid = uid))
        repository.insertPlaybackHistory(PlaybackHistoryEntity(trackUid = uid))
        repository.insertPlaybackHistory(PlaybackHistoryEntity(trackUid = uid))

        assertEquals(3, repository.getPlayCountForTrack(trackId))
    }

    // --- Stable UID guarantees ---

    @Test
    fun insertedTracksGetStableUid() = runBlocking {
        val id = repository.insertTrack(createSampleTrack(title = "Uid Song")).toInt()
        val stored = repository.getTrackById(id)!!
        assertTrue(stored.uid.startsWith("s1:"))
        assertEquals(TrackUid.of(stored), stored.uid)
    }

    @Test
    fun playlistSurvivesTrackRemovedAndRescanned() = runBlocking {
        val id = repository.insertTrack(createSampleTrack(title = "Comes Back")).toInt()
        val playlistId = repository.insertPlaylist(PlaylistEntity(name = "Keep")).toInt()
        repository.addTrackToPlaylist(playlistId, id)

        // Scanner removes the row (file temporarily unavailable): the song is hidden, not lost.
        repository.deleteTrack(id)
        assertTrue(database.musicDao().getTracksForPlaylist(playlistId).first().isEmpty())

        // File returns and is re-imported under a brand new row ID.
        val newId = repository.insertTrack(createSampleTrack(title = "Comes Back")).toInt()
        assertTrue(newId != id)
        val back = database.musicDao().getTracksForPlaylist(playlistId).first()
        assertEquals(listOf(newId), back.map { it.id })
    }

    @Test
    fun playlistKeepsInsertionOrder() = runBlocking {
        val a = repository.insertTrack(createSampleTrack(title = "A")).toInt()
        val b = repository.insertTrack(createSampleTrack(title = "B")).toInt()
        val c = repository.insertTrack(createSampleTrack(title = "C")).toInt()
        val playlistId = repository.insertPlaylist(PlaylistEntity(name = "Ordered")).toInt()
        repository.addTrackToPlaylist(playlistId, b)
        repository.addTrackToPlaylist(playlistId, c)
        repository.addTrackToPlaylist(playlistId, a)
        assertEquals(listOf("B", "C", "A"), database.musicDao().getTracksForPlaylist(playlistId).first().map { it.title })
        assertEquals(listOf(b, c, a), repository.allPlaylistTrackCrossRefs.first().map { it.trackId })
    }

    @Test
    fun retaggedTrackKeepsPlaylistAndHistory() = runBlocking {
        val id = repository.insertTrack(createSampleTrack(title = "Typo Titel")).toInt()
        val playlistId = repository.insertPlaylist(PlaylistEntity(name = "Retag")).toInt()
        repository.addTrackToPlaylist(playlistId, id)
        repository.insertPlaybackHistory(PlaybackHistoryEntity(trackUid = repository.getTrackById(id)!!.uid))

        // Scanner picks up corrected tags for the same file.
        repository.updateTrack(repository.getTrackById(id)!!.copy(title = "Typo Title"))

        val updated = repository.getTrackById(id)!!
        assertEquals(TrackUid.of(updated), updated.uid)
        assertEquals(listOf(id), database.musicDao().getTracksForPlaylist(playlistId).first().map { it.id })
        assertEquals(1, repository.getPlayCountForTrack(id))
    }

    @Test
    fun tracksByUidsChunksLargeLists() = runBlocking {
        val uids = (1..1500).map { i ->
            val id = repository.insertTrack(createSampleTrack(title = "Bulk $i")).toInt()
            repository.getTrackById(id)!!.uid
        }
        assertEquals(1500, repository.getTracksByUids(uids).size)
    }
}
