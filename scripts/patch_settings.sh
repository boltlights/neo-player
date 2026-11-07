#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/data/SettingsRepository.kt
package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.MusicThemeStyle
import com.example.AlbumArtStyle
import com.example.LibraryAlbumArtStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsRepository(context: Context) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("music_player_prefs", Context.MODE_PRIVATE)

    private val _themeStyle = MutableStateFlow(MusicThemeStyle.ELEGANT_DARK)
    val themeStyle: StateFlow<MusicThemeStyle> = _themeStyle.asStateFlow()

    private val _albumArtStyle = MutableStateFlow(AlbumArtStyle.CD)
    val albumArtStyle: StateFlow<AlbumArtStyle> = _albumArtStyle.asStateFlow()

    private val _libraryAlbumArtStyle = MutableStateFlow(LibraryAlbumArtStyle.ROUND)
    val libraryAlbumArtStyle: StateFlow<LibraryAlbumArtStyle> = _libraryAlbumArtStyle.asStateFlow()

    private val _rotateCd = MutableStateFlow(true)
    val rotateCd: StateFlow<Boolean> = _rotateCd.asStateFlow()

    private val _hideSampleMusic = MutableStateFlow(false)
    val hideSampleMusic: StateFlow<Boolean> = _hideSampleMusic.asStateFlow()

    private val _liquidPalette = MutableStateFlow(true)
    val liquidPalette: StateFlow<Boolean> = _liquidPalette.asStateFlow()

    private val _sortOption = MutableStateFlow("Filename")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    private val defaultTabs = "Songs,Genre,Artists,Playlists"
    private val _libraryTabs = MutableStateFlow(defaultTabs.split(","))
    val libraryTabs: StateFlow<List<String>> = _libraryTabs.asStateFlow()

    private val _includedFolders = MutableStateFlow<Set<String>>(emptySet())
    val includedFolders: StateFlow<Set<String>> = _includedFolders.asStateFlow()

    private val _excludedFolders = MutableStateFlow<Set<String>>(emptySet())
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    private val _autoRefreshEnabled = MutableStateFlow(true)
    val autoRefreshEnabled: StateFlow<Boolean> = _autoRefreshEnabled.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(true)
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDuration = MutableStateFlow(3)
    val crossfadeDuration: StateFlow<Int> = _crossfadeDuration.asStateFlow()

    private val _autoSyncMediaStore = MutableStateFlow(true)
    val autoSyncMediaStore: StateFlow<Boolean> = _autoSyncMediaStore.asStateFlow()

    private val folderMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            _themeStyle.value = MusicThemeStyle.valueOf(sharedPrefs.getString("theme_style", MusicThemeStyle.ELEGANT_DARK.name) ?: MusicThemeStyle.ELEGANT_DARK.name)
            _albumArtStyle.value = AlbumArtStyle.valueOf(sharedPrefs.getString("album_art_style", AlbumArtStyle.CD.name) ?: AlbumArtStyle.CD.name)
            _libraryAlbumArtStyle.value = LibraryAlbumArtStyle.valueOf(sharedPrefs.getString("library_album_art_style", LibraryAlbumArtStyle.ROUND.name) ?: LibraryAlbumArtStyle.ROUND.name)
            _rotateCd.value = sharedPrefs.getBoolean("rotate_cd", true)
            _hideSampleMusic.value = sharedPrefs.getBoolean("hide_sample_music", false)
            _liquidPalette.value = sharedPrefs.getBoolean("liquid_palette", true)
            _sortOption.value = sharedPrefs.getString("sort_option", "Filename") ?: "Filename"
            _libraryTabs.value = (sharedPrefs.getString("library_tabs_v2", defaultTabs) ?: defaultTabs).split(",").filter { it.isNotBlank() }
            _includedFolders.value = sharedPrefs.getStringSet("included_folders", emptySet()) ?: emptySet()
            _excludedFolders.value = sharedPrefs.getStringSet("excluded_folders", emptySet()) ?: emptySet()
            _autoRefreshEnabled.value = sharedPrefs.getBoolean("auto_refresh_enabled", true)
            _gaplessPlayback.value = sharedPrefs.getBoolean("gapless_playback", true)
            _crossfadeEnabled.value = sharedPrefs.getBoolean("crossfade_enabled", false)
            _crossfadeDuration.value = sharedPrefs.getInt("crossfade_duration", 3)
            _autoSyncMediaStore.value = sharedPrefs.getBoolean("auto_sync_mediastore", true)
        }
    }

    fun setThemeStyle(style: MusicThemeStyle) {
        _themeStyle.value = style
        scope.launch { sharedPrefs.edit().putString("theme_style", style.name).apply() }
    }

    fun setAlbumArtStyle(style: AlbumArtStyle) {
        _albumArtStyle.value = style
        scope.launch { sharedPrefs.edit().putString("album_art_style", style.name).apply() }
    }

    fun setLibraryAlbumArtStyle(style: LibraryAlbumArtStyle) {
        _libraryAlbumArtStyle.value = style
        scope.launch { sharedPrefs.edit().putString("library_album_art_style", style.name).apply() }
    }

    fun setRotateCd(rotate: Boolean) {
        _rotateCd.value = rotate
        scope.launch { sharedPrefs.edit().putBoolean("rotate_cd", rotate).apply() }
    }

    fun setLiquidPalette(enabled: Boolean) {
        _liquidPalette.value = enabled
        scope.launch { sharedPrefs.edit().putBoolean("liquid_palette", enabled).apply() }
    }

    fun setHideSampleMusic(hide: Boolean) {
        _hideSampleMusic.value = hide
        scope.launch { sharedPrefs.edit().putBoolean("hide_sample_music", hide).apply() }
    }

    fun setSortOption(option: String) {
        _sortOption.value = option
        scope.launch { sharedPrefs.edit().putString("sort_option", option).apply() }
    }

    fun setLibraryTabs(tabs: List<String>) {
        _libraryTabs.value = tabs
        scope.launch { sharedPrefs.edit().putString("library_tabs_v2", tabs.joinToString(",")).apply() }
    }

    suspend fun addIncludedFolder(uri: String) {
        folderMutex.withLock {
            val newSet = _includedFolders.value.toMutableSet().apply { add(uri) }
            _includedFolders.value = newSet
            sharedPrefs.edit().putStringSet("included_folders", newSet).apply()
        }
    }

    suspend fun removeIncludedFolder(uri: String) {
        folderMutex.withLock {
            val newSet = _includedFolders.value.toMutableSet().apply { remove(uri) }
            _includedFolders.value = newSet
            sharedPrefs.edit().putStringSet("included_folders", newSet).apply()
        }
    }

    suspend fun addExcludedFolder(uri: String) {
        folderMutex.withLock {
            val newSet = _excludedFolders.value.toMutableSet().apply { add(uri) }
            _excludedFolders.value = newSet
            sharedPrefs.edit().putStringSet("excluded_folders", newSet).apply()
        }
    }

    suspend fun removeExcludedFolder(uri: String) {
        folderMutex.withLock {
            val newSet = _excludedFolders.value.toMutableSet().apply { remove(uri) }
            _excludedFolders.value = newSet
            sharedPrefs.edit().putStringSet("excluded_folders", newSet).apply()
        }
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        _autoRefreshEnabled.value = enabled
        scope.launch { sharedPrefs.edit().putBoolean("auto_refresh_enabled", enabled).apply() }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        scope.launch { sharedPrefs.edit().putBoolean("gapless_playback", enabled).apply() }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        scope.launch { sharedPrefs.edit().putBoolean("crossfade_enabled", enabled).apply() }
    }

    fun setCrossfadeDuration(durationSeconds: Int) {
        _crossfadeDuration.value = durationSeconds
        scope.launch { sharedPrefs.edit().putInt("crossfade_duration", durationSeconds).apply() }
    }

    fun setAutoSyncMediaStore(enabled: Boolean) {
        _autoSyncMediaStore.value = enabled
        scope.launch { sharedPrefs.edit().putBoolean("auto_sync_mediastore", enabled).apply() }
    }
}
INNER_EOF
