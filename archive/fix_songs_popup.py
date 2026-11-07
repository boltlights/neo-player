with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                                        itemCount = filteredTracks.size, popupText = { index -> filteredTracks.getOrNull(index)?.title?.firstOrNull()?.uppercase() ?: "" },''',
'''                                        itemCount = filteredTracks.size, popupText = { index ->
                                            val song = filteredTracks.getOrNull(index) ?: return@FastScrollbar ""
                                            when (sortOption) {
                                                "Artist" -> song.artist.firstOrNull()?.uppercase() ?: "?"
                                                "Album" -> song.album?.firstOrNull()?.uppercase() ?: "?"
                                                "Oldest", "Newest" -> {
                                                    // Display year from DateAdded 
                                                    val cal = java.util.Calendar.getInstance()
                                                    cal.timeInMillis = song.dateAdded * 1000L
                                                    cal.get(java.util.Calendar.YEAR).toString()
                                                }
                                                "Track Number" -> song.trackNumber?.toString() ?: "?"
                                                else -> song.title.firstOrNull()?.uppercase() ?: "?"
                                            }
                                        },'''
)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
