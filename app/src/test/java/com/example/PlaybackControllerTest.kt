package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TrackEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackControllerTest {

    private lateinit var controller: PlaybackController

    private fun createTrack(id: Int, title: String): TrackEntity {
        return TrackEntity(
            id = id,
            title = title,
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 200000L,
            filePath = "/sdcard/Music/$title.mp3",
            artPath = null
        )
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        controller = PlaybackController(context)
    }

    @After
    fun tearDown() {
        controller.stopAndClear()
    }

    // =========================================================================
    // 2. Queue Operations
    // =========================================================================

    @Test
    fun setPlaybackList_updatesQueueAndBaseList() {
        val tracks = listOf(
            createTrack(1, "Track 1"),
            createTrack(2, "Track 2"),
            createTrack(3, "Track 3")
        )

        controller.setPlaybackList(tracks, tracks[0])

        assertEquals(3, controller.currentPlaybackList.value.size)
        assertEquals("Track 1", controller.currentPlaybackList.value[0].title)
        assertEquals("Track 2", controller.currentPlaybackList.value[1].title)
        assertEquals("Track 3", controller.currentPlaybackList.value[2].title)
        assertEquals(3, controller.getBasePlaybackList().size)
    }

    @Test
    fun playTrackNext_whenNothingIsPlaying_playsTrackImmediately() {
        val track = createTrack(10, "Swiped Solo Track")
        controller.playTrackNext(track)

        assertEquals(1, controller.currentPlaybackList.value.size)
        assertEquals(10, controller.currentPlaybackList.value[0].id)
    }

    @Test
    fun playTrackNext_whenAudioPlaying_insertsImmediatelyAfterCurrentTrack() {
        val tracks = listOf(
            createTrack(1, "Track 1"),
            createTrack(2, "Track 2"),
            createTrack(3, "Track 3")
        )
        controller.setPlaybackList(tracks, tracks[0])

        val swiped1 = createTrack(100, "Swiped 1")
        controller.playTrackNext(swiped1)

        val queueAfterFirstSwipe = controller.currentPlaybackList.value
        assertEquals(4, queueAfterFirstSwipe.size)
        assertEquals(1, queueAfterFirstSwipe[0].id)
        assertEquals(100, queueAfterFirstSwipe[1].id) // Placed right after currently playing track (id=1)
        assertEquals(2, queueAfterFirstSwipe[2].id)
        assertEquals(3, queueAfterFirstSwipe[3].id)

        // Second swipe: swiping B after A maintains FIFO order (plays in order A, then B)
        val swiped2 = createTrack(101, "Swiped 2")
        controller.playTrackNext(swiped2)

        val queueAfterSecondSwipe = controller.currentPlaybackList.value
        assertEquals(5, queueAfterSecondSwipe.size)
        assertEquals(1, queueAfterSecondSwipe[0].id)
        assertEquals(100, queueAfterSecondSwipe[1].id) // First swiped (A) plays first
        assertEquals(101, queueAfterSecondSwipe[2].id) // Second swiped (B) plays after A
        assertEquals(2, queueAfterSecondSwipe[3].id)
        assertEquals(3, queueAfterSecondSwipe[4].id)

        // Third swipe: swiping C after B queues in order: A, then B, then C
        val swiped3 = createTrack(102, "Swiped 3")
        controller.playTrackNext(swiped3)

        val queueAfterThirdSwipe = controller.currentPlaybackList.value
        assertEquals(6, queueAfterThirdSwipe.size)
        assertEquals(1, queueAfterThirdSwipe[0].id)
        assertEquals(100, queueAfterThirdSwipe[1].id) // A
        assertEquals(101, queueAfterThirdSwipe[2].id) // B
        assertEquals(102, queueAfterThirdSwipe[3].id) // C
        assertEquals(2, queueAfterThirdSwipe[4].id)
        assertEquals(3, queueAfterThirdSwipe[5].id)
    }

    @Test
    fun addToQueue_appendsToEndOfList() {
        val tracks = listOf(
            createTrack(1, "Track 1"),
            createTrack(2, "Track 2")
        )
        controller.setPlaybackList(tracks, tracks[0])

        val newTrack = createTrack(50, "Enqueued Track")
        controller.addToQueue(newTrack)

        val list = controller.currentPlaybackList.value
        assertEquals(3, list.size)
        assertEquals(50, list.last().id)
    }

    @Test
    fun moveQueueItem_reordersItemsCorrectly() {
        val tracks = listOf(
            createTrack(1, "A"),
            createTrack(2, "B"),
            createTrack(3, "C")
        )
        controller.setPlaybackList(tracks, tracks[0])

        // Move "A" (index 0) to index 2
        controller.moveQueueItem(0, 2)

        val reordered = controller.currentPlaybackList.value
        assertEquals("B", reordered[0].title)
        assertEquals("C", reordered[1].title)
        assertEquals("A", reordered[2].title)
    }

    @Test
    fun removeTrackFromQueue_removesSpecifiedItem() {
        val tracks = listOf(
            createTrack(1, "Track 1"),
            createTrack(2, "Track 2"),
            createTrack(3, "Track 3")
        )
        controller.setPlaybackList(tracks, tracks[0])

        controller.removeTrackFromQueue(2)

        val remaining = controller.currentPlaybackList.value
        assertEquals(2, remaining.size)
        assertFalse(remaining.any { it.id == 2 })
        assertEquals(1, remaining[0].id)
        assertEquals(3, remaining[1].id)
    }

    @Test
    fun clearUpcomingQueue_clearsAllUpcomingTracks() {
        val tracks = listOf(
            createTrack(1, "Track 1"),
            createTrack(2, "Track 2"),
            createTrack(3, "Track 3")
        )
        controller.setPlaybackList(tracks, tracks[0])

        controller.clearUpcomingQueue()

        val list = controller.currentPlaybackList.value
        // Current track remains, upcoming tracks removed
        assertEquals(1, list.size)
        assertEquals(1, list[0].id)
    }

    @Test
    fun stopAndClear_resetsAllPlaybackAndQueueStates() {
        val tracks = listOf(createTrack(1, "Track 1"))
        controller.setPlaybackList(tracks, tracks[0])
        assertFalse(controller.currentPlaybackList.value.isEmpty())

        controller.stopAndClear()

        assertTrue(controller.currentPlaybackList.value.isEmpty())
        assertTrue(controller.getBasePlaybackList().isEmpty())
        assertNull(controller.currentTrack.value)
        assertFalse(controller.isPlaying.value)
    }

    // =========================================================================
    // 3. Shuffle Toggle State Machine
    // =========================================================================

    @Test
    fun toggleShuffle_stateMachineTransitions() {
        val tracks = (1..10).map { createTrack(it, "Track $it") }
        controller.setPlaybackList(tracks, tracks[0])

        // Initial state
        assertFalse(controller.isShuffle.value)
        assertFalse(controller.replayGainProcessor.isShuffleActive)

        // Toggle ON
        controller.toggleShuffle()
        assertTrue(controller.isShuffle.value)
        assertTrue(controller.replayGainProcessor.isShuffleActive)
        assertEquals(10, controller.currentPlaybackList.value.size)

        // Base playlist is preserved
        assertEquals(10, controller.getBasePlaybackList().size)
        assertEquals("Track 1", controller.getBasePlaybackList()[0].title)

        // Toggle OFF (restores original base playlist ordering)
        controller.toggleShuffle()
        assertFalse(controller.isShuffle.value)
        assertFalse(controller.replayGainProcessor.isShuffleActive)

        val restoredList = controller.currentPlaybackList.value
        assertEquals(10, restoredList.size)
        for (i in 0 until 10) {
            assertEquals(tracks[i].id, restoredList[i].id)
        }
    }

    @Test
    fun playShuffledList_initiatesShuffleState() {
        val tracks = (1..8).map { createTrack(it, "Track $it") }

        controller.playShuffledList(tracks)

        assertTrue(controller.isShuffle.value)
        assertTrue(controller.replayGainProcessor.isShuffleActive)
        assertEquals(8, controller.currentPlaybackList.value.size)
        assertEquals(8, controller.getBasePlaybackList().size)
    }
}
