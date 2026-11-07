package com.example.util

import com.example.data.TrackEntity
import java.io.File
import java.util.Comparator

/**
 * NaturalOrderComparator performs natural alphanumeric comparison.
 * Strings like "Track 2" will be sorted before "Track 10",
 * and "01 - Intro" before "02 - Song" before "10 - Outro".
 */
object NaturalOrderComparator : Comparator<String> {
    private val LEADING_NUMBER_REGEX = Regex("""^(\d+)(?:[\s.\-_|:]|$)""")

    override fun compare(a: String?, b: String?): Int {
        if (a == null && b == null) return 0
        if (a == null) return -1
        if (b == null) return 1

        var ia = 0
        var ib = 0
        val nzA = a.length
        val nzB = b.length

        while (ia < nzA && ib < nzB) {
            val ca = a[ia]
            val cb = b[ib]

            if (ca.isDigit() && cb.isDigit()) {
                // Find complete number in both strings
                val startA = ia
                while (ia < nzA && a[ia].isDigit()) ia++
                val startB = ib
                while (ib < nzB && b[ib].isDigit()) ib++

                val numStrA = a.substring(startA, ia)
                val numStrB = b.substring(startB, ib)

                // Remove leading zeros for numerical comparison
                val trimmedA = numStrA.trimStart('0')
                val trimmedB = numStrB.trimStart('0')

                val lenA = trimmedA.length
                val lenB = trimmedB.length

                if (lenA != lenB) {
                    return lenA.compareTo(lenB)
                }

                val cmp = trimmedA.compareTo(trimmedB)
                if (cmp != 0) return cmp

                // If numerical value is identical (e.g., "01" vs "1"), compare length (shorter first)
                val lenDiff = numStrA.length.compareTo(numStrB.length)
                if (lenDiff != 0) return lenDiff
            } else {
                val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (cmp != 0) return cmp
                ia++
                ib++
            }
        }
        return nzA.compareTo(nzB)
    }

    /**
     * Extracts leading track number if present in text, e.g. "01 - Title" -> 1, "2. Track" -> 2
     */
    fun extractLeadingNumber(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()
        val match = LEADING_NUMBER_REGEX.find(trimmed)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    val TRACK_NUMBER_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        val disc1 = t1.discNumber ?: 1
        val disc2 = t2.discNumber ?: 1
        if (disc1 != disc2) return@Comparator disc1.compareTo(disc2)

        val num1 = t1.trackNumber
            ?: extractLeadingNumber(t1.title)
            ?: (if (t1.filePath.isNotEmpty()) extractLeadingNumber(File(t1.filePath).nameWithoutExtension) else null)
            ?: 9999
        val num2 = t2.trackNumber
            ?: extractLeadingNumber(t2.title)
            ?: (if (t2.filePath.isNotEmpty()) extractLeadingNumber(File(t2.filePath).nameWithoutExtension) else null)
            ?: 9999
        if (num1 != num2) return@Comparator num1.compareTo(num2)

        compare(t1.title, t2.title)
    }

    val FILENAME_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        val fn1 = if (t1.filePath.isNotEmpty()) File(t1.filePath).name else t1.title
        val fn2 = if (t2.filePath.isNotEmpty()) File(t2.filePath).name else t2.title
        compare(fn1, fn2)
    }

    val TITLE_ASC_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        compare(t1.title, t2.title)
    }

    val TITLE_DESC_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        compare(t2.title, t1.title)
    }

    val ARTIST_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        val a1 = if (!t1.albumArtist.isNullOrBlank()) t1.albumArtist else t1.artist
        val a2 = if (!t2.albumArtist.isNullOrBlank()) t2.albumArtist else t2.artist
        val cArtist = compare(a1, a2)
        if (cArtist != 0) return@Comparator cArtist

        val cAlbum = compare(t1.album, t2.album)
        if (cAlbum != 0) return@Comparator cAlbum

        TRACK_NUMBER_COMPARATOR.compare(t1, t2)
    }

    val ALBUM_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        val cAlbum = compare(t1.album, t2.album)
        if (cAlbum != 0) return@Comparator cAlbum

        TRACK_NUMBER_COMPARATOR.compare(t1, t2)
    }

    val ALBUM_TRACKS_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        TRACK_NUMBER_COMPARATOR.compare(t1, t2)
    }

    val ARTIST_TRACKS_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        val cAlbum = compare(t1.album, t2.album)
        if (cAlbum != 0) return@Comparator cAlbum

        TRACK_NUMBER_COMPARATOR.compare(t1, t2)
    }

    val GENRE_TRACKS_COMPARATOR: Comparator<TrackEntity> = Comparator { t1, t2 ->
        val cArtist = compare(t1.artist, t2.artist)
        if (cArtist != 0) return@Comparator cArtist

        val cAlbum = compare(t1.album, t2.album)
        if (cAlbum != 0) return@Comparator cAlbum

        TRACK_NUMBER_COMPARATOR.compare(t1, t2)
    }
}
