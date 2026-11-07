with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'    \}\n    \}', '    }', content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
