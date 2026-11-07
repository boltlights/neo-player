import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

new_methods = """
    fun setThemeStyle(style: MusicThemeStyle) {
        settingsRepository.setThemeStyle(style)
    }
    
    fun setSortOption(option: String) {
        settingsRepository.setSortOption(option)
    }

    fun updateSortOption(option: String) {
        settingsRepository.setSortOption(option)
    }
    
    fun setLibraryTabs(tabs: List<String>) {
        settingsRepository.setLibraryTabs(tabs)
    }

    fun updateLibraryTabs(tabs: List<String>) {
        settingsRepository.setLibraryTabs(tabs)
    }
"""

content = content.replace('override fun onCleared() {', new_methods + '\n    override fun onCleared() {')

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
