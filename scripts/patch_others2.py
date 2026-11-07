import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

pattern = r'(fun OthersSettingsScreen.*?\n        }\n    }\n})'
match = re.search(pattern, content, re.DOTALL)

if match:
    old_func = match.group(1)
    
    new_func = old_func[:-2] + """
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
                    tint = onBgColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Crash Logs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor
                    )
                    Text(
                        "View and export crash logs",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor.copy(alpha = 0.6f)
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
}"""
    
    content = content.replace(old_func, new_func)

# Append the dialog if not already there
if "fun ErrorLogsDialog" not in content:
    content += """

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
                    color = onBgColor
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
                        color = onBgColor.copy(alpha = 0.8f),
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

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
