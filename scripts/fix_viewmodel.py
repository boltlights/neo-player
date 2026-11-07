import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

content = re.sub(r'private val _hideSampleMusicButton = MutableStateFlow\(\n.*?\n\)\n    val hideSampleMusicButton: StateFlow<Boolean> = _hideSampleMusicButton.asStateFlow\(\)', 'val hideSampleMusic: StateFlow<Boolean> = settingsRepository.hideSampleMusic', content)
content = re.sub(r'private val _liquidPaletteEnabled = MutableStateFlow\(\n.*?\n\)\n    val liquidPaletteEnabled: StateFlow<Boolean> = _liquidPaletteEnabled.asStateFlow\(\)', 'val liquidPalette: StateFlow<Boolean> = settingsRepository.liquidPalette', content)
content = re.sub(r'_liquidPaletteEnabled.value = enabled', 'settingsRepository.setLiquidPalette(enabled)', content)
content = re.sub(r'_hideSampleMusicButton.value = hide', 'settingsRepository.setHideSampleMusic(hide)', content)
# Also fix any usages in MainActivity or elsewhere? No, in MainActivity it was probably `hideSampleMusicButton` but let's change MusicViewModel to match the previous names so we don't break MainActivity.

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
