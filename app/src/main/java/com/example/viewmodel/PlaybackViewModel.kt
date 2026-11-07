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
        val currentTrackId = track?.id ?: -1
        val pos = playbackController.currentPosition.value
        val isShuffle = playbackController.isShuffle.value
        val isRepeat = playbackController.isRepeat.value

        val queueHash = queue.hashCode()
        if (queueHash != lastSavedQueueHash) {
            lastSavedQueueStr = queue.joinToString(",") { it.id.toString() }
            val baseQueueStr = baseQueue.joinToString(",") { it.id.toString() }
            lastSavedQueueHash = queueHash
            scope.launch {
                repository.savePlaybackState(
                    PlaybackStateEntity(
                        id = 0,
                        baseQueueTrackIds = baseQueueStr,
                        queueTrackIds = lastSavedQueueStr,
                        currentTrackId = currentTrackId,
                        currentPositionMs = pos,
                        isShuffleEnabled = isShuffle,
                        isRepeatEnabled = isRepeat
                    )
                )
            }
        } else {
            scope.launch {
                repository.updatePlaybackPosition(pos, currentTrackId, isShuffle)
            }
        }
    }

    private fun observeAndSaveQueue() {
        scope.launch {
            playbackController.onDiscontinuity.collect {
                saveCurrentPlaybackState()
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
        val playbackState = repository.getPlaybackState()
        if (playbackState != null) {
            val baseQueueIdsStr = playbackState.baseQueueTrackIds
            val queueIdsStr = playbackState.queueTrackIds
            val currentTrackId = playbackState.currentTrackId
            val savedPos = playbackState.currentPositionMs
            val isShuffleEnabled = playbackState.isShuffleEnabled
            val isRepeatEnabled = playbackState.isRepeatEnabled

            if (queueIdsStr.isNotEmpty()) {
                val baseIds = if (baseQueueIdsStr.isNotEmpty()) baseQueueIdsStr.split(",").mapNotNull { it.toIntOrNull() } else emptyList()
                val ids = queueIdsStr.split(",").mapNotNull { it.toIntOrNull() }

                if (ids.isNotEmpty()) {
                    val allIds = (ids + baseIds).distinct()
                    val trackMap = repository.getTracksByIds(allIds).associateBy { it.id }

                    val trackList = ids.mapNotNull { trackMap[it] }.toList()
                    val baseTrackList = if (baseIds.isNotEmpty()) baseIds.mapNotNull { trackMap[it] }.toList() else trackList

                    if (trackList.isNotEmpty()) {
                        playbackController.restorePlaybackListAndTrack(
                            list = trackList,
                            baseList = baseTrackList,
                            activeTrack = trackList.firstOrNull { it.id == currentTrackId },
                            positionMs = savedPos,
                            isShuffleEnabled = isShuffleEnabled,
                            isRepeatEnabled = isRepeatEnabled
                        )
                    }
                }
            }
        }
        isRestoreCompleted = true
    }
}
