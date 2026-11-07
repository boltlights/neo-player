with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

import re

# We will expose these new settings to MusicViewModel

content = content.replace(
'''    val autoSyncMediaStore: StateFlow<Boolean> = settingsRepository.autoSyncMediaStore''',
'''    val autoSyncMediaStore: StateFlow<Boolean> = settingsRepository.autoSyncMediaStore
    val intelligentSorting: StateFlow<Boolean> = settingsRepository.intelligentSorting
    val separators: StateFlow<String> = settingsRepository.separators
    val useFileTreeCache: StateFlow<Boolean> = settingsRepository.useFileTreeCache
    val locationMode: StateFlow<String> = settingsRepository.locationMode'''
)

content = content.replace(
'''    fun setAutoSyncMediaStore(enabled: Boolean) {
        settingsRepository.setAutoSyncMediaStore(enabled)
    }''',
'''    fun setAutoSyncMediaStore(enabled: Boolean) {
        settingsRepository.setAutoSyncMediaStore(enabled)
    }

    fun setIntelligentSorting(enabled: Boolean) {
        settingsRepository.setIntelligentSorting(enabled)
    }

    fun setSeparators(separators: String) {
        settingsRepository.setSeparators(separators)
    }

    fun setUseFileTreeCache(enabled: Boolean) {
        settingsRepository.setUseFileTreeCache(enabled)
    }

    fun setLocationMode(mode: String) {
        settingsRepository.setLocationMode(mode)
    }'''
)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
