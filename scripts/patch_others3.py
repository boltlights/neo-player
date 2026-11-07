import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

old_end = """        if (showErrorLogs) {
            ErrorLogsDialog(
                onDismiss = { showErrorLogs = false },
                accentColor = accentColor,
                surfaceColor = surfaceColor,
                onBgColor = onBgColor
            )
        }
    }
}"""

new_end = """        if (showErrorLogs) {
            ErrorLogsDialog(
                onDismiss = { showErrorLogs = false },
                accentColor = accentColor,
                surfaceColor = surfaceColor,
                onBgColor = onBgColor
            )
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}"""

content = content.replace(old_end, new_end)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
