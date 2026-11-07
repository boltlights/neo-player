import re
with open('app/src/main/java/com/example/ui/components/PlaylistDialogs.kt', 'r') as f:
    content = f.read()

# Fix collectAsState import
if "import androidx.compose.runtime.collectAsState" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.runtime.collectAsState")

# Fix Check icon import
if "import androidx.compose.material.icons.filled.Check" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Add", "import androidx.compose.material.icons.filled.Add\nimport androidx.compose.material.icons.filled.Check")

# Fix collectAsState syntax
content = content.replace(".androidx.compose.runtime.collectAsState(initial = emptyList())", ".collectAsState(initial = emptyList())")

# Fix Icons.Default.Check syntax
content = content.replace("androidx.compose.material.icons.Icons.Default.Check", "Icons.Default.Check")

with open('app/src/main/java/com/example/ui/components/PlaylistDialogs.kt', 'w') as f:
    f.write(content)
