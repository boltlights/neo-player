#!/bin/bash
cat << 'INNER_EOF' > script.awk
/val windowSizeClass = calculateWindowSizeClass\(this@MainActivity\)/ {
    print
    print "            val context = LocalContext.current"
    next
}
/val context = LocalContext.current/ {
    # Skip the second declaration
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MainActivity.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MainActivity.kt
