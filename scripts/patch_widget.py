with open('app/src/main/java/com/example/widget/MusicWidgetProvider.kt', 'r') as f:
    content = f.read()

import_str = "import android.graphics.Bitmap\nimport android.graphics.BitmapFactory"
content = content.replace("import android.graphics.BitmapFactory", import_str)

old_code = """                if (currentTrack.artPath?.startsWith("content://") == true) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(context, android.net.Uri.parse(currentTrack.artPath))
                        val artBytes = retriever.embeddedPicture
                        if (artBytes != null) {
                            val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                            views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_media_play)
                        }
                        try { retriever.release() } catch(e: Exception) {}
                    } catch (e: Exception) {
                         views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_media_play)
                    }
                } else {
                     views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_media_play)
                }"""

new_code = """                if (!currentTrack.artPath.isNullOrEmpty()) {
                    try {
                        val bitmap: Bitmap? = if (currentTrack.artPath.startsWith("content://")) {
                            val retriever = android.media.MediaMetadataRetriever()
                            retriever.setDataSource(context, android.net.Uri.parse(currentTrack.artPath))
                            val artBytes = retriever.embeddedPicture
                            try { retriever.release() } catch(e: Exception) {}
                            if (artBytes != null) BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size) else null
                        } else {
                            BitmapFactory.decodeFile(currentTrack.artPath)
                        }
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_media_play)
                        }
                    } catch (e: Exception) {
                        views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_media_play)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_album_art, android.R.drawable.ic_media_play)
                }"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/widget/MusicWidgetProvider.kt', 'w') as f:
    f.write(content)
