package com.example

import android.app.NotificationChannel
import com.example.R
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.DefaultMediaNotificationProvider
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@UnstableApi
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    companion object {
        const val CHANNEL_ID = "ambient_music_playback_channel"
        const val ROOT_ID = "root_id"
    }

    override fun onCreate() {
        super.onCreate()
        initializeSession()
    }

    private fun initializeSession() {
        if (mediaSession != null) return

        val controller = (application as MusicApplication).playbackController
        controller.ensurePlayer()
        val exoPlayer = controller.exoPlayer ?: return

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, null))
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val app = application as MusicApplication
                val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<MediaItem>>()
                serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val trackId = mediaId.toIntOrNull()
                        if (trackId != null) {
                            val track = app.repository.getTrackById(trackId)
                            if (track != null) {
                                val item = MediaItem.Builder()
                                    .setMediaId(track.id.toString())
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(track.title)
                                            .setArtist(track.artist)
                                            .setAlbumTitle(track.album)
                                            .setIsPlayable(true)
                                            .build()
                                    )
                                    .build()
                                future.set(LibraryResult.ofItem(item, null))
                            } else {
                                future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                            }
                        } else {
                            future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                    }
                }
                return future
            }

            override fun onSearch(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<Void>> {
                return com.google.common.util.concurrent.Futures.immediateFuture(LibraryResult.ofVoid())
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                if (parentId == ROOT_ID) {
                    val app = application as MusicApplication
                    val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            val tracks = app.repository.allTracks.first()
                            val fromIndex = page * pageSize
                            val toIndex = minOf(fromIndex + pageSize.toLong(), tracks.size.toLong()).toInt()
                            val pagedTracks = if (fromIndex < tracks.size) tracks.subList(fromIndex, toIndex) else emptyList()
                            val children = mutableListOf<MediaItem>()
                            pagedTracks.forEach { track ->
                                children.add(
                                    MediaItem.Builder()
                                        .setMediaId(track.id.toString())
                                        .setUri(track.filePath)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(track.title)
                                                .setArtist(track.artist)
                                                .setAlbumTitle(track.album)
                                                .setArtworkUri(if (track.artPath.isNullOrEmpty()) null else android.net.Uri.parse(track.artPath))
                                                .build()
                                        )
                                        .build()
                                )
                            }
                            future.set(LibraryResult.ofItemList(children, null))
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            future.setException(e)
                        }
                    }
                    return future
                }
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), null))
            }
        }

        // Install our notification provider *before* the session exists. It used to be set after
        // addSession(), by which time Media3 had already picked its default provider, so playback
        // notifications went to Media3's "default_channel_id" and this app's channel was never used.
        createNotificationChannel()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
        setMediaNotificationProvider(notificationProvider)

        val session = MediaLibrarySession.Builder(this, exoPlayer, callback)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(com.example.util.CoilBitmapLoader(this))
            .build()

        mediaSession = session
        addSession(session)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            // Remove the stale Media3 default channel left behind by older builds, so the system
            // notification settings show a single "Music Playback" channel.
            if (manager.getNotificationChannel("default_channel_id") != null) {
                manager.deleteNotificationChannel("default_channel_id")
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        if (mediaSession == null) {
            initializeSession()
        }
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.let {
            removeSession(it)
            it.release()
        }
        mediaSession = null
        serviceJob.cancel()
        super.onDestroy()
    }
}
