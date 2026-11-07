package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parse_nullOrBlankContent_returnsEmptyUnsynchronized() {
        val resultNull = LrcParser.parse(null)
        assertFalse(resultNull.isSynchronized)
        assertTrue(resultNull.lines.isEmpty())
        assertEquals("", resultNull.rawText)

        val resultBlank = LrcParser.parse("   \n\t  ")
        assertFalse(resultBlank.isSynchronized)
        assertTrue(resultBlank.lines.isEmpty())
    }

    @Test
    fun parse_standardTwoDigitMillis_formatsCorrectly() {
        // [01:23.45] -> (1 * 60 + 23) * 1000 + 450 = 83450 ms
        val lrc = "[01:23.45] First line of lyrics"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynchronized)
        assertEquals(1, result.lines.size)
        assertEquals(83450L, result.lines[0].timeMs)
        assertEquals("First line of lyrics", result.lines[0].text)
    }

    @Test
    fun parse_threeDigitMillis_formatsCorrectly() {
        // [02:05.123] -> (2 * 60 + 5) * 1000 + 123 = 125123 ms
        val lrc = "[02:05.123] High precision timestamp"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynchronized)
        assertEquals(1, result.lines.size)
        assertEquals(125123L, result.lines[0].timeMs)
        assertEquals("High precision timestamp", result.lines[0].text)
    }

    @Test
    fun parse_singleDigitMillis_formatsCorrectly() {
        // [00:10.5] -> 10 * 1000 + 500 = 10500 ms
        val lrc = "[00:10.5] Single fraction"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynchronized)
        assertEquals(1, result.lines.size)
        assertEquals(10500L, result.lines[0].timeMs)
        assertEquals("Single fraction", result.lines[0].text)
    }

    @Test
    fun parse_noMillisTimestamp_formatsCorrectly() {
        // [01:15] -> (1 * 60 + 15) * 1000 = 75000 ms
        val lrc = "[01:15] Seconds only"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynchronized)
        assertEquals(1, result.lines.size)
        assertEquals(75000L, result.lines[0].timeMs)
        assertEquals("Seconds only", result.lines[0].text)
    }

    @Test
    fun parse_multipleTimestampsOnSingleLine_createsSortedEntries() {
        val lrc = "[00:05.00][00:25.00] Repeated chorus"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynchronized)
        assertEquals(2, result.lines.size)
        assertEquals(5000L, result.lines[0].timeMs)
        assertEquals("Repeated chorus", result.lines[0].text)
        assertEquals(25000L, result.lines[1].timeMs)
        assertEquals("Repeated chorus", result.lines[1].text)
    }

    @Test
    fun parse_metadataTags_areIgnored() {
        val lrc = """
            [ti:Test Title]
            [ar:Test Artist]
            [al:Test Album]
            [by:Creator]
            [offset:+200]
            [length:03:45]
            [00:12.00] Actual song starts here
        """.trimIndent()

        val result = LrcParser.parse(lrc)
        assertTrue(result.isSynchronized)
        assertEquals(1, result.lines.size)
        assertEquals(12000L, result.lines[0].timeMs)
        assertEquals("Actual song starts here", result.lines[0].text)
    }

    @Test
    fun parse_plainTextWithoutTimestamps_fallbackToUnsynchronized() {
        val plain = """
            Line one without timestamps
            Line two without timestamps
            Line three without timestamps
        """.trimIndent()

        val result = LrcParser.parse(plain)
        assertFalse(result.isSynchronized)
        assertEquals(3, result.lines.size)
        assertEquals(0L, result.lines[0].timeMs)
        assertEquals("Line one without timestamps", result.lines[0].text)
        assertEquals(1000L, result.lines[1].timeMs)
        assertEquals("Line two without timestamps", result.lines[1].text)
        assertEquals(2000L, result.lines[2].timeMs)
        assertEquals("Line three without timestamps", result.lines[2].text)
    }

    @Test
    fun parse_malformedTimestampsAndEmptyLines_handledGracefully() {
        val lrc = """
            [invalid:tag] Not a real tag
            [999:999] Malformed line
            
            [00:10.00] Valid lyric line
            
            Just random text
        """.trimIndent()

        val result = LrcParser.parse(lrc)
        assertTrue(result.isSynchronized)
        assertEquals(1, result.lines.size)
        assertEquals(10000L, result.lines[0].timeMs)
        assertEquals("Valid lyric line", result.lines[0].text)
    }

    @Test
    fun parse_unorderedTimestamps_areSortedChronologically() {
        val lrc = """
            [00:30.00] Verse two
            [00:10.00] Intro
            [00:20.00] Verse one
        """.trimIndent()

        val result = LrcParser.parse(lrc)
        assertTrue(result.isSynchronized)
        assertEquals(3, result.lines.size)
        assertEquals(10000L, result.lines[0].timeMs)
        assertEquals("Intro", result.lines[0].text)
        assertEquals(20000L, result.lines[1].timeMs)
        assertEquals("Verse one", result.lines[1].text)
        assertEquals(30000L, result.lines[2].timeMs)
        assertEquals("Verse two", result.lines[2].text)
    }

    @Test
    fun findActiveLineIndex_correctlyIdentifiesActiveLine() {
        val lines = listOf(
            LrcLine(1000L, "First line"),
            LrcLine(5000L, "Second line"),
            LrcLine(10000L, "Third line")
        )

        // Before first line
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 500L))

        // Exactly at first line
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 1000L))

        // Between first and second
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 3000L))

        // Exactly at second line
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 5000L))

        // Between second and third
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 8000L))

        // Exactly at third line
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 10000L))

        // Long after third line
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 99999L))

        // Empty lines
        assertEquals(-1, LrcParser.findActiveLineIndex(emptyList(), 1000L))
    }
}
