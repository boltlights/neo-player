with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

import_str = "import kotlinx.coroutines.delay"
if import_str not in content:
    content = content.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\n" + import_str + "\nimport kotlinx.coroutines.isActive")

old_effect = """                        LaunchedEffect(draggedItemOffset) {
                            if (draggedIndex != null) {
                                val layoutInfo = songsListState.layoutInfo
                                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == draggedIndex }
                                if (itemInfo != null) {
                                    val currentY = itemInfo.offset + draggedItemOffset
                                    val viewportStart = layoutInfo.viewportStartOffset
                                    val viewportEnd = layoutInfo.viewportEndOffset
                                    
                                    if (currentY < viewportStart + 150) {
                                        songsListState.animateScrollBy(-50f)
                                    } else if (currentY + itemInfo.size > viewportEnd - 150) {
                                        songsListState.animateScrollBy(50f)
                                    }
                                }
                            }
                        }"""

new_effect = """                        LaunchedEffect(draggedIndex) {
                            while (draggedIndex != null && isActive) {
                                val layoutInfo = songsListState.layoutInfo
                                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == draggedIndex }
                                if (itemInfo != null) {
                                    val currentY = itemInfo.offset + draggedItemOffset + (itemInfo.size / 2f)
                                    val topBoundary = layoutInfo.viewportStartOffset + 150
                                    val bottomBoundary = layoutInfo.viewportEndOffset - 150
                                    
                                    if (currentY < topBoundary) {
                                        val dist = (topBoundary - currentY).coerceAtLeast(1f)
                                        val speed = (dist * 0.4f).coerceIn(10f, 120f)
                                        songsListState.scrollBy(-speed)
                                    } else if (currentY > bottomBoundary) {
                                        val dist = (currentY - bottomBoundary).coerceAtLeast(1f)
                                        val speed = (dist * 0.4f).coerceIn(10f, 120f)
                                        songsListState.scrollBy(speed)
                                    }
                                }
                                delay(16L) // 60 FPS continuous smooth scroll
                            }
                        }"""

content = content.replace(old_effect, new_effect)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
