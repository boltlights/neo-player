package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IndexingState
import com.example.data.MusicDatabase
import com.example.data.PlaylistEntity
import com.example.data.TrackEntity
import com.example.viewmodel.LibraryViewModel
import com.example.viewmodel.PlaybackViewModel
import com.example.viewmodel.PlaylistViewModel
import com.example.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class Screen {
    LIBRARY,
    PLAYLISTS,
    SETTINGS
}

enum class AlbumArtStyle(val displayName: String) {
    CD("CD"),
    ROUNDED("Rounded"),
    SQUARE("Square"),
    COOKIE("Cookie"),
    SOFT_BURST("Soft Burst")
}

enum class LibraryAlbumArtStyle(val displayName: String) {
    ROUND("Round"),
    SQUARE("Square"),
    COOKIE("Cookie"),
    SOFT_BURST("Soft Burst")
}

enum class MusicThemeStyle(
    val displayName: String,
    val background: Long,
    val surface: Long,
    val primary: Long,
    val accent: Long,
    val onBackground: Long
) {
    ELEGANT_DARK("Elegant Dark", 0xFF000000, 0xFF121318, 0xFFD1E4FF, 0xFFD1E4FF, 0xFFE2E2E6),
    CARBON_MINIMAL("Carbon Minimal", 0xFF000000, 0xFF101011, 0xFFE5E5EA, 0xFF2F80ED, 0xFFF2F2F7),
    COSMIC_MIDNIGHT("Cosmic Midnight", 0xFF000000, 0xFF0D0B1F, 0xFFD0BCFF, 0xFFFF007F, 0xFFECEBFF),
    OBSIDIAN_TEAL("Obsidian Teal", 0xFF000000, 0xFF121212, 0xFF00ADB5, 0xFF00ADB5, 0xFFEEEEEE),
    CRIMSON_EDGE("Crimson Edge", 0xFF000000, 0xFF150A0A, 0xFFFF5E62, 0xFFE74C3C, 0xFFFCF5F5),
    CYBERPUNK_DARK("Cyberpunk Neo", 0xFF000000, 0xFF10111A, 0xFFF3EC1E, 0xFF00FFCC, 0xFF00FFCC),
    DYNAMIC_SYSTEM("Material You", 0xFF000000, 0xFF121318, 0xFFD1E4FF, 0xFFD1E4FF, 0xFFE2E2E6)
}

/**
 * Top-level coordinator ViewModel.
 * Decomposes business logic into 5 domain-specific ViewModels:
 * - [LibraryViewModel]
 * - [PlaybackViewModel]
 * - [SettingsViewModel]
 * - [PlaylistViewModel]
 *
 * Forwards calls and states to maintain seamless backward compatibility with all UI screens.
 */
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val musicApp = application as MusicApplication
    private val database = MusicDatabase.getDatabase(context)
    private val repository = musicApp.repository
    private val settingsRepository = musicApp.settingsRepository
    private val mediaStoreManager = musicApp.mediaStoreManager
    val playbackController = musicApp.playbackController

    // Domain ViewModels
    val library = LibraryViewModel(application, repository, settingsRepository, mediaStoreManager, playbackController, viewModelScope)
    val playback = PlaybackViewModel(application, repository, playbackController, viewModelScope)
    val settings = SettingsViewModel(application, settingsRepository, viewModelScope)
    val playlist = PlaylistViewModel(application, repository, viewModelScope)


    // Playlists
    val allPlaylists get() = playlist.allPlaylists
    val allPlaylistTrackCrossRefs get() = playlist.allPlaylistTrackCrossRefs
    val selectedPlaylistId get() = playlist.selectedPlaylistId
    val playlistTracks get() = playlist.playlistTracks
    val activePlaylistForViewer get() = playlist.activePlaylistForViewer
    val playlistViewModel get() = playlist
    fun selectPlaylist(id: Int?) = playlist.selectPlaylist(id)
    fun setActivePlaylistForViewer(p: PlaylistEntity?) = playlist.setActivePlaylistForViewer(p)
    fun createPlaylist(name: String) = playlist.createPlaylist(name)
    fun renamePlaylist(p: PlaylistEntity, newName: String) = playlist.renamePlaylist(p, newName)
    fun updatePlaylistCoverArt(p: PlaylistEntity, uri: String?) = playlist.updatePlaylistCoverArt(p, uri)
    fun deletePlaylist(playlistId: Int) = playlist.deletePlaylist(playlistId)
    fun addTrackToPlaylist(playlistId: Int, trackId: Int) = playlist.addTrackToPlaylist(playlistId, trackId)
    fun addTracksToPlaylist(playlistId: Int, trackIds: List<Int>) = playlist.addTracksToPlaylist(playlistId, trackIds)
    fun removeTrackFromPlaylist(playlistId: Int, trackId: Int) = playlist.removeTrackFromPlaylist(playlistId, trackId)
    fun exportPlaylistToM3u(context: Context, p: PlaylistEntity, tracks: List<TrackEntity>, uri: Uri, onResult: (Boolean, String) -> Unit) =
        playlist.exportPlaylistToM3u(context, p, tracks, uri, onResult)
    fun importPlaylistFromM3u(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) =
        playlist.importPlaylistFromM3u(context, uri, onResult)
    fun getDuplicateTrackCount(playlistId: Int, newTrackIds: List<Int>): Int =
        playlist.getDuplicateTrackCount(playlistId, newTrackIds)

    // Library & Tracks
    val allTracks get() = library.allTracks
    val searchQuery get() = library.searchQuery
    val sortOption get() = library.sortOption
    val libraryTabs get() = library.libraryTabs
    val recentlyAdded get() = library.recentlyAdded
    val mostPlayed get() = library.mostPlayed
    val recentlyPlayed get() = library.recentlyPlayed
    val sortedTracks get() = library.sortedTracks
    val searchResultTracks get() = library.searchResultTracks
    val indexedAlbums get() = library.indexedAlbums
    val indexedArtists get() = library.indexedArtists
    val indexedGenres get() = library.indexedGenres
    val isImporting get() = library.isImporting
    val importProgress get() = library.importProgress
    val deleteIntentSender get() = library.deleteIntentSender
    fun setSearchQuery(query: String) = library.setSearchQuery(query)
    fun updateSortOption(option: String) = library.updateSortOption(option)
    fun updateLibraryTabs(tabs: List<String>) = library.updateLibraryTabs(tabs)
    fun updateLibraryOrder(orderedTracks: List<TrackEntity>) = library.updateLibraryOrder(orderedTracks)
    fun fetchLyricsIfMissing(track: TrackEntity) = library.fetchLyricsIfMissing(track)
    fun deleteTrack(ctx: Context, track: TrackEntity) = library.deleteTrack(ctx, track) { playback.setPlayerExpanded(false) }
    fun deleteTracks(ctx: Context, tracks: List<TrackEntity>) = library.deleteTracks(ctx, tracks) { playback.setPlayerExpanded(false) }
    fun onDeleteConfirmationResult(granted: Boolean) = library.onDeleteConfirmationResult(context, granted)
    fun scanMediaStore(ctx: Context, onCompleted: (Int) -> Unit) = library.scanMediaStore(ctx, onCompleted)
    fun importAudioFiles(ctx: Context, uris: List<Uri>, onCompleted: (Int) -> Unit) = library.importAudioFiles(ctx, uris, onCompleted)
    fun importAudioFolder(ctx: Context, folderUri: Uri, onCompleted: (Int) -> Unit) = library.importAudioFolder(ctx, folderUri, onCompleted)
    fun readdSampleTracks(ctx: Context, onComplete: () -> Unit = {}) = library.readdSampleTracks(ctx, onComplete)

    // Settings
    val themeStyle get() = settings.musicThemeStyle
    val musicThemeStyle get() = settings.musicThemeStyle
    val appThemeMode get() = settings.appThemeMode
    val useBlackTheme get() = settings.useBlackTheme
    val isAmoledEnabled get() = settings.isAmoledEnabled
    val albumArtStyle get() = settings.albumArtStyle
    val libraryAlbumArtStyle get() = settings.libraryAlbumArtStyle
    val rotateCd get() = settings.rotateCd
    val liquidPaletteEnabled get() = settings.liquidPaletteEnabled
    val hideSampleMusicButton get() = settings.hideSampleMusicButton
    val includedFolders get() = settings.includedFolders
    val excludedFolders get() = settings.excludedFolders
    val autoRefreshEnabled get() = settings.autoRefreshEnabled
    val autoSyncMediaStore get() = settings.autoSyncMediaStore
    val intelligentSorting get() = settings.intelligentSorting
    val separators get() = settings.separators
    val useFileTreeCache get() = settings.useFileTreeCache
    val replayGainMode get() = settings.replayGainMode
    val replayGainPreAmpDb get() = settings.replayGainPreAmpDb

    fun setReplayGainMode(mode: com.example.ReplayGainMode) = settings.setReplayGainMode(mode)
    fun setReplayGainPreAmpDb(db: Float) = settings.setReplayGainPreAmpDb(db)

    fun changeThemeStyle(style: MusicThemeStyle) = settings.changeThemeStyle(style)
    fun setAppThemeMode(mode: AppThemeMode) = settings.changeAppThemeMode(mode)
    fun setUseBlackTheme(enabled: Boolean) = settings.setUseBlackTheme(enabled)
    fun setAlbumArtStyle(style: AlbumArtStyle) = settings.setAlbumArtStyle(style)
    fun setLibraryAlbumArtStyle(style: LibraryAlbumArtStyle) = settings.setLibraryAlbumArtStyle(style)
    fun setRotateCd(rotate: Boolean) = settings.setRotateCd(rotate)
    fun setLiquidPaletteEnabled(enabled: Boolean) = settings.setLiquidPaletteEnabled(enabled)
    fun setHideSampleMusicButton(hide: Boolean) = settings.setHideSampleMusicButton(hide)
    fun addIncludedFolder(uri: String) = settings.addIncludedFolder(uri)
    fun removeIncludedFolder(uri: String) = settings.removeIncludedFolder(uri)
    fun addExcludedFolder(uri: String) = settings.addExcludedFolder(uri)
    fun removeExcludedFolder(uri: String) = settings.removeExcludedFolder(uri)
    fun setAutoRefreshEnabled(enabled: Boolean) = settings.setAutoRefreshEnabled(enabled)
    fun setAutoSyncMediaStore(enabled: Boolean) = settings.setAutoSyncMediaStore(enabled)
    fun setIntelligentSorting(enabled: Boolean) = settings.setIntelligentSorting(enabled)
    fun setSeparators(separators: String) = settings.setSeparators(separators)
    fun setUseFileTreeCache(enabled: Boolean) = settings.setUseFileTreeCache(enabled)

    // Playback State & UI Overlay
    val currentScreen get() = playback.currentScreen
    val isPlayerExpanded get() = playback.isPlayerExpanded
    val currentBgColor get() = playback.currentBgColor

    fun selectScreen(screen: Screen) = playback.selectScreen(screen)
    fun setPlayerExpanded(expanded: Boolean) = playback.setPlayerExpanded(expanded)
    fun updatePalette(bitmap: android.graphics.Bitmap?) = playback.updatePalette(bitmap)

    fun performAutoRefresh() {
        val enabled = autoRefreshEnabled.value
        if (enabled && includedFolders.value.isNotEmpty()) {
            scanMediaStore(context) { }
        }
    }

    private var mediaStoreObserver: android.database.ContentObserver? = null
    private fun registerMediaStoreObserver() {
        if (mediaStoreObserver == null) {
            mediaStoreObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                private var debounceJob: kotlinx.coroutines.Job? = null
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    if (!autoSyncMediaStore.value) return
                    debounceJob?.cancel()
                    debounceJob = viewModelScope.launch {
                        delay(2000)
                        if (repository.indexingState.value !is IndexingState.Indexing) {
                            scanMediaStore(context) {}
                        }
                    }
                }
            }
            try {
                context.contentResolver.registerContentObserver(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mediaStoreObserver!!
                )
            } catch (e: Exception) {
                timber.log.Timber.tag("MusicViewModel").w(e, "Failed to register MediaStore observer")
            }
        }
    }

    init {
        viewModelScope.launch {
            performAutoRefresh()
        }

        // Sync ReplayGain settings to the playback controller
        playbackController.replayGainProcessor.mode = replayGainMode.value
        playbackController.replayGainProcessor.preAmpDb = replayGainPreAmpDb.value

        viewModelScope.launch {
            replayGainMode.collect {
                playbackController.replayGainProcessor.mode = it
            }
        }
        viewModelScope.launch {
            replayGainPreAmpDb.collect {
                playbackController.replayGainProcessor.preAmpDb = it
            }
        }

        registerMediaStoreObserver()

        viewModelScope.launch(Dispatchers.IO) {
            playbackController.currentTrack.collect { track ->
                if (track?.artPath != null) {
                    val realArt = track.artPath!!.replaceFirst("audiocover://", "content://")
                    try {
                        var bitmap: android.graphics.Bitmap? = null
                        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                            inSampleSize = 4
                            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                        }
                        if (realArt.startsWith("content://")) {
                            val retriever = android.media.MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, Uri.parse(realArt))
                                val artBytes = retriever.embeddedPicture
                                if (artBytes != null) {
                                    bitmap = android.graphics.BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, decodeOptions)
                                }
                            } catch (e: Exception) {
                                // ignore
                            } finally {
                                try { retriever.release() } catch (e: Exception) {}
                            }
                        } else {
                            val file = File(realArt)
                            if (file.exists()) {
                                bitmap = android.graphics.BitmapFactory.decodeFile(realArt, decodeOptions)
                            }
                        }
                        updatePalette(bitmap)
                    } catch (e: Exception) {
                        updatePalette(null)
                    }
                } else {
                    updatePalette(null)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaStoreObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
