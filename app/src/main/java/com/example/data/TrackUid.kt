package com.example.data

import java.security.MessageDigest
import java.util.Locale

/**
 * Stable song identity.
 *
 * Database row IDs are not stable: a track whose file is missing during a scan is deleted and gets a
 * new ID when it comes back. Anything persisted (playlist entries, play history, the saved queue)
 * therefore refers to songs by this UID, which is derived from the song itself:
 *
 *  - `mb:<id>` when the file carries a MusicBrainz track ID (globally unique, survives re-tagging);
 *  - otherwise `s1:<md5>` of its normalized tags. The album falls back to the containing folder's
 *    name so untagged files ("Track 01" in many folders) don't collide.
 *
 * The `s1` prefix versions the recipe: if it ever has to change, old UIDs remain recognisable and can
 * be migrated instead of silently breaking playlists. (Idea inspired by Auxio's UID design.)
 */
object TrackUid {
    private const val RECIPE_PREFIX = "s1:"
    private const val MUSICBRAINZ_PREFIX = "mb:"
    private val UNKNOWN_ALBUMS = setOf("", "unknown album", "unknown", "<unknown>")
    private val MBID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun of(track: TrackEntity): String = compute(
        title = track.title,
        artist = track.artist,
        album = track.album,
        albumArtist = track.albumArtist,
        year = track.year,
        trackNumber = track.trackNumber,
        discNumber = track.discNumber,
        musicBrainzId = track.musicBrainzId,
        filePath = track.filePath,
        sourceUri = track.sourceUri
    )

    fun compute(
        title: String?,
        artist: String?,
        album: String?,
        albumArtist: String?,
        year: Int?,
        trackNumber: Int?,
        discNumber: Int?,
        musicBrainzId: String?,
        filePath: String?,
        sourceUri: String?
    ): String {
        val mbid = musicBrainzId?.trim()
        if (mbid != null && MBID.matches(mbid)) return MUSICBRAINZ_PREFIX + mbid.lowercase(Locale.ROOT)

        val normalizedAlbum = norm(album)
        val albumOrFolder = if (normalizedAlbum in UNKNOWN_ALBUMS) {
            "dir:" + norm(folderName(filePath, sourceUri))
        } else {
            normalizedAlbum
        }
        val material = listOf(
            norm(title),
            albumOrFolder,
            year?.toString().orEmpty(),
            trackNumber?.toString().orEmpty(),
            discNumber?.toString().orEmpty(),
            norm(artist),
            norm(albumArtist)
        ).joinToString("\u0000")
        val digest = MessageDigest.getInstance("MD5").digest(material.toByteArray(Charsets.UTF_8))
        return RECIPE_PREFIX + digest.joinToString("") { "%02x".format(it) }
    }

    private fun norm(value: String?): String = value?.trim()?.lowercase(Locale.ROOT).orEmpty()

    /** Name of the folder containing the file, from a real path or a SAF/MediaStore document URI. */
    internal fun folderName(filePath: String?, sourceUri: String?): String {
        val path = when {
            !filePath.isNullOrBlank() && !filePath.contains("://") -> filePath
            !filePath.isNullOrBlank() && filePath.startsWith("file://") -> decode(filePath.removePrefix("file://"))
            !sourceUri.isNullOrBlank() && sourceUri.startsWith("file://") -> decode(sourceUri.removePrefix("file://"))
            // SAF document URIs end in an encoded document id such as "primary%3AMusic%2FAlbum%2Fsong.mp3".
            !sourceUri.isNullOrBlank() -> decode(sourceUri.substringAfterLast('/')).substringAfter(':')
            !filePath.isNullOrBlank() -> decode(filePath.substringAfterLast('/')).substringAfter(':')
            else -> ""
        }
        val parent = path.trimEnd('/').substringBeforeLast('/', missingDelimiterValue = "")
        return parent.substringAfterLast('/')
    }

    private fun decode(s: String): String = try {
        java.net.URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
    } catch (e: Exception) {
        s
    }
}
