#!/bin/bash
cat << 'INNER_EOF' > script.awk
/val currentScreen by viewModel.currentScreen/ {
    print "    val context = androidx.compose.ui.platform.LocalContext.current"
    print
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MainActivity.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MainActivity.kt
