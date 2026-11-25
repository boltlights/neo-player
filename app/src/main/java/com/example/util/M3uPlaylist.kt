package com.example.util

/**
 * Reads .m3u / .m3u8 playlists and matches their entries to songs in the library.
 *
 * Works with playlists from Neo for Windows' "Phone sync" (`#PLAYLIST:<name>` and relative
 * paths like `../Music/Artist/Album/01 Song.flac`), Neo's own export, MusicBee and other players
 * (Windows paths like `C:\Music\a.mp3`, URL-encoded entries).
 *
 * Matching, per entry: songs with the same file name are the candidates; among them the one whose
 * path ends with the longest part of the entry's path wins (`Artist/Album/01 Song.flac` before
 * `Album/01 Song.flac`), so two different `01 Intro.flac` files are told apart. If the path
 * doesn't settle it, the first song with that file name is used, as before.
 */
object M3uPlaylist {

    data class Parsed(val name: String?, val entries: List<String>)

    fun parse(lines: List<String>): Parsed {
        var name: String? = null
        val entries = mutableListOf<String>()
        for (raw in lines) {
            val l = raw.trim().removePrefix("\uFEFF")
            if (l.isEmpty()) continue
            if (l.startsWith("#")) {
                if (name == null && l.startsWith("#PLAYLIST:", ignoreCase = true)) {
                    name = l.substringAfter(":").trim().ifEmpty { null }
                }
                continue
            }
            entries.add(l)
        }
        return Parsed(name, entries)
    }

    /** Percent-decoding that leaves `+` alone (file names can contain it). */
    fun decode(s: String): String {
        if (!s.contains('%')) return s
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hex = s.substring(i + 1, i + 3).toIntOrNull(16)
                if (hex != null) {
                    out.write(hex)
                    i += 3
                    continue
                }
            }
            out.write(c.toString().toByteArray(Charsets.UTF_8))
            i++
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    /** Lower-case, forward slashes, decoded. */
    fun normalise(path: String): String = decode(path).replace('\\', '/').lowercase()

    /** The entry's path segments, without `..`, `.` and drive letters. */
    private fun segments(entry: String): List<String> =
        normalise(entry).split('/').filter { it.isNotEmpty() && it != ".." && it != "." && !it.endsWith(":") }

    /**
     * Returns the matched songs in playlist order (entries with no match are skipped).
     * [pathsOf] gives the places a song is known by (file path, content URI…).
     */
    fun <T> match(entries: List<String>, songs: List<T>, pathsOf: (T) -> List<String?>): List<T> {
        // file name → (song, its normalised paths)
        val byName = HashMap<String, MutableList<Pair<T, List<String>>>>()
        for (s in songs) {
            val paths = pathsOf(s).filterNotNull().filter { it.isNotEmpty() }.map { normalise(it) }
            val names = paths.map { it.substringAfterLast('/').substringAfterLast(':') }.toSet()
            for (n in names) byName.getOrPut(n) { mutableListOf() }.add(s to paths)
        }
        val out = ArrayList<T>(entries.size)
        for (e in entries) {
            val seg = segments(e)
            val file = seg.lastOrNull() ?: continue
            val candidates = byName[file] ?: continue
            if (candidates.size == 1) {
                out.add(candidates[0].first)
                continue
            }
            var best: T? = null
            for (k in seg.size downTo 2) {
                val tail = seg.takeLast(k).joinToString("/")
                val hits = candidates.filter { (_, paths) ->
                    paths.any { it == tail || it.endsWith("/$tail") || it.endsWith(":$tail") }
                }
                if (hits.isNotEmpty()) {
                    best = hits[0].first
                    break
                }
            }
            out.add(best ?: candidates[0].first)
        }
        return out
    }
}
