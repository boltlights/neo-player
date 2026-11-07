with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    fun changeThemeStyle(style: MusicThemeStyle) {
        settingsRepository.setThemeStyle(style)
    }''',
'''    fun changeThemeStyle(style: MusicThemeStyle) {
        settingsRepository.setThemeStyle(style)
    }

    fun setUseBlackTheme(enabled: Boolean) {
        settingsRepository.setUseBlackTheme(enabled)
    }'''
)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
