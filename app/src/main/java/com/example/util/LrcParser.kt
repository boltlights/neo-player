package com.example.util

data class LrcLine(
    val timeMs: Long,
    val text: String
)

data class ParsedLyrics(
    val isSynchronized: Boolean,
    val lines: List<LrcLine>,
    val rawText: String
)

object LrcParser {
    // Matches patterns like [01:23.45] or [01:23.456] or [01:23]
    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]""")

    fun parse(content: String?): ParsedLyrics {
        if (content.isNullOrBlank()) {
            return ParsedLyrics(
                isSynchronized = false,
                lines = emptyList(),
                rawText = ""
            )
        }

        val rawLines = content.lines()
        val parsedLines = mutableListOf<LrcLine>()
        var foundTimestamp = false

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Skip metadata tags like [ti:...], [ar:...], [al:...], [by:...], [offset:...]
            if (trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") ||
                trimmed.startsWith("[offset:") || trimmed.startsWith("[length:")
            ) {
                continue
            }

            val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                foundTimestamp = true
                // The actual lyric text is everything after the last timestamp match
                val lastMatch = matches.last()
                val text = trimmed.substring(lastMatch.range.last + 1).trim()

                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val millisFraction = match.groupValues.getOrNull(3) ?: ""
                    val millis = when (millisFraction.length) {
                        1 -> millisFraction.toLong() * 100
                        2 -> millisFraction.toLong() * 10
                        3 -> millisFraction.toLong()
                        else -> 0L
                    }

                    val timeMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                    parsedLines.add(LrcLine(timeMs = timeMs, text = text))
                }
            }
        }

        if (foundTimestamp && parsedLines.isNotEmpty()) {
            // Sort lines by time
            val sortedLines = parsedLines.sortedBy { it.timeMs }
            return ParsedLyrics(
                isSynchronized = true,
                lines = sortedLines,
                rawText = content
            )
        }

        // Fallback to plain text lines
        val plainLines = rawLines
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("[") }
            .mapIndexed { index, text -> LrcLine(timeMs = index.toLong() * 1000L, text = text) }

        return ParsedLyrics(
            isSynchronized = false,
            lines = plainLines,
            rawText = content
        )
    }

    /**
     * Returns the index of the line that is active at currentTimeMs
     */
    fun findActiveLineIndex(lines: List<LrcLine>, currentTimeMs: Long): Int {
        if (lines.isEmpty()) return -1
        var activeIndex = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= currentTimeMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
}
