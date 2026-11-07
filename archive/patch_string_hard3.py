with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'if \(searchQuery\.isNotEmpty\(\)\) "No tracks found matching query" else "It\'s quite lonely in here\.Add some songs to get started\.",', 'text = if (searchQuery.isNotEmpty()) "No tracks found matching query" else "It is quite lonely in here.\\nAdd some songs to get started.",', content)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
