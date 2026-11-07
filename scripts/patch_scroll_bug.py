import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

old_scroll = """                                    if (currentY < topBoundary) {
                                        val dist = (topBoundary - currentY).coerceAtLeast(1f)
                                        val speed = (dist * 0.4f).coerceIn(10f, 120f)
                                        songsListState.scrollBy(-speed)
                                    } else if (currentY > bottomBoundary) {
                                        val dist = (currentY - bottomBoundary).coerceAtLeast(1f)
                                        val speed = (dist * 0.4f).coerceIn(10f, 120f)
                                        songsListState.scrollBy(speed)
                                    }"""

new_scroll = """                                    if (currentY < topBoundary) {
                                        val dist = (topBoundary - currentY).coerceAtLeast(1f)
                                        val speed = (dist * 0.4f).coerceIn(10f, 120f)
                                        val consumed = songsListState.scrollBy(-speed)
                                        draggedItemOffset -= consumed
                                    } else if (currentY > bottomBoundary) {
                                        val dist = (currentY - bottomBoundary).coerceAtLeast(1f)
                                        val speed = (dist * 0.4f).coerceIn(10f, 120f)
                                        val consumed = songsListState.scrollBy(speed)
                                        draggedItemOffset -= consumed
                                    }"""

content = content.replace(old_scroll, new_scroll)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
