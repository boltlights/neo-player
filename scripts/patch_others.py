import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

old_others = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.setHideSampleMusicButton(!hideSampleMusicButton) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Hide sample music button",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Switch(
                checked = hideSampleMusicButton,
                onCheckedChange = { viewModel.setHideSampleMusicButton(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}"""

new_others = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.setHideSampleMusicButton(!hideSampleMusicButton) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Hide sample music button",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Switch(
                checked = hideSampleMusicButton,
                onCheckedChange = { viewModel.setHideSampleMusicButton(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Debugging",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        var showErrorLogs by remember { mutableStateOf(false) }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showErrorLogs = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = "Error Logs",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Crash Logs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        "View and export crash logs",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        if (showErrorLogs) {
            ErrorLogsDialog(
                onDismiss = { showErrorLogs = false },
                accentColor = accentColor,
                surfaceColor = surfaceColor,
                onBgColor = onBgColor
            )
        }
    }
}

@Composable
fun ErrorLogsDialog(
    onDismiss: () -> Unit,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var logs by remember { mutableStateOf(com.example.util.CrashReporter.getLogs(context)) }
    
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(logs.toByteArray())
                }
                android.widget.Toast.makeText(context, "Exported successfully", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Crash Logs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = logs,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = { 
                        com.example.util.CrashReporter.clearLogs(context)
                        logs = com.example.util.CrashReporter.getLogs(context)
                    }) {
                        Text("Clear", color = Color.Red.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace)
                    }
                    
                    Row {
                        TextButton(onClick = { 
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(logs))
                            android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copy", color = accentColor, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                val date = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault()).format(java.util.Date())
                                exportLauncher.launch("crash_logs_$date.md")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Export", color = Color.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}"""

content = content.replace(old_others, new_others)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
