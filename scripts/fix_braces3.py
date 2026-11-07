with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

import re

# Find instances of `        }\n    fun ` where the indent implies missing function bracket.
# Actually, the easiest way is to look at the indentation.
# A function at indent 4 (1 tab) ends with a `}` at indent 4.
# If we see `    fun ` after `        }`, it's a dead giveaway that the `    }` is missing.

content = re.sub(r'        }\n    (//|fun|@OptIn|val|private)', r'        }\n    }\n\n    \1', content)

# There might also be `        }\n}` at the end of the class.
content = re.sub(r'        }\n}', r'        }\n    }\n}', content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
