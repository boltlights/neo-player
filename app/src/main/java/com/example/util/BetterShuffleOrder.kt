package com.example.util

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import java.util.Random

@UnstableApi
class BetterShuffleOrder : ShuffleOrder {

    private val random: Random
    private val shuffled: IntArray
    private val inverted: IntArray

    constructor(length: Int, randomSeed: Long = Random().nextLong()) {
        random = Random(randomSeed)
        shuffled = createShuffledList(length, random)
        inverted = IntArray(length)
        for (i in 0 until length) {
            inverted[shuffled[i]] = i
        }
    }

    private constructor(random: Random, shuffled: IntArray, inverted: IntArray) {
        this.random = random
        this.shuffled = shuffled
        this.inverted = inverted
    }

    override fun getLength(): Int = shuffled.size

    override fun getNextIndex(index: Int): Int {
        if (index !in inverted.indices) return C.INDEX_UNSET
        var shuffledIndex = inverted[index]
        return if (++shuffledIndex < shuffled.size) shuffled[shuffledIndex] else C.INDEX_UNSET
    }

    override fun getPreviousIndex(index: Int): Int {
        if (index !in inverted.indices) return C.INDEX_UNSET
        var shuffledIndex = inverted[index]
        return if (--shuffledIndex >= 0) shuffled[shuffledIndex] else C.INDEX_UNSET
    }

    override fun getLastIndex(): Int = if (shuffled.isNotEmpty()) shuffled.last() else C.INDEX_UNSET

    override fun getFirstIndex(): Int = if (shuffled.isNotEmpty()) shuffled.first() else C.INDEX_UNSET

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        val newShuffled = IntArray(shuffled.size + insertionCount)
        val newInverted = IntArray(shuffled.size + insertionCount)

        val anchorIndex = insertionIndex - 1
        val insertShuffledPos = if (anchorIndex >= 0 && anchorIndex < inverted.size) {
            inverted[anchorIndex] + 1
        } else {
            if (shuffled.isEmpty()) 0 else random.nextInt(shuffled.size + 1)
        }

        var oldPos = 0
        var newPos = 0
        while (oldPos < shuffled.size || newPos < newShuffled.size) {
            if (newPos >= insertShuffledPos && newPos < insertShuffledPos + insertionCount) {
                newShuffled[newPos] = insertionIndex + (newPos - insertShuffledPos)
                newPos++
            } else {
                var oldItem = shuffled[oldPos++]
                if (oldItem >= insertionIndex) {
                    oldItem += insertionCount
                }
                newShuffled[newPos] = oldItem
                newPos++
            }
        }

        for (i in newShuffled.indices) {
            newInverted[newShuffled[i]] = i
        }

        return BetterShuffleOrder(random, newShuffled, newInverted)
    }

    override fun cloneAndRemove(indexFrom: Int, indexTo: Int): ShuffleOrder {
        val removalCount = indexTo - indexFrom
        val newShuffled = IntArray(shuffled.size - removalCount)
        val newInverted = IntArray(shuffled.size - removalCount)

        var newPos = 0
        for (i in shuffled.indices) {
            var item = shuffled[i]
            if (item in indexFrom until indexTo) {
                continue
            }
            if (item >= indexFrom) {
                item -= removalCount
            }
            newShuffled[newPos++] = item
        }

        for (i in newShuffled.indices) {
            newInverted[newShuffled[i]] = i
        }

        return BetterShuffleOrder(random, newShuffled, newInverted)
    }

    override fun cloneAndClear(): ShuffleOrder {
        return BetterShuffleOrder(0, random.nextLong())
    }

    companion object {
        private fun createShuffledList(length: Int, random: Random): IntArray {
            val list = IntArray(length)
            for (i in 0 until length) {
                list[i] = i
            }
            for (i in length - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val temp = list[i]
                list[i] = list[j]
                list[j] = temp
            }
            return list
        }
    }
}
