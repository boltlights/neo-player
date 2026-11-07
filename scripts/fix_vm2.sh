#!/bin/bash
cat << 'INNER_EOF' > script.awk
/private val geofenceManager/,/deleteSmartVolumeZone/ {
    buffer = buffer $0 "\n"
    next
}
/private val metadataExtractor/ {
    print
    print buffer
    buffer = ""
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MusicViewModel.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MusicViewModel.kt
