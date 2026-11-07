with open('app/src/main/java/com/example/data/SettingsRepository.kt', 'r') as f:
    content = f.read()

# I will replace the default values in MutableStateFlows with the synchronous reads from sharedPrefs.
# Actually, I can just remove `scope.launch` from `init { }`. Since `sharedPrefs.getString` is lightweight enough on Android, it's very commonly done synchronously.
# `SharedPreferences` reads are disk IO, but they are cached in memory after the first read. The first read is blocking anyway if it hasn't completed.
# If I just remove `scope.launch {` and `}`, it executes synchronously in `init`.

content = content.replace(
    '    init {\n        scope.launch {',
    '    init {'
)
content = content.replace(
    '        }\n    }\n\n    fun setThemeStyle',
    '    }\n\n    fun setThemeStyle'
)

with open('app/src/main/java/com/example/data/SettingsRepository.kt', 'w') as f:
    f.write(content)

