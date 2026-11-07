with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

import re

# Update imports
content = content.replace('import com.example.util.MediaStoreManager', 'import com.example.util.MediaStoreManager\nimport com.example.data.IndexingState')

# Replace isImporting and importProgress definitions
content = content.replace(
'''    val isImporting: StateFlow<Boolean> = mediaStoreManager.isImporting
    val importProgress: StateFlow<Float> = mediaStoreManager.importProgress''',
'''    val isImporting: StateFlow<Boolean> = repository.indexingState.map { it is IndexingState.Indexing }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val importProgress: StateFlow<Float> = repository.indexingState.map { (it as? IndexingState.Indexing)?.progress ?: 0f }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)'''
)

content = content.replace('!mediaStoreManager.isImporting.value', 'repository.indexingState.value !is IndexingState.Indexing')

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
