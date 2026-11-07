with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

sleep_timer_state = """    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()
    
    var sleepTimerEndOfTrack = false
        private set
    private var sleepTimerJob: Job? = null"""

content = content.replace(
    '    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)',
    sleep_timer_state + '\n\n    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)'
)

sleep_timer_methods = """    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        _sleepTimerRemainingSeconds.value = (minutes * 60).toLong()
        sleepTimerJob = playScope.launch {
            while (isActive) {
                delay(1000)
                val current = _sleepTimerRemainingSeconds.value
                if (current != null && current > 0) {
                    _sleepTimerRemainingSeconds.value = current - 1
                    if (current - 1 <= 0) {
                        smoothPause()
                        cancelSleepTimer()
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
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = null
        sleepTimerEndOfTrack = false
    }"""

content = content.replace(
    '    fun release() {',
    sleep_timer_methods + '\n\n    fun release() {'
)

on_media_transition = """                        if (sleepTimerEndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                            sleepTimerEndOfTrack = false
                            this@apply.pause()
                            _sleepTimerRemainingSeconds.value = null
                        }"""

content = content.replace(
    '                        updateCurrentTrackState(mediaItem)',
    '                        updateCurrentTrackState(mediaItem)\n' + on_media_transition
)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
