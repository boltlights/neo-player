import re

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

effect = """    var showSleepTimerDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(track) {
        viewModel.fetchLyricsIfMissing(track)
    }"""

content = content.replace(
    '    var showSleepTimerDialog by remember { mutableStateOf(false) }',
    effect
)

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
