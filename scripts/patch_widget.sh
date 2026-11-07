#!/bin/bash
awk '
/val decodeOpts = BitmapFactory.Options\(\).apply \{/ {
    print "                        val bitmap: Bitmap? = if (currentTrack.artPath.startsWith(\"content://\")) {"
    print "                            val retriever = android.media.MediaMetadataRetriever()"
    print "                            try {"
    print "                                retriever.setDataSource(context, android.net.Uri.parse(currentTrack.artPath))"
    print "                                val artBytes = retriever.embeddedPicture"
    print "                                if (artBytes != null) {"
    print "                                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }"
    print "                                    BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)"
    print "                                    val scale = Math.max(opts.outWidth / 200, opts.outHeight / 200)"
    print "                                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = Math.max(1, scale) }"
    print "                                    val bmp = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, decodeOpts)"
    print "                                    if (bmp != null) Bitmap.createScaledBitmap(bmp, 200, 200, true) else null"
    print "                                } else null"
    print "                            } catch (e: Exception) {"
    print "                                null"
    print "                            } finally {"
    print "                                try { retriever.release() } catch(e: Exception) {}"
    print "                            }"
    print "                        } else {"
    print "                            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }"
    print "                            BitmapFactory.decodeFile(currentTrack.artPath, opts)"
    print "                            val scale = Math.max(opts.outWidth / 200, opts.outHeight / 200)"
    print "                            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = Math.max(1, scale) }"
    print "                            val bmp = BitmapFactory.decodeFile(currentTrack.artPath, decodeOpts)"
    print "                            if (bmp != null) Bitmap.createScaledBitmap(bmp, 200, 200, true) else null"
    print "                        }"
    skip = 14
    next
}
{
    if (skip > 0) {
        skip--
    } else {
        print $0
    }
}
' app/src/main/java/com/example/widget/MusicWidgetProvider.kt > app/src/main/java/com/example/widget/MusicWidgetProvider.kt.tmp
mv app/src/main/java/com/example/widget/MusicWidgetProvider.kt.tmp app/src/main/java/com/example/widget/MusicWidgetProvider.kt
