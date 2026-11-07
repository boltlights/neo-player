import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

content = re.sub(r'fun setLiquidPalette\(enabled: Boolean\) \{', 'fun setLiquidPaletteEnabled(enabled: Boolean) {', content)
content = re.sub(r'fun setHideSampleMusic\(hide: Boolean\) \{', 'fun setHideSampleMusicButton(hide: Boolean) {', content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
