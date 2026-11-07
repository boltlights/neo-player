import re
with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'r') as f:
    content = f.read()

target = r'''@OptIn\(ExperimentalMaterial3Api::class\)
@OptIn\(androidx\.compose\.foundation\.ExperimentalFoundationApi::class\)'''

repl = r'''@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'w') as f:
    f.write(content)
