#!/bin/bash
cat << 'INNER_EOF' > script.awk
/^import android.graphics.BitmapFactory/ {
    print
    print "import kotlinx.coroutines.GlobalScope"
    print "import kotlinx.coroutines.launch"
    print "import kotlinx.coroutines.withContext"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/widget/MusicWidgetProvider.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/widget/MusicWidgetProvider.kt
