package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.AppThemeMode
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

    private val _appThemeMode = MutableStateFlow(AppThemeMode.NEO_CLASSIC)
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _useBlackTheme = MutableStateFlow(true)
    val useBlackTheme: StateFlow<Boolean> = _useBlackTheme.asStateFlow()

    private val _albumArtStyle = MutableStateFlow(AlbumArtStyle.CD)
    val albumArtStyle: StateFlow<AlbumArtStyle> = _albumArtStyle.asStateFlow()

    private val _libraryAlbumArtStyle = MutableStateFlow(LibraryAlbumArtStyle.ROUND)
    val libraryAlbumArtStyle: StateFlow<LibraryAlbumArtStyle> = _libraryAlbumArtStyle.asStateFlow()

    private val _rotateCd = MutableStateFlow(true)
    val rotateCd: StateFlow<Boolean> = _rotateCd.asStateFlow()

    private val _hideSampleMusic = MutableStateFlow(true)
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
    private val _autoSyncMediaStore = MutableStateFlow(true)
    val autoSyncMediaStore: StateFlow<Boolean> = _autoSyncMediaStore.asStateFlow()

    private val _intelligentSorting = MutableStateFlow(true)
    val intelligentSorting: StateFlow<Boolean> = _intelligentSorting.asStateFlow()

    private val _separators = MutableStateFlow("")
    val separators: StateFlow<String> = _separators.asStateFlow()

    private val _useFileTreeCache = MutableStateFlow(false)
    val useFileTreeCache: StateFlow<Boolean> = _useFileTreeCache.asStateFlow()
    private val _replayGainMode = MutableStateFlow(com.example.ReplayGainMode.DYNAMIC)
    val replayGainMode: StateFlow<com.example.ReplayGainMode> = _replayGainMode.asStateFlow()

    private val _replayGainPreAmpDb = MutableStateFlow(0f)
    val replayGainPreAmpDb: StateFlow<Float> = _replayGainPreAmpDb.asStateFlow()

    private val folderMutex = Mutex()
    // Writes call apply() directly: it is already async and, unlike one coroutine per write, preserves
    // order, so rapid slider changes can no longer persist a stale value.

    init {
            _themeStyle.value = try {
                MusicThemeStyle.valueOf(sharedPrefs.getString("theme_style", MusicThemeStyle.ELEGANT_DARK.name) ?: MusicThemeStyle.ELEGANT_DARK.name)
            } catch (e: Exception) {
                MusicThemeStyle.ELEGANT_DARK
            }
            _appThemeMode.value = try {
                AppThemeMode.valueOf(sharedPrefs.getString("app_theme_mode", AppThemeMode.NEO_CLASSIC.name) ?: AppThemeMode.NEO_CLASSIC.name)
            } catch (e: Exception) {
                AppThemeMode.NEO_CLASSIC
            }
            _useBlackTheme.value = sharedPrefs.getBoolean("use_black_theme", true)
            _albumArtStyle.value = try {
                AlbumArtStyle.valueOf(sharedPrefs.getString("album_art_style", AlbumArtStyle.CD.name) ?: AlbumArtStyle.CD.name)
            } catch (e: Exception) {
                AlbumArtStyle.CD
            }
            _libraryAlbumArtStyle.value = try {
                LibraryAlbumArtStyle.valueOf(sharedPrefs.getString("library_album_art_style", LibraryAlbumArtStyle.ROUND.name) ?: LibraryAlbumArtStyle.ROUND.name)
            } catch (e: Exception) {
                LibraryAlbumArtStyle.ROUND
            }
            _rotateCd.value = sharedPrefs.getBoolean("rotate_cd", true)
            _hideSampleMusic.value = sharedPrefs.getBoolean("hide_sample_music", true)
            _liquidPalette.value = sharedPrefs.getBoolean("liquid_palette", true)
            _sortOption.value = sharedPrefs.getString("sort_option", "Filename") ?: "Filename"
            // "Genres" was a duplicate name for the Genre tab; fold it in and drop repeats.
            _libraryTabs.value = (sharedPrefs.getString("library_tabs_v2", defaultTabs) ?: defaultTabs).split(",")
                .filter { it.isNotBlank() }
                .map { if (it == "Genres") "Genre" else it }
                .distinct()
            _includedFolders.value = sharedPrefs.getStringSet("included_folders", emptySet()) ?: emptySet()
            _excludedFolders.value = sharedPrefs.getStringSet("excluded_folders", emptySet()) ?: emptySet()
            _autoRefreshEnabled.value = sharedPrefs.getBoolean("auto_refresh_enabled", true)
            _autoSyncMediaStore.value = sharedPrefs.getBoolean("auto_sync_mediastore", true)
            _intelligentSorting.value = sharedPrefs.getBoolean("intelligent_sorting", true)
            _separators.value = sharedPrefs.getString("separators", "") ?: ""
            _useFileTreeCache.value = sharedPrefs.getBoolean("use_file_tree_cache", false)
            _replayGainMode.value = try {
                com.example.ReplayGainMode.valueOf(
                    sharedPrefs.getString("replaygain_mode", com.example.ReplayGainMode.DYNAMIC.name) ?: com.example.ReplayGainMode.DYNAMIC.name
                )
            } catch (e: Exception) {
                com.example.ReplayGainMode.DYNAMIC
            }
            _replayGainPreAmpDb.value = sharedPrefs.getFloat("replaygain_preamp_db", 0f)
    }

    fun setThemeStyle(style: MusicThemeStyle) {
        _themeStyle.value = style
        sharedPrefs.edit().putString("theme_style", style.name).apply()
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        _appThemeMode.value = mode
        sharedPrefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    fun setUseBlackTheme(enabled: Boolean) {
        _useBlackTheme.value = enabled
        sharedPrefs.edit().putBoolean("use_black_theme", enabled).apply()
    }

    fun setAlbumArtStyle(style: AlbumArtStyle) {
        _albumArtStyle.value = style
        sharedPrefs.edit().putString("album_art_style", style.name).apply()
    }

    fun setLibraryAlbumArtStyle(style: LibraryAlbumArtStyle) {
        _libraryAlbumArtStyle.value = style
        sharedPrefs.edit().putString("library_album_art_style", style.name).apply()
    }

    fun setRotateCd(rotate: Boolean) {
        _rotateCd.value = rotate
        sharedPrefs.edit().putBoolean("rotate_cd", rotate).apply()
    }

    fun setLiquidPalette(enabled: Boolean) {
        _liquidPalette.value = enabled
        sharedPrefs.edit().putBoolean("liquid_palette", enabled).apply()
    }

    fun setHideSampleMusic(hide: Boolean) {
        _hideSampleMusic.value = hide
        sharedPrefs.edit().putBoolean("hide_sample_music", hide).apply()
    }

    fun setSortOption(option: String) {
        _sortOption.value = option
        sharedPrefs.edit().putString("sort_option", option).apply()
    }

    fun setLibraryTabs(tabs: List<String>) {
        _libraryTabs.value = tabs
        sharedPrefs.edit().putString("library_tabs_v2", tabs.joinToString(",")).apply()
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
        sharedPrefs.edit().putBoolean("auto_refresh_enabled", enabled).apply()
    }
    fun setAutoSyncMediaStore(enabled: Boolean) {
        _autoSyncMediaStore.value = enabled
        sharedPrefs.edit().putBoolean("auto_sync_mediastore", enabled).apply()
    }

    fun setIntelligentSorting(enabled: Boolean) {
        _intelligentSorting.value = enabled
        sharedPrefs.edit().putBoolean("intelligent_sorting", enabled).apply()
    }

    fun setSeparators(separators: String) {
        _separators.value = separators
        sharedPrefs.edit().putString("separators", separators).apply()
    }

    fun setUseFileTreeCache(enabled: Boolean) {
        _useFileTreeCache.value = enabled
        sharedPrefs.edit().putBoolean("use_file_tree_cache", enabled).apply()
    }
    fun setReplayGainMode(mode: com.example.ReplayGainMode) {
        _replayGainMode.value = mode
        sharedPrefs.edit().putString("replaygain_mode", mode.name).apply()
    }

    fun setReplayGainPreAmpDb(db: Float) {
        _replayGainPreAmpDb.value = db
        sharedPrefs.edit().putFloat("replaygain_preamp_db", db).apply()
    }
}
