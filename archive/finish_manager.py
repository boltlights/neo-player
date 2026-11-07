with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

import re

# Remove the StateFlow fields entirely
content = content.replace(
'''    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()''',
'''
    private fun updateProgress(progress: Float) {
        if (progress >= 1f) {
            repository.updateIndexingState(com.example.data.IndexingState.Completed(null))
        } else {
            repository.updateIndexingState(com.example.data.IndexingState.Indexing(progress))
        }
    }
    
    private fun setImporting(importing: Boolean) {
        if (importing) {
            repository.updateIndexingState(com.example.data.IndexingState.Indexing(0f))
        } else {
            repository.updateIndexingState(com.example.data.IndexingState.Completed(null))
        }
    }'''
)

# Now apply
content = content.replace('_isImporting.value = true', 'setImporting(true)')
content = content.replace('_isImporting.value = false', 'setImporting(false)')
content = re.sub(r'_importProgress\.value\s*=\s*(.+?)\n', r'updateProgress(\1)\n', content)

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
