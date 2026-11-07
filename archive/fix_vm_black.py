with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    val themeStyle: StateFlow<MusicThemeStyle> = settingsRepository.themeStyle''',
'''    val themeStyle: StateFlow<MusicThemeStyle> = settingsRepository.themeStyle
    val useBlackTheme: StateFlow<Boolean> = settingsRepository.useBlackTheme'''
)

content = content.replace(
'''    fun setThemeStyle(style: MusicThemeStyle) {
        settingsRepository.setThemeStyle(style)
    }''',
'''    fun setThemeStyle(style: MusicThemeStyle) {
        settingsRepository.setThemeStyle(style)
    }

    fun setUseBlackTheme(enabled: Boolean) {
        settingsRepository.setUseBlackTheme(enabled)
    }'''
)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
