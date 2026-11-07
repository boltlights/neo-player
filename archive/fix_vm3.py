with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'    fun changeThemeStyle\(style: MusicThemeStyle\) \{.*?settingsRepository\.setUseBlackTheme\(enabled\)', 
'''    fun changeThemeStyle(style: MusicThemeStyle) {
        settingsRepository.setThemeStyle(style)
    }

    fun setUseBlackTheme(enabled: Boolean) {
        settingsRepository.setUseBlackTheme(enabled)
    }''', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
