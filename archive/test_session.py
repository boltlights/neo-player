import re
with open('app/src/main/java/com/example/PlaybackService.kt', 'r') as f:
    content = f.read()

if 'session.isActive' not in content:
    print("isActive not found in PlaybackService")
