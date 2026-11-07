package com.example.util

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class BetterShuffleOrderTest {

    @Test
    fun testEmptyShuffleOrder() {
        val order = BetterShuffleOrder(0)
        assertEquals(0, order.length)
        assertEquals(C.INDEX_UNSET, order.firstIndex)
        assertEquals(C.INDEX_UNSET, order.lastIndex)
        assertEquals(C.INDEX_UNSET, order.getNextIndex(0))
        assertEquals(C.INDEX_UNSET, order.getPreviousIndex(0))
    }

    @Test
    fun testSingleItemShuffleOrder() {
        val order = BetterShuffleOrder(1)
        assertEquals(1, order.length)
        assertEquals(0, order.firstIndex)
        assertEquals(0, order.lastIndex)
        assertEquals(C.INDEX_UNSET, order.getNextIndex(0))
        assertEquals(C.INDEX_UNSET, order.getPreviousIndex(0))
    }

    @Test
    fun testShuffleCoversAllIndices() {
        val length = 50
        val order = BetterShuffleOrder(length, 12345L)
        assertEquals(length, order.length)

        val visited = mutableListOf<Int>()
        var current = order.firstIndex
        assertNotEquals(C.INDEX_UNSET, current)

        while (current != C.INDEX_UNSET) {
            visited.add(current)
            current = order.getNextIndex(current)
        }

        // Must visit every element exactly once
        assertEquals(length, visited.size)
        assertEquals(length, visited.toSet().size)
        assertTrue(visited.containsAll((0 until length).toList()))
        assertEquals(order.lastIndex, visited.last())
    }

    @Test
    fun testReverseTraversalMatchesForward() {
        val length = 20
        val order = BetterShuffleOrder(length, 42L)

        val forward = mutableListOf<Int>()
        var current = order.firstIndex
        while (current != C.INDEX_UNSET) {
            forward.add(current)
            current = order.getNextIndex(current)
        }

        val backward = mutableListOf<Int>()
        current = order.lastIndex
        while (current != C.INDEX_UNSET) {
            backward.add(current)
            current = order.getPreviousIndex(current)
        }

        assertEquals(forward.reversed(), backward)
    }

    @Test
    fun testCloneAndInsert() {
        val initialLength = 10
        val order = BetterShuffleOrder(initialLength, 99L)
        val newOrder = order.cloneAndInsert(5, 3)

        assertEquals(13, newOrder.length)

        val visited = mutableListOf<Int>()
        var current = newOrder.firstIndex
        while (current != C.INDEX_UNSET) {
            visited.add(current)
            current = newOrder.getNextIndex(current)
        }

        assertEquals(13, visited.size)
        assertEquals(13, visited.toSet().size)
        assertTrue(visited.containsAll((0 until 13).toList()))
    }

    @Test
    fun testCloneAndRemove() {
        val initialLength = 15
        val order = BetterShuffleOrder(initialLength, 777L)
        val newOrder = order.cloneAndRemove(3, 7) // removes 4 items (3, 4, 5, 6)

        assertEquals(11, newOrder.length)

        val visited = mutableListOf<Int>()
        var current = newOrder.firstIndex
        while (current != C.INDEX_UNSET) {
            visited.add(current)
            current = newOrder.getNextIndex(current)
        }

        assertEquals(11, visited.size)
        assertEquals(11, visited.toSet().size)
        assertTrue(visited.containsAll((0 until 11).toList()))
    }
}
