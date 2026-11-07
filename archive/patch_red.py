import re
with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("val color = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.8f)", "val color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)")

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
