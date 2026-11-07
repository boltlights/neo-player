with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()
if 'import androidx.compose.material.icons.automirrored.filled.*' not in content:
    content = content.replace(
        'import androidx.compose.material.icons.filled.*',
        'import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.automirrored.filled.*'
    )
with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
