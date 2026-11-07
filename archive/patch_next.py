import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

target = r'''            val baseList = basePlaybackList\.toMutableList\(\)
            if \(current != null\) \{
                val baseIndex = baseList\.indexOfFirst \{ it\.id == current\.id \}
                if \(baseIndex != -1\) \{
                    var offset = 1
                    for \(track in tracks\) \{
                        if \(!baseList\.contains\(track\)\) \{
                            baseList\.add\(baseIndex \+ offset, track\)
                            offset\+\+
                        \}
                    \}
                \}
            \}
            basePlaybackList = baseList'''

repl = r'''            val baseList = basePlaybackList.toMutableList()
            if (current != null) {
                val baseIndex = baseList.indexOfFirst { it.id == current.id }
                if (baseIndex != -1) {
                    if (!baseList.contains(track)) {
                        baseList.add(baseIndex + 1, track)
                    }
                }
            } else {
                if (!baseList.contains(track)) {
                    baseList.add(track)
                }
            }
            basePlaybackList = baseList'''
content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
