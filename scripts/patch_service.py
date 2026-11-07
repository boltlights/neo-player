with open('app/src/main/java/com/example/PlaybackService.kt', 'r') as f:
    content = f.read()

import_str = "import com.google.common.util.concurrent.ListenableFuture\nimport kotlinx.coroutines.runBlocking\nimport kotlinx.coroutines.flow.first"
content = content.replace("import com.google.common.util.concurrent.ListenableFuture", import_str)

old_code = """                if (parentId == ROOT_ID) {
                    val children = mutableListOf<MediaItem>()
                    // Get currently playing item as a child for Android Auto if we don't implement full browsing
                    val currentTrack = controller.currentTrack.value
                    if (currentTrack != null) {
                        children.add(
                            MediaItem.Builder()
                                .setMediaId(currentTrack.id.toString())
                                .setUri(currentTrack.filePath)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(currentTrack.title)
                                        .setArtist(currentTrack.artist)
                                        .setAlbumTitle(currentTrack.album)
                                        .setIsPlayable(true)
                                        .build()
                                )
                                .build()
                        )
                    }
                    return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
                }"""

new_code = """                if (parentId == ROOT_ID) {
                    val children = mutableListOf<MediaItem>()
                    try {
                        val app = application as MusicApplication
                        val tracks = runBlocking { app.repository.allTracks.first() }
                        tracks.forEach { track ->
                            children.add(
                                MediaItem.Builder()
                                    .setMediaId(track.id.toString())
                                    .setUri(track.filePath)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(track.title)
                                            .setArtist(track.artist)
                                            .setAlbumTitle(track.album)
                                            .setIsPlayable(true)
                                            .build()
                                    )
                                    .build()
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
                }"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/PlaybackService.kt', 'w') as f:
    f.write(content)
