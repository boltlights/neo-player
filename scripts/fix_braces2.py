with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

import re
# First, remove those stray brackets I just added.
content = re.sub(r'    fun importAudioFiles\(context: Context, uris: List<Uri>, onCompleted: \(Int\) -> Unit\) \{\n    \}', '    }\n\n    fun importAudioFiles(context: Context, uris: List<Uri>, onCompleted: (Int) -> Unit) {', content)
content = re.sub(r'    fun importAudioFolder\(context: Context, folderUri: Uri, onCompleted: \(Int\) -> Unit\) \{\n    \}', '    }\n\n    fun importAudioFolder(context: Context, folderUri: Uri, onCompleted: (Int) -> Unit) {', content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
