package com.example

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.data.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class PlaybackProgress(
    val position: Int,
    val duration: Int,
    val isBuffering: Boolean
)

class PlaybackController(private val context: Context) {
    private val queueMutex = kotlinx.coroutines.sync.Mutex()
    val replayGainProcessor = ReplayGainAudioProcessor()
    
    var exoPlayer: ExoPlayer? = null
        private set

    private var loggedForCurrentTrack = false
    private var consecutiveErrors = 0
    private var currentTrackIdForLogging: Int? = null
    private var currentAudioSessionId: Int = 0
    private var lastMediaItemIndex: Int = -1
    private val enqueuedNextTrackIds = mutableListOf<Int>()
    
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()
    
    var sleepTimerEndOfTrack = false
        private set
    private var sleepTimerJob: Job? = null

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var basePlaybackList: List<TrackEntity> = emptyList()
    fun getBasePlaybackList(): List<TrackEntity> = basePlaybackList
    private val _currentPlaybackList = MutableStateFlow<List<TrackEntity>>(emptyList())
    val currentPlaybackList: StateFlow<List<TrackEntity>> = _currentPlaybackList.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val playScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var trackerJob: Job? = null
    private var fadeJob: Job? = null

    val playbackProgress: StateFlow<PlaybackProgress> = combine(
        _currentPosition,
        _duration,
        _isBuffering
    ) { pos, dur, buffering ->
        PlaybackProgress(pos, dur, buffering)
    }.stateIn(
        scope = playScope,
        started = SharingStarted.Lazily,
        initialValue = PlaybackProgress(0, 0, false)
    )

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _onDiscontinuity = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val onDiscontinuity = _onDiscontinuity.asSharedFlow()

    var crossfadeEnabled: Boolean = false
    var crossfadeDurationSeconds: Int = 3
    var gaplessEnabled: Boolean = true

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        ensurePlayer()
        val list = _currentPlaybackList.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _currentPlaybackList.value = list
            exoPlayer?.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun smoothPlay(durationOverrideMs: Long? = null) {
        val player = exoPlayer ?: return

        // After a playback error (or stop) the player sits in STATE_IDLE and play() alone does nothing.
        // Previously only togglePlayPause re-prepared, so tapping a queued song or next/prev was silent.
        if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
            player.prepare()
        }

        fadeJob?.cancel()
        player.volume = 0f
        player.play()
        val durationMs = durationOverrideMs ?: if (crossfadeEnabled) (crossfadeDurationSeconds * 1000L).coerceIn(500L, 8000L) else 300L
        fadeJob = playScope.launch {
            val steps = 20
            val delayMs = (durationMs / steps).coerceAtLeast(10L)
            for (i in 1..steps) {
                if (!isActive) break
                val currentVolume = (1.0f / steps) * i
                player.volume = currentVolume.coerceIn(0f, 1f)
                delay(delayMs)
            }
            player.volume = 1.0f
        }
    }

    fun smoothPause(durationOverrideMs: Long? = null) {
        val player = exoPlayer ?: return
        if (!player.isPlaying) return
        fadeJob?.cancel()
        val startVolume = player.volume
        val durationMs = durationOverrideMs ?: if (crossfadeEnabled) (crossfadeDurationSeconds * 1000L).coerceIn(500L, 8000L) else 300L
        fadeJob = playScope.launch {
            val steps = 20
            val delayMs = (durationMs / steps).coerceAtLeast(10L)
            for (i in 1..steps) {
                if (!isActive) break
                val currentVolume = startVolume - (startVolume / steps) * i
                player.volume = currentVolume.coerceIn(0f, 1f)
                delay(delayMs)
            }
            player.volume = 0f
            player.pause()
            player.volume = 1.0f
        }
    }

    init {
        startPositionTracker()
    }
    
    fun ensurePlayer() {
        if (exoPlayer == null) {
            initPlayer()
        }
    }

    private var isReceiverRegistered = false
    private val audioBecomingNoisyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                smoothPause()
            }
        }
    }

    private fun registerNoisyReceiver() {
        if (!isReceiverRegistered) {
            val filter = android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(audioBecomingNoisyReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(audioBecomingNoisyReceiver)
            } catch (e: Exception) {
                // ignore
            }
            isReceiverRegistered = false
        }
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            registerNoisyReceiver()
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink? {
                    return DefaultAudioSink.Builder(context)
                        .setAudioProcessors(arrayOf(replayGainProcessor))
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .build()
                }
            }

            exoPlayer = ExoPlayer.Builder(context, renderersFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                setWakeMode(C.WAKE_MODE_LOCAL)
                
                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        super.onAudioSessionIdChanged(audioSessionId)
                        if (currentAudioSessionId != 0 && currentAudioSessionId != audioSessionId) {
                            try {
                                val closeIntent = Intent(android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, currentAudioSessionId)
                                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                }
                                context.sendBroadcast(closeIntent)
                            } catch (e: Exception) {
                                timber.log.Timber.tag("PlaybackController").e(e, "Failed to broadcast close eq session")
                            }
                        }
                        
                        currentAudioSessionId = audioSessionId
                        
                        try {
                            val openIntent = Intent(android.media.audiofx.AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                                putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                                putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            }
                            context.sendBroadcast(openIntent)
                        } catch (e: Exception) {
                            timber.log.Timber.tag("PlaybackController").e(e, "Failed to broadcast open eq session")
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        com.example.widget.MusicWidgetProvider.updateAllWidgets(context)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        _currentPosition.value = 0
                        _duration.value = 0
                        val newIndex = this@apply.currentMediaItemIndex
                        lastMediaItemIndex = newIndex
                        updateCurrentTrackState(mediaItem)
                        if (sleepTimerEndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                            sleepTimerEndOfTrack = false
                            this@apply.pauseAtEndOfMediaItems = false
                            this@PlaybackController.smoothPause()
                            _sleepTimerRemainingSeconds.value = null
                        }
                        com.example.widget.MusicWidgetProvider.updateAllWidgets(context)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _isBuffering.value = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) {
                            consecutiveErrors = 0
                            val dur = this@apply.duration.toInt()
                            _duration.value = if (dur > 0) dur else 0
                        }
                        if (sleepTimerEndOfTrack && playbackState == Player.STATE_ENDED) {
                            sleepTimerEndOfTrack = false
                            this@apply.pauseAtEndOfMediaItems = false
                            this@PlaybackController.smoothPause()
                            _sleepTimerRemainingSeconds.value = null
                        }
                    }



                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _isRepeat.value = repeatMode == Player.REPEAT_MODE_ALL || repeatMode == Player.REPEAT_MODE_ONE
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                        _onDiscontinuity.tryEmit(Unit)
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        super.onPlayerError(error)
                        timber.log.Timber.tag("PlaybackController").e(error, "Player error")
                        
                        consecutiveErrors++
                        if (consecutiveErrors < 5 && this@apply.hasNextMediaItem()) {
                            try {
                                android.widget.Toast.makeText(context, context.getString(R.string.playback_error_skipping), android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {}
                            
                            this@apply.seekToNextMediaItem()
                            this@apply.prepare()
                            this@apply.play()
                        } else {
                            _isPlaying.value = false
                            try {
                                val msg = error.message ?: ""
                                android.widget.Toast.makeText(context, context.getString(R.string.playback_error_format, msg), android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {}
                        }
                    }

                    override fun onMetadata(metadata: androidx.media3.common.Metadata) {
                        super.onMetadata(metadata)
                        val trackGain = com.example.util.AudioQualityUtil.getReplayGainTrackGainFromMetadata(metadata)
                        val albumGain = com.example.util.AudioQualityUtil.getReplayGainAlbumGainFromMetadata(metadata)
                        val trackPeak = com.example.util.AudioQualityUtil.getReplayGainTrackPeakFromMetadata(metadata)
                        val albumPeak = com.example.util.AudioQualityUtil.getReplayGainAlbumPeakFromMetadata(metadata)

                        replayGainProcessor.setGains(trackGain, albumGain, trackPeak, albumPeak)
                        timber.log.Timber.tag("PlaybackController").d(
                            "ReplayGain metadata: trackGain=%s, albumGain=%s, trackPeak=%s, albumPeak=%s",
                            trackGain, albumGain, trackPeak, albumPeak
                        )
                    }
                })
            }

            try {
                val serviceIntent = Intent(context, PlaybackService::class.java)
                context.startService(serviceIntent)
            } catch (e: Exception) {
                timber.log.Timber.tag("PlaybackController").e(e, "Failed to start PlaybackService")
            }
        }
    }

    private fun updateCurrentTrackState(mediaItem: MediaItem?) {
        replayGainProcessor.clearGain()
        val idStr = mediaItem?.mediaId
        if (idStr != null) {
            val id = idStr.toIntOrNull()
            if (id != null && id != currentTrackIdForLogging) {
                currentTrackIdForLogging = id
                loggedForCurrentTrack = false
            }
            val track = _currentPlaybackList.value.find { it.id == id }
            _currentTrack.value = track
            val dur = exoPlayer?.duration?.toInt() ?: 0
            _duration.value = if (dur > 0) dur else 0

            // Apply direct bit-perfect playback if a USB DAC is connected on Android 14+
            track?.let {
                com.example.util.AudioQualityUtil.configureBitPerfectIfSupported(context, it)
            }
        } else {
            _currentTrack.value = null
            currentTrackIdForLogging = null
            loggedForCurrentTrack = false
        }
    }

    private fun createMediaItem(track: TrackEntity): MediaItem {
        val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setIsPlayable(true)
            .also { builder ->
                track.artPath?.let { art ->
                    try {
                        if (art.startsWith("audiocover://") || art.startsWith("content://")) {
                            builder.setArtworkUri(android.net.Uri.parse(art))
                        } else {
                            val file = java.io.File(art)
                            builder.setArtworkUri(android.net.Uri.fromFile(file))
                        }
                    } catch (e: Exception) {
                        // ignore malformed URI
                    }
                }
            }
            .build()

        val bestUri = when {
            track.filePath.isNotBlank() && java.io.File(track.filePath).canRead() -> {
                android.net.Uri.fromFile(java.io.File(track.filePath)).toString()
            }
            !track.sourceUri.isNullOrBlank() -> {
                track.sourceUri
            }
            else -> {
                track.filePath
            }
        }

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(bestUri)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    private fun getMediaItemsFromList(list: List<TrackEntity>): List<MediaItem> {
        return list.map { createMediaItem(it) }
    }

    private fun checkIfAlbumPlayback(list: List<TrackEntity>): Boolean {
        if (list.size <= 1) return false
        val firstAlbum = list.firstOrNull()?.album?.trim()
        if (firstAlbum.isNullOrBlank() || firstAlbum.equals("Unknown", ignoreCase = true)) return false
        return list.all { it.album.trim().equals(firstAlbum, ignoreCase = true) }
    }

    fun playShuffledList(list: List<TrackEntity>) {
        if (list.isEmpty()) return
        basePlaybackList = list
        _isShuffle.value = true
        replayGainProcessor.isShuffleActive = true
        val randomFirstTrack = list.random()
        val shuffled = fairShuffle(list, randomFirstTrack)
        setPlaybackList(shuffled, randomFirstTrack, isExplicitShuffle = true)
    }

    fun setPlaybackList(list: List<TrackEntity>, startTrack: TrackEntity? = null, isExplicitShuffle: Boolean = false) {
        enqueuedNextTrackIds.clear()
        lastMediaItemIndex = -1
        if (!isExplicitShuffle) {
            basePlaybackList = list
            _isShuffle.value = false
        }
        ensurePlayer()
        _currentPlaybackList.value = list
        replayGainProcessor.isAlbumPlayback = checkIfAlbumPlayback(list)
        replayGainProcessor.isShuffleActive = _isShuffle.value
        try {
            exoPlayer?.apply {
                setMediaItems(getMediaItemsFromList(list))
                prepare()
                if (startTrack != null) {
                    val index = list.indexOfFirst { it.id == startTrack.id }
                    if (index != -1) {
                        seekTo(index, 0L)
                    }
                } else if (list.isNotEmpty()) {
                    seekTo(0, 0L)
                }
                this@PlaybackController.smoothPlay()
            }
        } catch (e: Exception) {
            timber.log.Timber.tag("PlaybackController").e(e, "Error setting playback list")
            val msg = e.message ?: ""
            android.widget.Toast.makeText(context, context.getString(R.string.error_playing_track, msg), android.widget.Toast.LENGTH_LONG).show()
        }
    }


    fun addToQueue(tracks: List<TrackEntity>) {
        ensurePlayer()
        if (tracks.isEmpty()) return
        val currentList = _currentPlaybackList.value.toMutableList()
        currentList.addAll(tracks)
        _currentPlaybackList.value = currentList
        
        val baseList = basePlaybackList.toMutableList()
        baseList.addAll(tracks)
        basePlaybackList = baseList
        
        val mediaItems = tracks.map { createMediaItem(it) }
        exoPlayer?.addMediaItems(mediaItems)
        
        if (_currentTrack.value == null) {
            exoPlayer?.prepare()
            smoothPlay()
        }
    }

    fun addToQueue(track: TrackEntity) {
        ensurePlayer()
        val currentList = _currentPlaybackList.value.toMutableList()
        currentList.add(track)
        _currentPlaybackList.value = currentList
        
        val baseList = basePlaybackList.toMutableList()
        baseList.add(track)
        basePlaybackList = baseList
        
        val mediaItem = createMediaItem(track)
            
        exoPlayer?.addMediaItem(mediaItem)
        
        if (_currentTrack.value == null) {
            exoPlayer?.prepare()
            smoothPlay()
        }
    }


    fun playTracksNext(tracks: List<TrackEntity>) {
        ensurePlayer()
        if (tracks.isEmpty()) return
        val current = _currentTrack.value
        val currentList = _currentPlaybackList.value.toMutableList()
        val baseList = basePlaybackList.toMutableList()
        
        if (current == null) {
            for (track in tracks) {
                if (!currentList.contains(track)) {
                    currentList.add(track)
                    if (!baseList.contains(track)) baseList.add(track)
                }
            }
            _currentPlaybackList.value = currentList
            basePlaybackList = baseList
            
            exoPlayer?.setMediaItems(getMediaItemsFromList(currentList))
            exoPlayer?.prepare()
            smoothPlay()
        } else {
            val filteredTracks = mutableListOf<TrackEntity>()
            for (track in tracks) {
                if (track.id == current.id) continue
                filteredTracks.add(track)
            }
            
            for (track in filteredTracks) {
                val oldIndex = currentList.indexOfFirst { it.id == track.id }
                if (oldIndex != -1) {
                    exoPlayer?.removeMediaItem(oldIndex)
                    currentList.removeAt(oldIndex)
                }
                
                val oldBase = baseList.indexOfFirst { it.id == track.id }
                if (oldBase != -1) baseList.removeAt(oldBase)
            }
            
            val currentIndex = exoPlayer?.currentMediaItemIndex ?: -1
            val mediaItemsToAdd = filteredTracks.map { createMediaItem(it) }
            
            if (currentIndex != -1) {
                currentList.addAll(currentIndex + 1, filteredTracks)
                exoPlayer?.addMediaItems(currentIndex + 1, mediaItemsToAdd)
            } else {
                currentList.addAll(0, filteredTracks)
                exoPlayer?.addMediaItems(0, mediaItemsToAdd)
            }
            _currentPlaybackList.value = currentList
            
            val baseIndex = baseList.indexOfFirst { it.id == current.id }
            if (baseIndex != -1) {
                baseList.addAll(baseIndex + 1, filteredTracks)
            } else {
                baseList.addAll(filteredTracks)
            }
            basePlaybackList = baseList
        }
    }

    fun playTrackNext(track: TrackEntity) {
        ensurePlayer()
        val current = _currentTrack.value
        val player = exoPlayer

        // If nothing is currently being played, the swiped song will play first
        if (current == null || player == null || player.mediaItemCount == 0 || player.playbackState == androidx.media3.common.Player.STATE_IDLE || !_isPlaying.value && _currentPlaybackList.value.isEmpty()) {
            val currentList = _currentPlaybackList.value
            if (currentList.isNotEmpty()) {
                val updatedList = currentList.toMutableList()
                val oldIdx = updatedList.indexOfFirst { it.id == track.id }
                if (oldIdx != -1) {
                    updatedList.removeAt(oldIdx)
                }
                updatedList.add(0, track)
                setPlaybackList(updatedList, track)
            } else {
                setPlaybackList(listOf(track), track)
            }
            return
        }

        // If swiping the currently playing track itself, it is already playing
        if (current.id == track.id) {
            return
        }

        val currentList = _currentPlaybackList.value.toMutableList()
        val baseList = basePlaybackList.toMutableList()
        val mediaItem = createMediaItem(track)

        val currentIndex = player.currentMediaItemIndex
        if (currentIndex == -1) {
            setPlaybackList(listOf(track), track)
            return
        }

        val oldIndex = currentList.indexOfFirst { it.id == track.id }
        if (oldIndex != -1) {
            player.removeMediaItem(oldIndex)
            currentList.removeAt(oldIndex)
        }

        val activeIndex = player.currentMediaItemIndex
        if (activeIndex == -1) {
            setPlaybackList(listOf(track), track)
            return
        }

        // Only retain IDs that are in the upcoming queue after activeIndex
        val upcomingIds = currentList.drop(activeIndex + 1).map { it.id }.toSet()
        enqueuedNextTrackIds.retainAll(upcomingIds)
        enqueuedNextTrackIds.remove(track.id)

        // Count contiguous tracks directly after activeIndex that were enqueued to play next
        var contiguousNextCount = 0
        for (i in (activeIndex + 1) until currentList.size) {
            if (enqueuedNextTrackIds.contains(currentList[i].id)) {
                contiguousNextCount++
            } else {
                break
            }
        }

        val targetIndex = (activeIndex + 1 + contiguousNextCount).coerceIn(activeIndex + 1, currentList.size)
        currentList.add(targetIndex, track)
        player.addMediaItem(targetIndex, mediaItem)
        _currentPlaybackList.value = currentList
        enqueuedNextTrackIds.add(track.id)

        // Synchronize with basePlaybackList
        val oldBaseIndex = baseList.indexOfFirst { it.id == track.id }
        if (oldBaseIndex != -1) {
            baseList.removeAt(oldBaseIndex)
        }
        val currentBaseIndex = baseList.indexOfFirst { it.id == current.id }
        val targetBaseIndex = if (currentBaseIndex != -1) {
            var baseContiguousCount = 0
            for (i in (currentBaseIndex + 1) until baseList.size) {
                if (enqueuedNextTrackIds.contains(baseList[i].id)) {
                    baseContiguousCount++
                } else {
                    break
                }
            }
            (currentBaseIndex + 1 + baseContiguousCount).coerceIn(currentBaseIndex + 1, baseList.size)
        } else {
            baseList.size
        }.coerceIn(0, baseList.size)
        baseList.add(targetBaseIndex, track)
        basePlaybackList = baseList
    }

    fun removeTrackFromQueue(trackId: Int) {
        ensurePlayer()
        enqueuedNextTrackIds.remove(trackId)
        val currentList = _currentPlaybackList.value
        val index = currentList.indexOfFirst { it.id == trackId }
        if (index != -1) {
            val newList = currentList.toMutableList()
            newList.removeAt(index)
            _currentPlaybackList.value = newList
            
            val baseList = basePlaybackList.toMutableList()
            val baseIndex = baseList.indexOfFirst { it.id == trackId }
            if (baseIndex != -1) {
                baseList.removeAt(baseIndex)
                basePlaybackList = baseList
            }
            
            exoPlayer?.removeMediaItem(index)
        }
    }

    fun stopAndClear() {
        enqueuedNextTrackIds.clear()
        lastMediaItemIndex = -1
        exoPlayer?.apply {
            stop()
            clearMediaItems()
        }
        basePlaybackList = emptyList()
        _currentPlaybackList.value = emptyList()
        _currentTrack.value = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _duration.value = 0
    }

    fun clearUpcomingQueue() {
        ensurePlayer()
        val current = _currentTrack.value ?: return
        val currentList = _currentPlaybackList.value
        val currentIndex = exoPlayer?.currentMediaItemIndex ?: return
        if (currentIndex < 0 || currentIndex >= currentList.size) return

        _currentPlaybackList.value = listOf(current)
        basePlaybackList = listOf(current)

        exoPlayer?.let { player ->
            val total = player.mediaItemCount
            if (total > currentIndex + 1) {
                player.removeMediaItems(currentIndex + 1, total)
            }
            if (currentIndex > 0) {
                player.removeMediaItems(0, currentIndex)
            }
        }
    }

    fun restorePlaybackListAndTrack(list: List<TrackEntity>, baseList: List<TrackEntity>, activeTrack: TrackEntity?, positionMs: Int, isShuffleEnabled: Boolean = false, isRepeatEnabled: Boolean = false) {
        ensurePlayer()
        basePlaybackList = baseList
        _currentPlaybackList.value = list
        _isShuffle.value = isShuffleEnabled
        _isRepeat.value = isRepeatEnabled
        
        try {
            exoPlayer?.apply {
                setMediaItems(getMediaItemsFromList(list))
                repeatMode = if (isRepeatEnabled) androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF
                prepare()

                if (activeTrack != null) {
                    val index = list.indexOfFirst { it.id == activeTrack.id }
                    if (index != -1) {
                        seekTo(index, positionMs.toLong())
                        // The position tracker only runs while playing, and the media-item transition
                        // above reset it to 0. Without this the restored song showed 00:00 and the save
                        // triggered by the seek overwrote the resume point with 0 on every launch.
                        _currentPosition.value = positionMs
                    }
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.tag("PlaybackController").e(e, "Failed to restore playback list and track")
        }
    }

    fun playTrack(track: TrackEntity) {
        ensurePlayer()
        try {
            val currentList = _currentPlaybackList.value
            val index = currentList.indexOfFirst { it.id == track.id }
            if (index != -1) {
                exoPlayer?.apply {
                    seekTo(index, 0L)
                }
                smoothPlay()
            } else {
                // Track not in queue? Play as single item or set as new queue
                setPlaybackList(listOf(track), track)
            }
        } catch (e: Exception) {
            timber.log.Timber.tag("PlaybackController").e(e, "Error playing track: %s", track.title)
            val msg = e.message ?: ""
            android.widget.Toast.makeText(context, context.getString(R.string.error_playing_track, msg), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun togglePlayPause() {
        ensurePlayer()
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                smoothPause()
            } else {
                // If the player is at the end of the queue, or hasn't started, and has items
                if (player.playbackState == Player.STATE_IDLE && _currentPlaybackList.value.isNotEmpty()) {
                    player.prepare()
                }
                smoothPlay()
            }
        }
    }

    fun seekTo(positionMs: Int) {
        ensurePlayer()
        exoPlayer?.seekTo(positionMs.toLong())
        _currentPosition.value = positionMs
    }

    fun playNext() {
        ensurePlayer()
        if (exoPlayer?.hasNextMediaItem() == true) {
            exoPlayer?.seekToNextMediaItem()
            smoothPlay()
        }
    }

    fun playPrevious() {
        ensurePlayer()
        if (exoPlayer?.hasPreviousMediaItem() == true) {
            exoPlayer?.seekToPreviousMediaItem()
        } else {
            exoPlayer?.seekTo(0L)
        }
        smoothPlay()
    }

    private fun fairShuffle(tracks: List<TrackEntity>, firstTrack: TrackEntity? = null): List<TrackEntity> {
        if (tracks.isEmpty()) return emptyList()
        val remaining = tracks.toMutableList()
        firstTrack?.let { remaining.remove(it) }

        if (remaining.isEmpty()) {
            return firstTrack?.let { listOf(it) } ?: emptyList()
        }

        val byArtist = remaining.groupBy { it.artist }
            .mapValues { it.value.shuffled() }

        val totalRemaining = remaining.size
        val result = arrayOfNulls<TrackEntity>(totalRemaining)

        val sortedArtists = byArtist.entries.sortedByDescending { it.value.size }

        for ((_, artistTracks) in sortedArtists) {
            val spacing = totalRemaining.toDouble() / artistTracks.size
            var offset = kotlin.random.Random.nextDouble(spacing)

            for (track in artistTracks) {
                var targetIndex = offset.toInt() % totalRemaining
                while (result[targetIndex] != null) {
                    targetIndex = (targetIndex + 1) % totalRemaining
                }
                result[targetIndex] = track
                offset += spacing
            }
        }

        val finalList = result.filterNotNull().toMutableList()
        firstTrack?.let { finalList.add(0, it) }
        return finalList
    }

    fun toggleShuffle() {
        ensurePlayer()
        val isShuffled = !_isShuffle.value
        _isShuffle.value = isShuffled
        replayGainProcessor.isShuffleActive = isShuffled
        
        exoPlayer?.let { player ->
            val currentTrack = _currentTrack.value
            val currentPositionMs = player.currentPosition
            val wasPlaying = player.isPlaying
            if (isShuffled) {
                basePlaybackList = _currentPlaybackList.value
                val shuffled = fairShuffle(basePlaybackList, currentTrack)
                _currentPlaybackList.value = shuffled
                val newIndex = if (currentTrack != null) shuffled.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0) else 0
                player.setMediaItems(getMediaItemsFromList(shuffled), newIndex, currentPositionMs)
                if (wasPlaying) player.play()
            } else {
                val restored = basePlaybackList
                if (restored.isNotEmpty()) {
                    _currentPlaybackList.value = restored
                    val newIndex = if (currentTrack != null) restored.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0) else 0
                    player.setMediaItems(getMediaItemsFromList(restored), newIndex, currentPositionMs)
                    if (wasPlaying) player.play()
                }
            }
        }
    }

    fun toggleRepeat() {
        ensurePlayer()
        exoPlayer?.let {
            val newMode = if (it.repeatMode == Player.REPEAT_MODE_OFF) {
                Player.REPEAT_MODE_ALL
            } else {
                Player.REPEAT_MODE_OFF
            }
            it.repeatMode = newMode
        }
    }

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        _sleepTimerRemainingSeconds.value = (minutes * 60).toLong()
        sleepTimerJob = playScope.launch {
            while (isActive) {
                delay(1000)
                val current = _sleepTimerRemainingSeconds.value
                if (current != null && current > 0) {
                    val remaining = current - 1
                    _sleepTimerRemainingSeconds.value = remaining
                    
                    // Auxio-style gentle volume fade out over the last 15 seconds
                    if (remaining in 1..15) {
                        val fadeFactor = (remaining.toFloat() / 15f).coerceIn(0.05f, 1f)
                        exoPlayer?.volume = fadeFactor
                    }
                    
                    if (remaining <= 0) {
                        // Volume is already faded out; pause immediately. The old code started a 300ms
                        // fade-out and then cancelSleepTimer() reset volume to 1.0 while still playing,
                        // producing a short full-volume blast right at the end of the timer.
                        fadeJob?.cancel()
                        exoPlayer?.volume = 0f
                        exoPlayer?.pause()
                        cancelSleepTimer() // also restores volume to 1.0 for the next play
                        break
                    }
                } else {
                    break
                }
            }
        }
    }

    fun startSleepTimerEndOfTrack() {
        cancelSleepTimer()
        sleepTimerEndOfTrack = true
        exoPlayer?.pauseAtEndOfMediaItems = true
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = null
        sleepTimerEndOfTrack = false
        exoPlayer?.pauseAtEndOfMediaItems = false
        exoPlayer?.volume = 1f
    }

    fun release() {
        unregisterNoisyReceiver()
        trackerJob?.cancel()
        playScope.cancel()
        if (currentAudioSessionId != 0) {
            try {
                val closeIntent = Intent(android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, currentAudioSessionId)
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                }
                context.sendBroadcast(closeIntent)
                currentAudioSessionId = 0
            } catch (e: Exception) {
                // ignore
            }
        }
        exoPlayer?.release()
        exoPlayer = null
        _isPlaying.value = false
        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            context.stopService(serviceIntent)
        } catch (e: Exception) {
            timber.log.Timber.tag("PlaybackController").w(e, "Error stopping PlaybackService on release")
        }
    }

    private fun startPositionTracker() {
        trackerJob?.cancel()
        trackerJob = playScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition.toInt()
                        _currentPosition.value = pos
                        
                        val trackId = currentTrackIdForLogging
                        // History is keyed by stable UID so play counts survive rescans.
                        val trackUid = _currentTrack.value?.takeIf { it.id == trackId }?.uid
                        val dur = _duration.value
                        if (trackId != null && !trackUid.isNullOrEmpty() && dur > 0 && pos >= dur * 0.9 && !loggedForCurrentTrack) {
                            loggedForCurrentTrack = true
                            launch(Dispatchers.IO) {
                                try {
                                    val repo = (context.applicationContext as MusicApplication).repository
                                    repo.insertPlaybackHistory(com.example.data.PlaybackHistoryEntity(trackUid = trackUid))
                                    timber.log.Timber.tag("PlaybackController").d("Successfully logged playback history for track: %s", trackId)
                                } catch (e: Exception) {
                                    timber.log.Timber.tag("PlaybackController").e(e, "Failed to log playback history")
                                }
                            }
                        }
                    }
                }
                delay(250)
            }
        }
    }
}
