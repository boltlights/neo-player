#!/bin/bash
cat << 'INNER_EOF' > script.awk
/fun SmartVolumeScreen\(/ {
    print
    print "    onBgColor: Color,"
    skip_onBgColor = 1
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt
sed -i 's/Color\.White/onBgColor/g' app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt

cat << 'INNER_EOF' > script2.awk
/fun ZoneItem\(/ {
    print
    print "    onBgColor: Color,"
    next
}
/ZoneItem\(/ {
    print
    print "                            onBgColor = onBgColor,"
    next
}
/fun AddSmartVolumeZoneDialog\(/ {
    print
    print "    onBgColor: Color,"
    next
}
/AddSmartVolumeZoneDialog\(/ {
    print
    print "            onBgColor = onBgColor,"
    next
}
{ print }
INNER_EOF
awk -f script2.awk app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt
