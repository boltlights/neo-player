import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

start_marker = r'// Preferences & Settings persistence'
end_marker = r'val importProgress: StateFlow<Float> = mediaStoreManager\.importProgress'

replacement = """// Preferences & Settings persistence
    val themeStyle: StateFlow<MusicThemeStyle> = settingsRepository.themeStyle
    val albumArtStyle: StateFlow<AlbumArtStyle> = settingsRepository.albumArtStyle
    val libraryAlbumArtStyle: StateFlow<LibraryAlbumArtStyle> = settingsRepository.libraryAlbumArtStyle
    val rotateCd: StateFlow<Boolean> = settingsRepository.rotateCd
    val hideSampleMusicButton: StateFlow<Boolean> = settingsRepository.hideSampleMusic
    val liquidPaletteEnabled: StateFlow<Boolean> = settingsRepository.liquidPalette
    val sortOption: StateFlow<String> = settingsRepository.sortOption
    val libraryTabs: StateFlow<List<String>> = settingsRepository.libraryTabs
    val includedFolders: StateFlow<Set<String>> = settingsRepository.includedFolders
    val excludedFolders: StateFlow<Set<String>> = settingsRepository.excludedFolders
    val autoRefreshEnabled: StateFlow<Boolean> = settingsRepository.autoRefreshEnabled
    val isImporting: StateFlow<Boolean> = mediaStoreManager.isImporting
    val importProgress: StateFlow<Float> = mediaStoreManager.importProgress"""

content = re.sub(start_marker + r'.*?' + end_marker, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
