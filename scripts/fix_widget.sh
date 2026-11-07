#!/bin/bash
cat << 'INNER_EOF' > script.awk
/override fun onUpdate\(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray\) \{/ {
    print
    print "        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {"
    skip_close = 1
    next
}
/val views = RemoteViews\(context.packageName, R.layout.music_widget\)/ {
    print
    next
}
/appWidgetManager.updateAppWidget\(appWidgetId, views\)/ {
    print "            withContext(kotlinx.coroutines.Dispatchers.Main) {"
    print "                appWidgetManager.updateAppWidget(appWidgetId, views)"
    print "            }"
    next
}
skip_close && /^\s*\}\s*$/ {
    if (close_count == 1) {
        print "        }"
        print "    }"
        skip_close = 0
        next
    }
    close_count++
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/widget/MusicWidgetProvider.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/widget/MusicWidgetProvider.kt
