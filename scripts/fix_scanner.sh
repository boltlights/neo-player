#!/bin/bash
cat << 'INNER_EOF' > script.awk
/return "\/storage\/emulated\/0\/" \+ path.removePrefix\("\/tree\/primary:"\)/ {
    print
    next
}
/} else if \(path.startsWith\("\/tree\/raw:"\)\) \{/ {
    print
    next
}
/return path.removePrefix\("\/tree\/raw:"\)/ {
    print
    print "        } else if (path.startsWith(\"/tree/\")) {"
    print "            val suffix = path.removePrefix(\"/tree/\")"
    print "            val parts = suffix.split(\":\", limit = 2)"
    print "            if (parts.size == 2) {"
    print "                return \"/storage/\" + parts[0] + \"/\" + parts[1]"
    print "            }"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/util/DirectoryScanner.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/util/DirectoryScanner.kt
