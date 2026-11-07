import re

with open('app/src/main/java/com/example/ui/components/TrackRowItem.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.compose.foundation.combinedClickable', 
                          'import androidx.compose.foundation.combinedClickable\nimport androidx.compose.foundation.clickable\nimport androidx.compose.material.icons.filled.Person\nimport androidx.compose.material.icons.filled.Category')

with open('app/src/main/java/com/example/ui/components/TrackRowItem.kt', 'w') as f:
    f.write(content)
