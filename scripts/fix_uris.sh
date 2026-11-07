#!/bin/bash
cat << 'INNER_EOF' > script.awk
/if \(uris.isNotEmpty\(\)\) \{/ {
    print
    print "            for (uri in uris) {"
    print "                try {"
    print "                    context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)"
    print "                } catch (e: Exception) { e.printStackTrace() }"
    print "            }"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MainActivity.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MainActivity.kt
