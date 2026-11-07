#!/bin/bash

# Fix MusicWidgetProvider
cat << 'INNER_EOF' > script_widget.awk
/override fun onUpdate/ {
    print
    print "        val pendingResult = goAsync()"
    next
}
/GlobalScope.launch\(kotlinx.coroutines.Dispatchers.IO\)/ {
    print "        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {"
    next
}
/val app = context.applicationContext/ {
    print "            val app = context.applicationContext as? MusicApplication"
    print "            if (app == null) {"
    print "                pendingResult?.finish()"
    print "                return@launch"
    print "            }"
    next
}
/^[ \t]+}$/ {
    if (in_update == 1) {
        print "            pendingResult?.finish()"
        print "        }"
        in_update = 0
        next
    }
}
/override fun onUpdate/ {
    in_update = 1
}
{
    if (in_update == 1 && $0 ~ /^    }$/) {
        in_update = 0
    }
    print
}
INNER_EOF
# Actually simpler to just use sed for MusicWidgetProvider.
