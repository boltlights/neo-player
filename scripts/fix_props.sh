#!/bin/bash
cat << 'INNER_EOF' > script.awk
/extractTrackProperties\(track\)/ {
    print "            extractTrackProperties(context, track)"
    next
}
/private fun extractTrackProperties\(track: TrackEntity\): Map<String, String> \{/ {
    print "private fun extractTrackProperties(context: android.content.Context, track: TrackEntity): Map<String, String> {"
    next
}
/val file = File\(track.filePath\)/ {
    print "    if (track.filePath.startsWith(\"content://\")) {"
    print "        try {"
    print "            val uri = android.net.Uri.parse(track.filePath)"
    print "            val cursor = context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.SIZE), null, null, null)"
    print "            cursor?.use { c ->"
    print "                if (c.moveToFirst()) {"
    print "                    val sizeIndex = c.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)"
    print "                    if (sizeIndex != -1) {"
    print "                        val size = c.getLong(sizeIndex)"
    print "                        val mb = size / (1024.0 * 1024.0)"
    print "                        props[\"File Size\"] = String.format(\"%.2f MB\", mb)"
    print "                    }"
    print "                }"
    print "            }"
    print "            if (!props.containsKey(\"File Size\")) {"
    print "                val df = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)"
    print "                if (df != null && df.exists()) {"
    print "                    val mb = df.length() / (1024.0 * 1024.0)"
    print "                    props[\"File Size\"] = String.format(\"%.2f MB\", mb)"
    print "                } else {"
    print "                    props[\"File Size\"] = \"Unknown\""
    print "                }"
    print "            }"
    print "        } catch (e: Exception) {"
    print "            props[\"File Size\"] = \"Unknown\""
    print "        }"
    print "    } else {"
    print "        val file = File(track.filePath)"
    print "        if (file.exists()) {"
    print "            val length = file.length()"
    print "            val mb = length / (1024.0 * 1024.0)"
    print "            props[\"File Size\"] = String.format(\"%.2f MB\", mb)"
    print "        } else {"
    print "            props[\"File Size\"] = \"Unknown\""
    print "        }"
    print "    }"
    
    skip = 1
    next
}
skip && /\} else \{/ {
    skip = 2
    next
}
skip == 2 && /props\["File Size"\] = "Unknown"/ {
    skip = 3
    next
}
skip == 3 && /\}/ {
    skip = 0
    next
}
/extractor.setDataSource\(track.filePath\)/ {
    print "        if (track.filePath.startsWith(\"content://\")) {"
    print "            extractor.setDataSource(context, android.net.Uri.parse(track.filePath), null)"
    print "        } else {"
    print "            extractor.setDataSource(track.filePath)"
    print "        }"
    next
}
/if \(durationSec > 0 && file.exists\(\)\) \{/ {
    print "                    if (durationSec > 0) {"
    print "                        // try to extract size again"
    print "                        var fsize = 0L"
    print "                        if (track.filePath.startsWith(\"content://\")) {"
    print "                             val uri = android.net.Uri.parse(track.filePath)"
    print "                             val df = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)"
    print "                             if (df != null && df.exists()) fsize = df.length()"
    print "                        } else {"
    print "                             val f = File(track.filePath)"
    print "                             if (f.exists()) fsize = f.length()"
    print "                        }"
    print "                        if (fsize > 0) {"
    print "                            val br = (fsize * 8) / durationSec"
    print "                            props[\"Bitrate\"] = \"${br / 1000} kbps (est)\""
    print "                        }"
    print "                    }"
    skip_bitrate = 1
    next
}
skip_bitrate && /\}/ {
    skip_bitrate = 0
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/TrackPropertiesModal.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/TrackPropertiesModal.kt
