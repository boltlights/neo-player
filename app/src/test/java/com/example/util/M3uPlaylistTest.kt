package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uPlaylistTest {

    private data class Song(val id: Int, val path: String, val uri: String? = null)

    private fun ids(entries: List<String>, songs: List<Song>) =
        M3uPlaylist.match(entries, songs) { listOf(it.path, it.uri) }.map { it.id }

    @Test
    fun parsesNeoPhoneSyncPlaylist() {
        val text = "\uFEFF#EXTM3U\r\n#PLAYLIST:Gym: hard\r\n#EXTINF:61,Art - One\r\n../Music/Art/Alb/01 One.flac\r\n\r\n"
        val p = M3uPlaylist.parse(text.lines())
        assertEquals("Gym: hard", p.name)
        assertEquals(listOf("../Music/Art/Alb/01 One.flac"), p.entries)
    }

    @Test
    fun noNameWithoutPlaylistLine() {
        assertEquals(null, M3uPlaylist.parse(listOf("#EXTM3U", "a.mp3")).name)
    }

    @Test
    fun sameFileNameIsToldApartByFolders() {
        val songs = listOf(
            Song(1, "/storage/emulated/0/Music/Phone sync/Music/A/Live/01 Intro.flac"),
            Song(2, "/storage/emulated/0/Music/Phone sync/Music/B/Studio/01 Intro.flac"),
        )
        assertEquals(listOf(2, 1), ids(listOf("../Music/B/Studio/01 Intro.flac", "../Music/A/Live/01 Intro.flac"), songs))
    }

    @Test
    fun matchesFolderPickerUris() {
        val songs = listOf(
            Song(1, "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2FPhone%20sync%2FMusic%2FA%2F01%20Intro.flac"),
            Song(2, "/storage/emulated/0/Other/01 Intro.flac"),
        )
        assertEquals(listOf(1), ids(listOf("../Music/A/01 Intro.flac"), songs))
    }

    @Test
    fun windowsPathsAndUnknownFoldersFallBackToFileName() {
        val songs = listOf(Song(7, "/sdcard/Download/Song + Friends.mp3"))
        assertEquals(listOf(7), ids(listOf("C:\\Music\\Whatever\\Song + Friends.mp3"), songs))
        assertEquals(listOf(7), ids(listOf("Song%20%2B%20Friends.mp3"), songs))
    }

    @Test
    fun keepsOrderAndRepeatsAndSkipsMissing() {
        val songs = listOf(Song(1, "/m/a.mp3"), Song(2, "/m/b.mp3"))
        assertEquals(listOf(2, 1, 2), ids(listOf("b.mp3", "missing.mp3", "a.mp3", "b.mp3"), songs))
    }

    @Test
    fun caseInsensitive() {
        val songs = listOf(Song(1, "/m/Art/Song.FLAC"))
        assertEquals(listOf(1), ids(listOf("../music/art/song.flac"), songs))
    }
}
