import re

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

import_str = "import com.example.ui.components.SleepTimerDialog\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll"
if "import com.example.ui.components.SleepTimerDialog" not in content:
    content = content.replace("import com.example.ui.components.SkipPrevIcon", import_str + "\nimport com.example.ui.components.SkipPrevIcon")

# add variables
state_vars = """    val targetBg = if (liquidPaletteEnabled) bgColor else MaterialTheme.colorScheme.background
    val animatedBg by androidx.compose.animation.animateColorAsState(targetValue = targetBg, animationSpec = tween(1000))

    val sleepTimerRemaining by viewModel.playbackController.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { viewModel.playbackController.startSleepTimer(it) },
            onSetEndOfTrack = { viewModel.playbackController.startSleepTimerEndOfTrack() },
            onCancelTimer = { viewModel.playbackController.cancelSleepTimer() },
            accentColor = accentColor
        )
    }"""

content = re.sub(r'    val targetBg.*?\n.*?showQueue.*?false.*?\n', state_vars + '\n', content, flags=re.DOTALL | re.MULTILINE)

# add top buttons
top_buttons = """                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sleepTimerRemaining != null) {
                        val minutes = sleepTimerRemaining!! / 60
                        val seconds = sleepTimerRemaining!! % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(accentColor.copy(alpha=0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal=8.dp, vertical=4.dp)
                                .padding(end=4.dp)
                        )
                    }
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemaining != null) accentColor else onBgColor
                        )
                    }
                    IconButton(onClick = { 
                        showLyrics = !showLyrics
                        if (showLyrics) showQueue = false
                    }) {
                        Icon(
                            if (showLyrics) Icons.Default.SubtitlesOff else Icons.Default.Subtitles,
                            contentDescription = "Toggle Lyrics",
                            tint = if (showLyrics) accentColor else onBgColor
                        )
                    }
                    IconButton(onClick = { 
                        showQueue = !showQueue 
                        if (showQueue) showLyrics = false
                    }) {"""

content = content.replace("                Row(verticalAlignment = Alignment.CenterVertically) {\n                    IconButton(onClick = { showQueue = !showQueue }) {", top_buttons)

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
