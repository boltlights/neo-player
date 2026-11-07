import re

# Fix GeofenceBroadcastReceiver
with open('app/src/main/java/com/example/geofence/GeofenceBroadcastReceiver.kt', 'r') as f:
    geofence_content = f.read()

geofence_content = geofence_content.replace(
    'CoroutineScope(Dispatchers.IO).launch {',
    'val pendingResult = goAsync()\n            CoroutineScope(Dispatchers.IO).launch {'
)
geofence_content = re.sub(
    r'(applyVolume\(audioManager, currentVol, targetVol, zone\.isGradualFade\)\n[ \t]*})',
    r'\1\n                }\n                pendingResult?.finish()',
    geofence_content
)

with open('app/src/main/java/com/example/geofence/GeofenceBroadcastReceiver.kt', 'w') as f:
    f.write(geofence_content)

# Fix MusicWidgetProvider
with open('app/src/main/java/com/example/widget/MusicWidgetProvider.kt', 'r') as f:
    widget_content = f.read()

widget_content = widget_content.replace(
    'GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {',
    'val pendingResult = goAsync()\n        GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {'
)
widget_content = widget_content.replace(
    'val app = context.applicationContext as? MusicApplication ?: return@launch',
    'val app = context.applicationContext as? MusicApplication\n            if (app == null) { pendingResult?.finish(); return@launch }'
)
# Inject pendingResult?.finish() before the end of the launch block
# The launch block ends at line 116.
widget_content = re.sub(
    r'(appWidgetManager\.updateAppWidget\(appWidgetId, views\)\n[ \t]*}\n[ \t]*})',
    r'appWidgetManager.updateAppWidget(appWidgetId, views)\n            }\n            pendingResult?.finish()\n        }',
    widget_content
)

with open('app/src/main/java/com/example/widget/MusicWidgetProvider.kt', 'w') as f:
    f.write(widget_content)

