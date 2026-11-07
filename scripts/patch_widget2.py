with open('app/src/main/java/com/example/widget/MusicWidgetProvider.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_media_play)',
    'views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)'
)

with open('app/src/main/java/com/example/widget/MusicWidgetProvider.kt', 'w') as f:
    f.write(content)
