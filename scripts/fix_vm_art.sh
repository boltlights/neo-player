#!/bin/bash
cat << 'INNER_EOF' > script.awk
/if \(!track.artPath.isNullOrEmpty\(\)\)/ {
    print
    print "                    val realArt = track.artPath.replaceFirst(\"audiocover://\", \"content://\")"
    next
}
/if \(track.artPath.startsWith\("content:\/\/\"\)\)/ {
    print "                        if (realArt.startsWith(\"content://\")) {"
    next
}
/retriever.setDataSource\(context, android.net.Uri.parse\(track.artPath\)\)/ {
    print "                                retriever.setDataSource(context, android.net.Uri.parse(realArt))"
    next
}
/val file = File\(track.artPath\)/ {
    print "                            val file = File(realArt)"
    next
}
/bitmap = android.graphics.BitmapFactory.decodeFile\(track.artPath, decodeOptions\)/ {
    print "                                bitmap = android.graphics.BitmapFactory.decodeFile(realArt, decodeOptions)"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MusicViewModel.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MusicViewModel.kt
