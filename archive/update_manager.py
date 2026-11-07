with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

import re

# We will remove _isImporting and _importProgress and replace with updating repository.indexingState
content = content.replace(
'''    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()''',
'''    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private fun updateProgress(progress: Float) {
        _importProgress.value = progress
        if (progress >= 1f) {
            repository.updateIndexingState(com.example.data.IndexingState.Completed(null))
        } else {
            repository.updateIndexingState(com.example.data.IndexingState.Indexing(progress))
        }
    }
    
    private fun setImporting(importing: Boolean) {
        _isImporting.value = importing
        if (importing) {
            repository.updateIndexingState(com.example.data.IndexingState.Indexing(0f))
        } else {
            repository.updateIndexingState(com.example.data.IndexingState.Completed(null))
        }
    }'''
)

# Now replace usages
content = content.replace('_isImporting.value = true', 'setImporting(true)')
content = content.replace('_isImporting.value = false', 'setImporting(false)')

content = content.replace('_importProgress.value = ', 'updateProgress(')

# Fix calls like `updateProgress(0.5f + ...)` that might be missing the closing parenthesis.
# Using regex to capture `_importProgress.value = (expr)` and replace with `updateProgress((expr))`
content = re.sub(r'_importProgress\.value\s*=\s*(.+?)(?=\n)', r'updateProgress(\1)', content)

# But wait, `updateProgress(` was already substituted by `content.replace('_importProgress.value = ', 'updateProgress(')`
# Let's just do a clean regex from the original file.
