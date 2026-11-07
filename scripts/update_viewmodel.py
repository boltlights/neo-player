import re

with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

# Add new imports
if 'import com.example.data.SettingsRepository' not in content:
    content = content.replace('import com.example.data.MusicRepository', 'import com.example.data.SettingsRepository\nimport com.example.util.MediaStoreManager\nimport com.example.data.MusicRepository')

# Replace the repository declarations inside the class
repo_pattern = r'private val repository = musicApp.repository'
new_repo = '''private val repository = musicApp.repository
    private val settingsRepository = musicApp.settingsRepository
    private val mediaStoreManager = musicApp.mediaStoreManager'''
content = re.sub(repo_pattern, new_repo, content)

# Remove sharedPrefs declaration
shared_prefs_pattern = r'private val sharedPrefs = context\.getSharedPreferences\("music_player_prefs", Context\.MODE_PRIVATE\)'
content = re.sub(shared_prefs_pattern, '', content)

# Replace the MutableStateFlows initialized with sharedPrefs to just reference settingsRepository
settings_props = [
    ('_themeStyle', 'themeStyle', 'MusicThemeStyle', 'ELEGANT_DARK', 'setThemeStyle', 'settingsRepository.themeStyle'),
    ('_albumArtStyle', 'albumArtStyle', 'AlbumArtStyle', 'CD', 'setAlbumArtStyle', 'settingsRepository.albumArtStyle'),
    ('_libraryAlbumArtStyle', 'libraryAlbumArtStyle', 'LibraryAlbumArtStyle', 'ROUND', 'setLibraryAlbumArtStyle', 'settingsRepository.libraryAlbumArtStyle'),
    ('_rotateCd', 'rotateCd', 'Boolean', 'true', 'setRotateCd', 'settingsRepository.rotateCd'),
    ('_hideSampleMusic', 'hideSampleMusic', 'Boolean', 'false', 'setHideSampleMusic', 'settingsRepository.hideSampleMusic'),
    ('_liquidPalette', 'liquidPalette', 'Boolean', 'true', 'setLiquidPalette', 'settingsRepository.liquidPalette'),
    ('_sortOption', 'sortOption', 'String', 'Oldest', 'setSortOption', 'settingsRepository.sortOption'),
    ('_libraryTabs', 'libraryTabs', 'List<String>', 'emptyList()', 'setLibraryTabs', 'settingsRepository.libraryTabs'),
    ('_includedFolders', 'includedFolders', 'Set<String>', 'emptySet()', 'addIncludedFolder', 'settingsRepository.includedFolders'),
    ('_excludedFolders', 'excludedFolders', 'Set<String>', 'emptySet()', 'addExcludedFolder', 'settingsRepository.excludedFolders'),
    ('_autoRefreshEnabled', 'autoRefreshEnabled', 'Boolean', 'true', 'setAutoRefreshEnabled', 'settingsRepository.autoRefreshEnabled')
]

for private_prop, public_prop, prop_type, default, setter, repo_prop in settings_props:
    # Remove private prop definition
    if private_prop == '_themeStyle':
        content = re.sub(r'private val _themeStyle = MutableStateFlow\([^)]+\)', '', content)
    elif private_prop == '_albumArtStyle':
        content = re.sub(r'private val _albumArtStyle = MutableStateFlow\([^)]+\)', '', content)
    elif private_prop == '_libraryAlbumArtStyle':
        content = re.sub(r'private val _libraryAlbumArtStyle = MutableStateFlow\([^)]+\)', '', content)
    elif private_prop == '_libraryTabs':
        content = re.sub(r'private val defaultTabs = "Recently Added,Recently Played,Most Played"\n\s+private val _libraryTabs = MutableStateFlow\([^)]+\)', '', content)
    else:
        content = re.sub(rf'private val {private_prop} = MutableStateFlow\([^)]+\)', '', content)
    
    # Update public prop
    content = re.sub(rf'val {public_prop}: StateFlow<{prop_type}> = {private_prop}\.asStateFlow\(\)', rf'val {public_prop}: StateFlow<{prop_type}> = {repo_prop}', content)

# Remove the body of the settings setters
content = re.sub(r'fun setThemeStyle\(style: MusicThemeStyle\) \{[^}]+\}', r'fun setThemeStyle(style: MusicThemeStyle) {\n        settingsRepository.setThemeStyle(style)\n    }', content)
content = re.sub(r'fun setAlbumArtStyle\(style: AlbumArtStyle\) \{[^}]+\}', r'fun setAlbumArtStyle(style: AlbumArtStyle) {\n        settingsRepository.setAlbumArtStyle(style)\n    }', content)
content = re.sub(r'fun setLibraryAlbumArtStyle\(style: LibraryAlbumArtStyle\) \{[^}]+\}', r'fun setLibraryAlbumArtStyle(style: LibraryAlbumArtStyle) {\n        settingsRepository.setLibraryAlbumArtStyle(style)\n    }', content)
content = re.sub(r'fun setRotateCd\(rotate: Boolean\) \{[^}]+\}', r'fun setRotateCd(rotate: Boolean) {\n        settingsRepository.setRotateCd(rotate)\n    }', content)
content = re.sub(r'fun setLiquidPalette\(enabled: Boolean\) \{[^}]+\}', r'fun setLiquidPalette(enabled: Boolean) {\n        settingsRepository.setLiquidPalette(enabled)\n    }', content)
content = re.sub(r'fun setHideSampleMusic\(hide: Boolean\) \{[^}]+\}', r'fun setHideSampleMusic(hide: Boolean) {\n        settingsRepository.setHideSampleMusic(hide)\n    }', content)
content = re.sub(r'fun setSortOption\(option: String\) \{[^}]+\}', r'fun setSortOption(option: String) {\n        settingsRepository.setSortOption(option)\n    }', content)
content = re.sub(r'fun setLibraryTabs\(tabs: List<String>\) \{[^}]+\}', r'fun setLibraryTabs(tabs: List<String>) {\n        settingsRepository.setLibraryTabs(tabs)\n    }', content)
content = re.sub(r'fun addIncludedFolder\(uri: String\) \{[^}]+\}', r'fun addIncludedFolder(uri: String) {\n        settingsRepository.addIncludedFolder(uri)\n    }', content)
content = re.sub(r'fun removeIncludedFolder\(uri: String\) \{[^}]+\}', r'fun removeIncludedFolder(uri: String) {\n        settingsRepository.removeIncludedFolder(uri)\n    }', content)
content = re.sub(r'fun addExcludedFolder\(uri: String\) \{[^}]+\}', r'fun addExcludedFolder(uri: String) {\n        settingsRepository.addExcludedFolder(uri)\n    }', content)
content = re.sub(r'fun removeExcludedFolder\(uri: String\) \{[^}]+\}', r'fun removeExcludedFolder(uri: String) {\n        settingsRepository.removeExcludedFolder(uri)\n    }', content)
content = re.sub(r'fun setAutoRefreshEnabled\(enabled: Boolean\) \{[^}]+\}', r'fun setAutoRefreshEnabled(enabled: Boolean) {\n        settingsRepository.setAutoRefreshEnabled(enabled)\n    }', content)

# Remove `isImporting` and `importProgress` from ViewModel (we'll forward them from MediaStoreManager)
content = re.sub(r'private val _isImporting = MutableStateFlow\(false\)\s+val isImporting: StateFlow<Boolean> = _isImporting\.asStateFlow\(\)', 'val isImporting: StateFlow<Boolean> = mediaStoreManager.isImporting', content)
content = re.sub(r'private val _importProgress = MutableStateFlow\(0f\)\s+val importProgress: StateFlow<Float> = _importProgress\.asStateFlow\(\)', 'val importProgress: StateFlow<Float> = mediaStoreManager.importProgress', content)

# Replace scanMediaStore with call to manager
scan_mediastore_new = '''fun scanMediaStore(context: Context, onCompleted: (Int) -> Unit) {
        viewModelScope.launch {
            val included = includedFolders.value
            val excluded = excludedFolders.value
            val existing = allTracks.value
            val count = mediaStoreManager.scanMediaStore(context, included, excluded, existing)
            onCompleted(count)
        }
    }'''
content = re.sub(r'fun scanMediaStore\(context: Context, onCompleted: \(Int\) -> Unit\) \{.*?(?=fun importAudioFiles)', scan_mediastore_new + '\n\n    ', content, flags=re.DOTALL)

# Replace importAudioFiles with call to manager
import_audio_files_new = '''fun importAudioFiles(context: Context, uris: List<Uri>, onCompleted: (Int) -> Unit) {
        viewModelScope.launch {
            val existing = allTracks.value
            val count = mediaStoreManager.importAudioFiles(context, uris, existing)
            onCompleted(count)
        }
    }'''
content = re.sub(r'fun importAudioFiles\(context: Context, uris: List<Uri>, onCompleted: \(Int\) -> Unit\) \{.*?(?=fun importAudioFolder)', import_audio_files_new + '\n\n    ', content, flags=re.DOTALL)

# In importAudioFolder
import_folder_new = '''fun importAudioFolder(context: Context, folderUri: Uri, onCompleted: (Int) -> Unit) {
        val uriStr = folderUri.toString()
        if (!includedFolders.value.contains(uriStr)) {
            addIncludedFolder(uriStr)
        }
        scanMediaStore(context, onCompleted)
    }'''
content = re.sub(r'fun importAudioFolder\(context: Context, folderUri: Uri, onCompleted: \(Int\) -> Unit\) \{[^}]+\}', import_folder_new, content)


with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
