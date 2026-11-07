import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

old_state = """    // Sorting logic (searching is now handled externally)
    val filteredTracks = remember(tracks, sortOption) {"""

new_state = """    var isReordering by remember { mutableStateOf(false) }
    
    // Sorting logic (searching is now handled externally)
    val filteredTracks = remember(tracks, sortOption) {"""

content = content.replace(old_state, new_state)

old_current = """                                var draggedItemOffset by remember { mutableStateOf(0f) }"""

new_current = """                                var draggedItemOffset by remember { mutableStateOf(0f) }
                                
                                LaunchedEffect(filteredTracks) {
                                    if (!isReordering) {
                                        currentTracks = filteredTracks
                                    } else {
                                        isReordering = false
                                    }
                                }"""

content = content.replace(old_current, new_current)

old_dragend = """                                                onDragEnd = { 
                                                    draggedIndex = null
                                                    draggedItemOffset = 0f
                                                    onSortOptionChange("Custom")
                                                    onReorderLibrary(currentTracks)
                                                }"""

new_dragend = """                                                onDragEnd = { 
                                                    isReordering = true
                                                    draggedIndex = null
                                                    draggedItemOffset = 0f
                                                    onSortOptionChange("Custom")
                                                    onReorderLibrary(currentTracks)
                                                }"""

content = content.replace(old_dragend, new_dragend)

# Remove the original currentTracks remember dependency to prevent it from resetting automatically
old_var = """                                var currentTracks by remember(filteredTracks) { mutableStateOf(filteredTracks) }"""
new_var = """                                var currentTracks by remember { mutableStateOf(filteredTracks) }"""
content = content.replace(old_var, new_var)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
