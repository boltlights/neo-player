package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackUidTest {

    private fun uid(
        title: String = "Song",
        artist: String = "Artist",
        album: String = "Album",
        filePath: String = "/storage/emulated/0/Music/Album/song.mp3",
        sourceUri: String? = null,
        mbid: String? = null,
        track: Int? = 1
    ) = TrackUid.compute(title, artist, album, null, 2020, track, 1, mbid, filePath, sourceUri)

    @Test fun sameTagsSameUid_evenIfFileMoved() =
        assertEquals(uid(filePath = "/a/x.mp3"), uid(filePath = "/b/y.flac"))

    @Test fun normalizesCaseAndWhitespace() =
        assertEquals(uid(title = "Song"), uid(title = "  sOnG "))

    @Test fun differentTagsDifferentUid() {
        assertNotEquals(uid(title = "Song"), uid(title = "Other"))
        assertNotEquals(uid(track = 1), uid(track = 2))
    }

    @Test fun musicBrainzIdWins() {
        val mbid = "0B5A9E1C-1D2E-4F3A-8B9C-0D1E2F3A4B5C"
        assertEquals("mb:${mbid.lowercase()}", uid(mbid = mbid))
        assertEquals(uid(mbid = mbid), uid(title = "Retagged", mbid = mbid))
    }

    @Test fun malformedMusicBrainzIdIsIgnored() = assertTrue(uid(mbid = "not-an-id").startsWith("s1:"))

    @Test fun untaggedFilesInDifferentFoldersDoNotCollide() {
        val a = uid(title = "Track 01", album = "Unknown Album", filePath = "/Music/Rap/Track 01.mp3")
        val b = uid(title = "Track 01", album = "Unknown Album", filePath = "/Music/Jazz/Track 01.mp3")
        assertNotEquals(a, b)
    }

    @Test fun folderNameFromSafDocumentUri() = assertEquals(
        "Album",
        TrackUid.folderName(
            "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2FAlbum%2Fsong.mp3",
            "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2FAlbum%2Fsong.mp3"
        )
    )

    @Test fun folderNameFromPath() = assertEquals("Album", TrackUid.folderName("/storage/emulated/0/Music/Album/song.mp3", null))
}
