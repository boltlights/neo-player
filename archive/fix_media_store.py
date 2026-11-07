import re

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''        } finally {
            setImporting(false)
            updateProgress(0f)
        }''',
'''        } finally {
            setImporting(false)
        }''')

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
