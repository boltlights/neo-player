with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Fix the import
if 'import androidx.compose.ui.window.Dialog' not in content:
    content = content.replace('import androidx.compose.foundation.background', 'import androidx.compose.ui.window.Dialog\nimport androidx.compose.foundation.background')

# Fix the extra }
old_code = """        if (showErrorLogs) {
            ErrorLogsDialog(
                onDismiss = { showErrorLogs = false },
                accentColor = accentColor,
                surfaceColor = surfaceColor,
                onBgColor = onBgColor
            )
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}
}"""

new_code = """        if (showErrorLogs) {
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

if old_code in content:
    content = content.replace(old_code, new_code)
else:
    # try another way
    content = content.replace("        Spacer(modifier = Modifier.height(120.dp))\n    }\n}\n}\n", "        Spacer(modifier = Modifier.height(120.dp))\n    }\n}\n")

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
