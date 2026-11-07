with open('app/src/main/java/com/example/widget/MusicWidgetProvider.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '                withContext(kotlinx.coroutines.Dispatchers.Main) {\n                    appWidgetManager.updateAppWidget(appWidgetId, views)\n            }\n            pendingResult?.finish()\n        }\n        }\n    }',
    '                withContext(kotlinx.coroutines.Dispatchers.Main) {\n                    appWidgetManager.updateAppWidget(appWidgetId, views)\n                }\n            }\n            pendingResult?.finish()\n        }\n    }'
)

with open('app/src/main/java/com/example/widget/MusicWidgetProvider.kt', 'w') as f:
    f.write(content)
