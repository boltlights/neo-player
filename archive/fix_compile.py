import re
with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'r') as f:
    content = f.read()

# Fix @Composable twice
content = re.sub(r'''@Composable\n@OptIn\(androidx\.compose\.foundation\.ExperimentalFoundationApi::class\)\n@Composable\nfun PlaylistDetailScreen''', 
                 r'''@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen''', content, count=1)

content = re.sub(r'''@OptIn\(androidx\.compose\.foundation\.ExperimentalFoundationApi::class\)\n@Composable\n@Composable\nfun PlaylistDetailScreen''', 
                 r'''@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen''', content, count=1)

# Fix AutoMirrored
content = content.replace('Icons.AutoMirrored.Filled.ArrowBack', 'Icons.Default.ArrowBack')
content = content.replace('Icons.AutoMirrored.Filled.PlaylistPlay', 'Icons.Default.PlaylistPlay')
content = content.replace('Icons.AutoMirrored.Filled.QueueMusic', 'Icons.Default.QueueMusic')

# Fix combinedClickable
if 'import androidx.compose.foundation.combinedClickable' not in content:
    content = content.replace('import androidx.compose.foundation.clickable', 'import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.combinedClickable')

with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'w') as f:
    f.write(content)
