import re
with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

cleanup_code = """
                // Clean up dead tracks
                val resolver = context.contentResolver
                existingTracks.forEach { track ->
                    var exists = false
                    try {
                        if (track.sourceUri.startsWith("content://")) {
                            val cursor = resolver.query(android.net.Uri.parse(track.sourceUri), null, null, null, null)
                            if (cursor != null) {
                                exists = cursor.count > 0
                                cursor.close()
                            }
                        } else if (track.filePath.isNotEmpty()) {
                            exists = java.io.File(track.filePath).exists()
                        }
                    } catch (e: Exception) {
                        exists = false
                    }
                    if (!exists) {
                        repository.deleteTrack(track.id)
                    }
                }
"""

content = content.replace("withContext(Dispatchers.IO) {", "withContext(Dispatchers.IO) {" + cleanup_code)

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
