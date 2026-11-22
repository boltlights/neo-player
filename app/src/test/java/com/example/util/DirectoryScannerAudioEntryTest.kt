package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectoryScannerAudioEntryTest {

    @Test
    fun playlistFilesAreNotSongs() {
        // Android reports playlists as audio MIME types; they must not become library "songs".
        assertFalse(DirectoryScanner.isAudioEntry("On the Road.m3u8", "audio/x-mpegurl"))
        assertFalse(DirectoryScanner.isAudioEntry("Mix.m3u", "audio/mpegurl"))
        assertFalse(DirectoryScanner.isAudioEntry("radio.pls", "audio/x-scpls"))
        assertFalse(DirectoryScanner.isAudioEntry(null, "application/vnd.apple.mpegurl"))
        assertFalse(DirectoryScanner.isAudioEntry("album.cue", "application/octet-stream"))
    }

    @Test
    fun songsStillCount() {
        assertTrue(DirectoryScanner.isAudioEntry("01. Alicia.flac", "audio/flac"))
        assertTrue(DirectoryScanner.isAudioEntry("a.MP3", null))
        assertTrue(DirectoryScanner.isAudioEntry(null, "audio/mpeg"))
        assertTrue(DirectoryScanner.isAudioEntry("x.oga", "application/ogg"))
        assertFalse(DirectoryScanner.isAudioEntry("cover.jpg", "image/jpeg"))
    }
}
