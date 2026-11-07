#!/bin/bash
cat << 'INNER_EOF' > script.awk
/tint = Color.White,/ {
    if (in_smart_volume) {
        print "                    tint = onBgColor,"
        next
    }
}
/contentDescription = "Smart Volume Driver Mode",/ {
    in_smart_volume = 1
}
/color = Color.White$/ {
    if (in_smart_volume) {
        print "                        color = onBgColor"
        next
    }
}
/color = Color.White.copy\(alpha = 0.6f\)/ {
    if (in_smart_volume) {
        print "                        color = onBgColor.copy(alpha = 0.6f)"
        in_smart_volume = 0
        next
    }
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/SettingsScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/SettingsScreen.kt
