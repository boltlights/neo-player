#!/bin/bash
cat << 'INNER_EOF' > script.awk
/private lateinit var logFile: File/ {
    print
    print "    private var isInitialized = false"
    next
}
/fun init\(context: Context\)/ {
    print
    print "        if (isInitialized) return"
    print "        isInitialized = true"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/util/CrashReporter.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/util/CrashReporter.kt
