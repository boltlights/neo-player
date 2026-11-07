#!/bin/bash
cat << 'INNER_EOF' > script.awk
/fun MainScreenLayout\(/ {
    in_main_screen = 1
}
in_main_screen && /val themeStyle/ {
    print
    print "    val context = LocalContext.current"
    in_main_screen = 0
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MainActivity.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MainActivity.kt
