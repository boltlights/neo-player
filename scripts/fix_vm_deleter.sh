#!/bin/bash
cat << 'INNER_EOF' > script.awk
/val autoRefreshEnabled: StateFlow<Boolean> = settingsRepository.autoRefreshEnabled/ {
    print
    print "    private val _deleteIntentSender = kotlinx.coroutines.flow.MutableSharedFlow<android.content.IntentSender>()"
    print "    val deleteIntentSender: kotlinx.coroutines.flow.SharedFlow<android.content.IntentSender> = _deleteIntentSender.asSharedFlow()"
    next
}
/com.example.util.FileDeleter.deleteSafely\(context, track.filePath\)/ {
    print "                    val sender1 = com.example.util.FileDeleter.deleteSafely(context, track.filePath)"
    print "                    if (sender1 != null) { _deleteIntentSender.emit(sender1) }"
    next
}
/com.example.util.FileDeleter.deleteSafely\(context, path\)/ {
    print "                        val sender2 = com.example.util.FileDeleter.deleteSafely(context, path)"
    print "                        if (sender2 != null) { _deleteIntentSender.emit(sender2) }"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MusicViewModel.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MusicViewModel.kt
