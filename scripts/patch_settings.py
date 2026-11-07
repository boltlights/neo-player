import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

import_str = "import com.example.ui.components.SleepTimerDialog"
if "import com.example.ui.components.SleepTimerDialog" not in content:
    content = content.replace("import com.example.MusicViewModel", import_str + "\nimport com.example.MusicViewModel")

header_block_old = """            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    "SETTINGS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }"""

header_block_new = """            var showSleepTimerDialog by remember { mutableStateOf(false) }
            val sleepTimerRemaining by viewModel.playbackController.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()

            if (showSleepTimerDialog) {
                SleepTimerDialog(
                    onDismiss = { showSleepTimerDialog = false },
                    onSetTimer = { viewModel.playbackController.startSleepTimer(it) },
                    onSetEndOfTrack = { viewModel.playbackController.startSleepTimerEndOfTrack() },
                    onCancelTimer = { viewModel.playbackController.cancelSleepTimer() },
                    accentColor = accentColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SETTINGS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            tint = if (sleepTimerRemaining != null) accentColor else Color.White
                        )
                    }
                }
            }"""

content = content.replace(header_block_old, header_block_new)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
