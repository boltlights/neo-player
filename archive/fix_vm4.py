with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

import re
# We want to replace everything from `fun changeThemeStyle` up to `fun setAlbumArtStyle`
content = re.sub(r'    fun changeThemeStyle\(style: MusicThemeStyle\).*?fun setAlbumArtStyle\(style: AlbumArtStyle\)', 
'''    fun changeThemeStyle(style: MusicThemeStyle) {
        settingsRepository.setThemeStyle(style)
    }

    fun setUseBlackTheme(enabled: Boolean) {
        settingsRepository.setUseBlackTheme(enabled)
    }

    fun setAlbumArtStyle(style: AlbumArtStyle)''', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
