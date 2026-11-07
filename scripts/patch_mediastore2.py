import re
with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

content = content.replace('if (track.sourceUri.startsWith("content://"))', 'if (track.sourceUri?.startsWith("content://") == true)')
content = content.replace('android.net.Uri.parse(track.sourceUri)', 'android.net.Uri.parse(track.sourceUri!!)')

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
