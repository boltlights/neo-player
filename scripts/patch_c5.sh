#!/bin/bash
awk '
BEGIN { skip = 0 }
/if \(uriString.startsWith\("content:\/\/"\)\) \{/ {
    print "                if (uriString.startsWith(\"content://\")) {"
    print "                    val retriever = android.media.MediaMetadataRetriever()"
    print "                    try {"
    print "                        retriever.setDataSource(context, Uri.parse(uriString))"
    print "                        return ExtractedMetadata("
    print "                            title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE),"
    print "                            artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST),"
    print "                            album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM),"
    print "                            albumArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),"
    print "                            genre = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE),"
    print "                            durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),"
    print "                            discNumber = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.split(\"/\")?.firstOrNull()?.toIntOrNull(),"
    print "                            trackNumber = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.split(\"/\")?.firstOrNull()?.toIntOrNull(),"
    print "                            musicBrainzId = null,"
    print "                            lyrics = null"
    print "                        )"
    print "                    } finally {"
    print "                        try { retriever.release() } catch(e: Exception) {}"
    print "                    }"
    print "                }"
    skip = 1
}
/\} catch \(e: Exception\) \{/ {
    if (skip) {
        skip = 0
    }
}
{
    if (!skip && $0 !~ /if \(uriString.startsWith\("content:\/\/"\)\) \{/) print $0
}
' app/src/main/java/com/example/util/MetadataExtractor.kt > app/src/main/java/com/example/util/MetadataExtractor.kt.tmp
mv app/src/main/java/com/example/util/MetadataExtractor.kt.tmp app/src/main/java/com/example/util/MetadataExtractor.kt
