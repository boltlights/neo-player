with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'updateProgress\((.+?)\n', r'updateProgress(\1)\n', content)

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
