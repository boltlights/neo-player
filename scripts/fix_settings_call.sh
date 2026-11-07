#!/bin/bash
cat << 'INNER_EOF' > script.awk
/com.example.ui.screens.SmartVolumeScreen\(/ {
    in_call = 1
}
/onBack = { showSmartVolumeScreen = false }/ {
    if (in_call) {
        print "            onBgColor = onBgColor,"
        print
        in_call = 0
        next
    }
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/SettingsScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/SettingsScreen.kt
