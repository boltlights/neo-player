import re
with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'val baseList = basePlaybackList.toMutableList()' in line:
        pass
        # I should just run gradle to see what errors remain.
