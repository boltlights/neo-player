import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

import_str = "import androidx.compose.foundation.gestures.scrollBy"
if import_str not in content:
    content = content.replace("import androidx.compose.foundation.gestures.animateScrollBy", "import androidx.compose.foundation.gestures.animateScrollBy\n" + import_str)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
