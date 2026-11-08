package com.example.viewmodel

import android.app.Application
import com.example.AlbumArtStyle
import com.example.AppThemeMode
import com.example.LibraryAlbumArtStyle
import com.example.MusicThemeStyle
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Domain-specific ViewModel handling theme, preferences, folder inclusions, and audio playback settings.
 */
class SettingsViewModel(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    val musicThemeStyle: StateFlow<MusicThemeStyle> = settingsRepository.themeStyle
    val appThemeMode: StateFlow<AppThemeMode> = settingsRepository.appThemeMode
    val useBlackTheme: StateFlow<Boolean> = settingsRepository.useBlackTheme
    val isAmoledEnabled: StateFlow<Boolean> = settingsRepository.useBlackTheme
    val albumArtStyle: StateFlow<AlbumArtStyle> = settingsRepository.albumArtStyle
    val libraryAlbumArtStyle: StateFlow<LibraryAlbumArtStyle> = settingsRepository.libraryAlbumArtStyle
    val rotateCd: StateFlow<Boolean> = settingsRepository.rotateCd
    val liquidPaletteEnabled: StateFlow<Boolean> = settingsRepository.liquidPalette
    val hideSampleMusicButton: StateFlow<Boolean> = settingsRepository.hideSampleMusic
    val sortOption: StateFlow<String> = settingsRepository.sortOption
    val libraryTabs: StateFlow<List<String>> = settingsRepository.libraryTabs

    val includedFolders: StateFlow<Set<String>> = settingsRepository.includedFolders
    val excludedFolders: StateFlow<Set<String>> = settingsRepository.excludedFolders
    val autoRefreshEnabled: StateFlow<Boolean> = settingsRepository.autoRefreshEnabled
    val autoSyncMediaStore: StateFlow<Boolean> = settingsRepository.autoSyncMediaStore
    val intelligentSorting: StateFlow<Boolean> = settingsRepository.intelligentSorting
    val separators: StateFlow<String> = settingsRepository.separators
    val useFileTreeCache: StateFlow<Boolean> = settingsRepository.useFileTreeCache
    val replayGainMode: StateFlow<com.example.ReplayGainMode> = settingsRepository.replayGainMode
    val replayGainPreAmpDb: StateFlow<Float> = settingsRepository.replayGainPreAmpDb

    fun changeThemeStyle(style: MusicThemeStyle) {
        scope.launch { settingsRepository.setThemeStyle(style) }
    }

    fun changeAppThemeMode(mode: AppThemeMode) {
        scope.launch { settingsRepository.setAppThemeMode(mode) }
    }

    fun setUseBlackTheme(enabled: Boolean) {
        scope.launch { settingsRepository.setUseBlackTheme(enabled) }
    }

    fun setAlbumArtStyle(style: AlbumArtStyle) {
        scope.launch { settingsRepository.setAlbumArtStyle(style) }
    }

    fun setLibraryAlbumArtStyle(style: LibraryAlbumArtStyle) {
        scope.launch { settingsRepository.setLibraryAlbumArtStyle(style) }
    }

    fun setRotateCd(rotate: Boolean) {
        scope.launch { settingsRepository.setRotateCd(rotate) }
    }

    fun setLiquidPaletteEnabled(enabled: Boolean) {
        scope.launch { settingsRepository.setLiquidPalette(enabled) }
    }

    fun setHideSampleMusicButton(hide: Boolean) {
        scope.launch { settingsRepository.setHideSampleMusic(hide) }
    }

    fun updateSortOption(option: String) {
        scope.launch { settingsRepository.setSortOption(option) }
    }

    fun updateLibraryTabs(tabs: List<String>) {
        scope.launch { settingsRepository.setLibraryTabs(tabs) }
    }

    fun addIncludedFolder(uri: String) {
        scope.launch { settingsRepository.addIncludedFolder(uri) }
    }

    fun removeIncludedFolder(uri: String) {
        scope.launch { settingsRepository.removeIncludedFolder(uri) }
    }

    fun addExcludedFolder(uri: String) {
        scope.launch { settingsRepository.addExcludedFolder(uri) }
    }

    fun removeExcludedFolder(uri: String) {
        scope.launch { settingsRepository.removeExcludedFolder(uri) }
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        scope.launch { settingsRepository.setAutoRefreshEnabled(enabled) }
    }



    fun setAutoSyncMediaStore(enabled: Boolean) {
        scope.launch { settingsRepository.setAutoSyncMediaStore(enabled) }
    }

    fun setIntelligentSorting(enabled: Boolean) {
        scope.launch { settingsRepository.setIntelligentSorting(enabled) }
    }

    fun setSeparators(separators: String) {
        scope.launch { settingsRepository.setSeparators(separators) }
    }

    fun setUseFileTreeCache(enabled: Boolean) {
        scope.launch { settingsRepository.setUseFileTreeCache(enabled) }
    }

    fun setReplayGainMode(mode: com.example.ReplayGainMode) {
        scope.launch { settingsRepository.setReplayGainMode(mode) }
    }

    fun setReplayGainPreAmpDb(db: Float) {
        scope.launch { settingsRepository.setReplayGainPreAmpDb(db) }
    }
}
