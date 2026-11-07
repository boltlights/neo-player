#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/util/TimeUtils.kt
package com.example.util

fun formatDuration(ms: Long): String {
    if (ms < 0) return "00:00"
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    val hr = ms / (1000 * 60 * 60)
    return if (hr > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hr, min, sec)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", min, sec)
    }
}
INNER_EOF
