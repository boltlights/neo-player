#!/bin/bash
awk '
/val tracks = runBlocking \{ app.repository.allTracks.first\(\) \}/ {
    print "                        val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()"
    print "                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {"
    print "                            try {"
    print "                                val tracks = app.repository.allTracks.first()"
    print "                                val children = mutableListOf<MediaItem>()"
    print "                                tracks.forEach { track ->"
    print "                                    children.add("
    print "                                        MediaItem.Builder()"
    print "                                            .setMediaId(track.id.toString())"
    print "                                            .setUri(track.filePath)"
    print "                                            .setMediaMetadata("
    print "                                                MediaMetadata.Builder()"
    print "                                                    .setTitle(track.title)"
    print "                                                    .setArtist(track.artist)"
    print "                                                    .setAlbumTitle(track.album)"
    print "                                                    .setArtworkUri(if (track.artPath.isNullOrEmpty()) null else android.net.Uri.parse(track.artPath))"
    print "                                                    .build()"
    print "                                            )"
    print "                                            .build()"
    print "                                    )"
    print "                                }"
    print "                                future.set(LibraryResult.ofItemList(children, null))"
    print "                            } catch(e: Exception) {"
    print "                                future.setException(e)"
    print "                            }"
    print "                        }"
    print "                        return future"
    skip = 23
    next
}
{
    if (skip > 0) {
        skip--
    } else {
        print $0
    }
}
' app/src/main/java/com/example/PlaybackService.kt > app/src/main/java/com/example/PlaybackService.kt.tmp
mv app/src/main/java/com/example/PlaybackService.kt.tmp app/src/main/java/com/example/PlaybackService.kt
