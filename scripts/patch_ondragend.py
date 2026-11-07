with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

old_drag_end = """                                                onDragEnd = { 
                                                    draggedIndex = null
                                                    draggedItemOffset = 0f
                                                    onReorderLibrary(currentTracks)
                                                }"""

new_drag_end = """                                                onDragEnd = { 
                                                    draggedIndex = null
                                                    draggedItemOffset = 0f
                                                    onSortOptionChange("Custom")
                                                    onReorderLibrary(currentTracks)
                                                }"""

content = content.replace(old_drag_end, new_drag_end)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
