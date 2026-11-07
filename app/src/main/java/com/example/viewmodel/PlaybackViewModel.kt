package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.example.PlaybackController
import com.example.Screen
import com.example.data.MusicRepository
import com.example.data.PlaybackStateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Domain-specific ViewModel handling playback state, queue persistence, overlay modes, and dynamic palette.
 */
class PlaybackViewModel(
    private val application: Application,
    private val repository: MusicRepository,
    val playbackController: PlaybackController,
    private val scope: CoroutineScope
) {
    private val _currentScreen = MutableStateFlow(Screen.LIBRARY)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _currentBgColor = MutableStateFlow(Color.Black)
    val currentBgColor: StateFlow<Color> = _currentBgColor.asStateFlow()

    private var isRestoreCompleted = false
    private var lastSavedQueueHash = 0
    private var lastSavedQueueStr = ""

    init {
        scope.launch {
            restoreQueueState()
            observeAndSaveQueue()
        }
    }

    fun selectScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    fun updatePalette(bitmap: Bitmap?) {
        if (bitmap == null) {
            _currentBgColor.value = Color.Black
            return
        }
        scope.launch(Dispatchers.Default) {
            try {
                val palette = Palette.from(bitmap).generate()
                val color = palette.getDarkMutedColor(0xFF000000.toInt())
                _currentBgColor.value = Color(color)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _currentBgColor.value = Color.Black
            }
        }
    }

    fun saveCurrentPlaybackState() {
        if (!isRestoreCompleted) return
        val queue = playbackController.currentPlaybackList.value
        val baseQueue = playbackController.getBasePlaybackList()
        val track = playbackController.currentTrack.value
        // Persist by stable UID: row IDs can change between sessions (rescans), UIDs don't.
        val currentTrackUid = track?.uid?.takeIf { it.isNotEmpty() }
        val pos = playbackController.currentPosition.value
        val isShuffle = playbackController.isShuffle.value
        val isRepeat = playbackController.isRepeat.value

        val queueHash = queue.hashCode()
        if (queueHash != lastSavedQueueHash) {
            lastSavedQueueStr = queue.joinToString(",") { it.uid }
            val baseQueueStr = baseQueue.joinToString(",") { it.uid }
            lastSavedQueueHash = queueHash
            scope.launch {
                repository.savePlaybackState(
                    PlaybackStateEntity(
                        id = 0,
                        baseQueueTrackUids = baseQueueStr,
                        queueTrackUids = lastSavedQueueStr,
                        currentTrackUid = currentTrackUid,
                        currentPositionMs = pos,
                        isShuffleEnabled = isShuffle,
                        isRepeatEnabled = isRepeat
                    )
                )
            }
        } else {
            scope.launch {
                repository.updatePlaybackPosition(pos, currentTrackUid, isShuffle)
            }
        }
    }

    private fun observeAndSaveQueue() {
        scope.launch {
            playbackController.onDiscontinuity.collect {
                saveCurrentPlaybackState()
            }
        }
        // Persist the exact position when playback pauses/stops; the periodic save below only runs while
        // playing, so pausing and then having the process killed lost up to 5 seconds plus queue edits.
        scope.launch {
            playbackController.isPlaying.collect { playing ->
                if (!playing) saveCurrentPlaybackState()
            }
        }
        scope.launch {
            while (isActive) {
                delay(5000L)
                if (isRestoreCompleted && playbackController.isPlaying.value) {
                    saveCurrentPlaybackState()
                }
            }
        }
    }

    private suspend fun restoreQueueState() {
        // The PlaybackController (and its player) is app-scoped and outlives this ViewModel. If the user
        // backed out of the activity while music kept playing and then reopened the app, restoring here
        // replaced the live queue with the last saved snapshot and jumped playback back a few seconds.
        if (playbackController.currentPlaybackList.value.isNotEmpty()) {
            isRestoreCompleted = true
            return
        }
        val playbackState = repository.getPlaybackState()
        if (playbackState != null) {
            val savedPos = playbackState.currentPositionMs
            val isShuffleEnabled = playbackState.isShuffleEnabled
            val isRepeatEnabled = playbackState.isRepeatEnabled
            val uids = playbackState.queueTrackUids.split(",").filter { it.isNotBlank() }
            val baseUids = playbackState.baseQueueTrackUids.split(",").filter { it.isNotBlank() }

            if (uids.isNotEmpty()) {
                // Identical copies share a UID; resolve each to the lowest row ID, matching the DAO.
                val trackByUid = repository.getTracksByUids(uids + baseUids)
                    .groupBy { it.uid }
                    .mapValues { (_, copies) -> copies.minBy { it.id } }

                val trackList = uids.mapNotNull { trackByUid[it] }
                val baseTrackList = if (baseUids.isNotEmpty()) baseUids.mapNotNull { trackByUid[it] } else trackList

                if (trackList.isNotEmpty()) {
                    playbackController.restorePlaybackListAndTrack(
                        list = trackList,
                        baseList = baseTrackList,
                        activeTrack = trackList.firstOrNull { it.uid == playbackState.currentTrackUid },
                        positionMs = savedPos,
                        isShuffleEnabled = isShuffleEnabled,
                        isRepeatEnabled = isRepeatEnabled
                    )
                }
            }
        }
        isRestoreCompleted = true
    }
}
