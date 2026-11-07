#!/bin/bash
cat << 'INNER_EOF' > script.awk
/val match = Regex/ {
    print "        val match = LEADING_NUMBER_REGEX.find(trimmed)"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/util/NaturalOrderComparator.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/util/NaturalOrderComparator.kt
