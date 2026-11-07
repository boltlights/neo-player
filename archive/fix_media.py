import re
with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

target = r'''        val mediaMetadata = androidx\.media3\.common\.MediaMetadata\.Builder\(\)
            \.setTitle\(track\.title\)
            \.setArtist\(track\.artist\)
            \.setAlbumTitle\(track\.album\)'''
repl = r'''        val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setIsPlayable(true)'''
content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
