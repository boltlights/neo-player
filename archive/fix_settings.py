with open('app/src/main/java/com/example/data/SettingsRepository.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    private val _themeStyle = MutableStateFlow(MusicThemeStyle.ELEGANT_DARK)
    val themeStyle: StateFlow<MusicThemeStyle> = _themeStyle.asStateFlow()''',
'''    private val _themeStyle = MutableStateFlow(MusicThemeStyle.ELEGANT_DARK)
    val themeStyle: StateFlow<MusicThemeStyle> = _themeStyle.asStateFlow()

    private val _useBlackTheme = MutableStateFlow(false)
    val useBlackTheme: StateFlow<Boolean> = _useBlackTheme.asStateFlow()'''
)

content = content.replace(
'''            _themeStyle.value = MusicThemeStyle.valueOf(sharedPrefs.getString("theme_style", MusicThemeStyle.ELEGANT_DARK.name) ?: MusicThemeStyle.ELEGANT_DARK.name)''',
'''            _themeStyle.value = MusicThemeStyle.valueOf(sharedPrefs.getString("theme_style", MusicThemeStyle.ELEGANT_DARK.name) ?: MusicThemeStyle.ELEGANT_DARK.name)
            _useBlackTheme.value = sharedPrefs.getBoolean("use_black_theme", false)'''
)

content = content.replace(
'''    fun setThemeStyle(style: MusicThemeStyle) {
        _themeStyle.value = style
        scope.launch { sharedPrefs.edit().putString("theme_style", style.name).apply() }
    }''',
'''    fun setThemeStyle(style: MusicThemeStyle) {
        _themeStyle.value = style
        scope.launch { sharedPrefs.edit().putString("theme_style", style.name).apply() }
    }

    fun setUseBlackTheme(enabled: Boolean) {
        _useBlackTheme.value = enabled
        scope.launch { sharedPrefs.edit().putBoolean("use_black_theme", enabled).apply() }
    }'''
)

with open('app/src/main/java/com/example/data/SettingsRepository.kt', 'w') as f:
    f.write(content)
