package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderComparatorTest {

    @Test
    fun testNumericOrdering() {
        val list = listOf("Track 10", "Track 1", "Track 2", "Track 20", "Track 3")
        val sorted = list.sortedWith(NaturalOrderComparator)
        val expected = listOf("Track 1", "Track 2", "Track 3", "Track 10", "Track 20")
        assertEquals(expected, sorted)
    }

    @Test
    fun testLeadingZeros() {
        val list = listOf("03 - Third", "01 - First", "10 - Tenth", "02 - Second")
        val sorted = list.sortedWith(NaturalOrderComparator)
        val expected = listOf("01 - First", "02 - Second", "03 - Third", "10 - Tenth")
        assertEquals(expected, sorted)
    }

    @Test
    fun testEqualStrings() {
        assertEquals(0, NaturalOrderComparator.compare("Song A", "Song A"))
        assertEquals(0, NaturalOrderComparator.compare(null, null))
    }

    @Test
    fun testNullHandling() {
        assertTrue(NaturalOrderComparator.compare(null, "Song") < 0)
        assertTrue(NaturalOrderComparator.compare("Song", null) > 0)
    }

    @Test
    fun testComplexTrackNames() {
        val list = listOf(
            "Disc 2 - Track 10",
            "Disc 1 - Track 1",
            "Disc 1 - Track 10",
            "Disc 1 - Track 2",
            "Disc 2 - Track 1"
        )
        val sorted = list.sortedWith(NaturalOrderComparator)
        val expected = listOf(
            "Disc 1 - Track 1",
            "Disc 1 - Track 2",
            "Disc 1 - Track 10",
            "Disc 2 - Track 1",
            "Disc 2 - Track 10"
        )
        assertEquals(expected, sorted)
    }
}
