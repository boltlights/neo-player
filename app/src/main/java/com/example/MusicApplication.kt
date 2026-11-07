package com.example

import android.app.Application
import com.example.data.MusicDatabase
import com.example.data.MusicRepository
import com.example.data.SettingsRepository
import com.example.data.SongRepository
import com.example.util.MediaStoreManager

/**
 * Dependency container interface defining the application's core singletons.
 * Provides clean dependency inversion, testability, and modularity.
 */
interface AppContainer {
    val playbackController: PlaybackController
    val repository: MusicRepository
    val songRepository: SongRepository
    val settingsRepository: SettingsRepository
    val mediaStoreManager: MediaStoreManager
}

class MusicApplication : Application(), AppContainer, coil.ImageLoaderFactory {
    
    override lateinit var playbackController: PlaybackController
        private set
    override lateinit var repository: MusicRepository
        private set
    override lateinit var songRepository: SongRepository
        private set
    override lateinit var settingsRepository: SettingsRepository
        private set
    override lateinit var mediaStoreManager: MediaStoreManager
        private set

    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base)
        if (base != null) {
            com.example.util.CrashReporter.init(base)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            timber.log.Timber.plant(timber.log.Timber.DebugTree())
        }
        com.example.util.CrashReporter.init(this)
        playbackController = PlaybackController(this)
        repository = MusicRepository(MusicDatabase.getDatabase(this).musicDao())
        songRepository = SongRepository(this)
        settingsRepository = SettingsRepository(this)
        mediaStoreManager = MediaStoreManager(repository, settingsRepository)
    }

    override fun newImageLoader(): coil.ImageLoader {
        return coil.ImageLoader.Builder(this)
            .components {
                add(com.example.util.AudioAlbumArtFetcher.Factory(this@MusicApplication))
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(128 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        com.example.util.CrashReporter.addBreadcrumb("onTrimMemory: level=$level")
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            try {
                coil.Coil.imageLoader(this).memoryCache?.clear()
            } catch (_: Throwable) {}
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        com.example.util.CrashReporter.addBreadcrumb("onLowMemory triggered by OS")
        try {
            coil.Coil.imageLoader(this).memoryCache?.clear()
        } catch (_: Throwable) {}
    }
}
