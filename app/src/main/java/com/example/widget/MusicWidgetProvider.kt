package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MusicApplication
import com.example.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.example.widget.ACTION_PREV"
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MusicWidgetProvider::class.java))
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, MusicWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? MusicApplication ?: return@launch
                val controller = app.playbackController
            val currentTrack = controller.currentTrack.value
            val isPlaying = controller.isPlaying.value

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.music_widget)
                
                if (currentTrack != null) {
                    views.setTextViewText(R.id.widget_title, currentTrack.title)
                    views.setTextViewText(R.id.widget_artist, currentTrack.artist)
                    
                    val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)
                    
                    if (!currentTrack.artPath.isNullOrEmpty()) {
                        try {
                            val artPath = currentTrack.artPath
                            val bitmap: Bitmap? = if (com.example.util.AudioThumbnailCache.hasThumbnail(context, artPath)) {
                                val thumbFile = com.example.util.AudioThumbnailCache.getThumbnailFile(context, artPath)
                                val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                                if (bmp != null) Bitmap.createScaledBitmap(bmp, 200, 200, true) else null
                            } else {
                                val resolvedUri = if (artPath.startsWith("audiocover://")) {
                                    android.net.Uri.parse(artPath.replaceFirst("audiocover://", "content://"))
                                } else if (artPath.startsWith("content://")) {
                                    android.net.Uri.parse(artPath)
                                } else {
                                    null
                                }

                                if (resolvedUri != null) {
                                    val retriever = android.media.MediaMetadataRetriever()
                                    try {
                                        retriever.setDataSource(context, resolvedUri)
                                        val artBytes = retriever.embeddedPicture
                                        if (artBytes != null) {
                                            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
                                            val scale = Math.max(opts.outWidth / 200, opts.outHeight / 200)
                                            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = Math.max(1, scale) }
                                            val bmp = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, decodeOpts)
                                            if (bmp != null) Bitmap.createScaledBitmap(bmp, 200, 200, true) else null
                                        } else null
                                    } catch (e: Exception) {
                                        null
                                    } finally {
                                        try { retriever.release() } catch(e: Exception) {}
                                    }
                                } else {
                                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                    BitmapFactory.decodeFile(artPath, opts)
                                    val scale = Math.max(opts.outWidth / 200, opts.outHeight / 200)
                                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = Math.max(1, scale) }
                                    val bmp = BitmapFactory.decodeFile(artPath, decodeOpts)
                                    if (bmp != null) Bitmap.createScaledBitmap(bmp, 200, 200, true) else null
                                }
                            }

                            if (bitmap != null) {
                                views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                            } else {
                                views.setImageViewResource(R.id.widget_album_art, R.drawable.img_default_album_art)
                            }
                        } catch (e: Exception) {
                            views.setImageViewResource(R.id.widget_album_art, R.drawable.img_default_album_art)
                        }
                    } else {
                        views.setImageViewResource(R.id.widget_album_art, R.drawable.img_default_album_art)
                    }
                } else {
                    views.setTextViewText(R.id.widget_title, "No song playing")
                    views.setTextViewText(R.id.widget_artist, "Neo Player")
                    views.setImageViewResource(R.id.widget_btn_play_pause, android.R.drawable.ic_media_play)
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.img_default_album_art)
                }

                views.setOnClickPendingIntent(R.id.widget_btn_play_pause, getPendingIntent(context, ACTION_PLAY_PAUSE))
                views.setOnClickPendingIntent(R.id.widget_btn_next, getPendingIntent(context, ACTION_NEXT))
                views.setOnClickPendingIntent(R.id.widget_btn_prev, getPendingIntent(context, ACTION_PREV))
                
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launchIntent != null) {
                    val launchPendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    views.setOnClickPendingIntent(R.id.widget_album_art, launchPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_title, launchPendingIntent)
                }
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        } catch (t: Throwable) {
            timber.log.Timber.tag("MusicWidgetProvider").w(t, "Error updating app widgets")
        } finally {
            pendingResult?.finish()
        }
    }
}

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MusicWidgetProvider::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val app = context.applicationContext as? MusicApplication ?: return
        val controller = app.playbackController

        when (intent.action) {
            ACTION_PLAY_PAUSE -> controller.togglePlayPause()
            ACTION_NEXT -> controller.playNext()
            ACTION_PREV -> controller.playPrevious()
        }
        
        if (intent.action in listOf(ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV)) {
            updateAllWidgets(context)
        }
    }
}
