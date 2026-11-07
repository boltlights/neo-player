package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import com.example.data.PlaylistEntity
import com.example.data.TrackEntity
import com.example.ui.components.ActiveBottomBar
import com.example.ui.screens.FocusedNowPlayingScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.Typography
import androidx.compose.ui.draw.clip
import com.example.ui.components.ShuffleIcon

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.CrashReporter.addBreadcrumb("MainActivity.onCreate")
        enableEdgeToEdge()

        val viewModel = androidx.lifecycle.ViewModelProvider(
            this,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MusicViewModel::class.java]

        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.isPlayerExpanded.value) {
                viewModel.setPlayerExpanded(false)
            } else if (viewModel.activePlaylistForViewer.value != null) {
                viewModel.setActivePlaylistForViewer(null)
            } else if (viewModel.currentScreen.value != Screen.LIBRARY) {
                viewModel.selectScreen(Screen.LIBRARY)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }

        setContent {
            @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
            val context = LocalContext.current
            val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            // Request Notification Permission on Android 13+ to allow Quick Settings Media Player to show up
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }
                LaunchedEffect(Unit) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            val deleteLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    // The user granted permission, the file is usually deleted automatically or we can retry
                    viewModel.performAutoRefresh()
                }
            }
            LaunchedEffect(viewModel) {
                viewModel.deleteIntentSender.collect { intentSender ->
                    deleteLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
            val themeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
            val useBlackTheme by viewModel.useBlackTheme.collectAsStateWithLifecycle()
            val isSAndAbove = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            val isDark = isSystemInDarkTheme()

            val customColorScheme = if (themeStyle == MusicThemeStyle.DYNAMIC_SYSTEM && isSAndAbove) {
                if (isDark) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            } else {
                val bg = Color(themeStyle.background)
                val surf = Color(themeStyle.surface)
                val prim = Color(themeStyle.primary)
                val onBg = Color(themeStyle.onBackground)
                darkColorScheme(
                    background = bg,
                    surface = surf,
                    primary = prim,
                    onBackground = onBg,
                    onSurface = onBg,
                    surfaceVariant = surf.copy(alpha = 0.8f)
                )
            }

            // Map custom theme colors
            val bgColor = customColorScheme.background
            val surfaceColor = customColorScheme.surface
            val primaryColor = customColorScheme.primary
            val accentColor = if (themeStyle == MusicThemeStyle.DYNAMIC_SYSTEM && isSAndAbove) {
                customColorScheme.secondary
            } else {
                Color(themeStyle.accent)
            }
            val onBgColor = customColorScheme.onBackground

            MaterialTheme(
                colorScheme = customColorScheme,
                typography = Typography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreenLayout(viewModel, primaryColor, accentColor, surfaceColor, onBgColor, isExpanded)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreenLayout(
    viewModel: MusicViewModel,
    primaryColor: Color,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    isExpanded: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val allPlaylistTrackCrossRefs by viewModel.allPlaylistTrackCrossRefs.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val searchResultTracks by viewModel.searchResultTracks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playbackController.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playbackController.isPlaying.collectAsStateWithLifecycle()
    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsStateWithLifecycle()
    val hideSampleMusicButton by viewModel.hideSampleMusicButton.collectAsStateWithLifecycle()
    val libraryAlbumArtStyle by viewModel.libraryAlbumArtStyle.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val libraryTabs by viewModel.libraryTabs.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    
    val playerBgColor by viewModel.currentBgColor.collectAsStateWithLifecycle()
    val liquidPaletteEnabled by viewModel.liquidPaletteEnabled.collectAsStateWithLifecycle()

    val view = androidx.compose.ui.platform.LocalView.current
    val currentBg = MaterialTheme.colorScheme.background
    
    if (!view.isInEditMode) {
        val topColor = if (isPlayerExpanded) {
            if (liquidPaletteEnabled) playerBgColor else currentBg
        } else {
            currentBg
        }
        LaunchedEffect(topColor, isPlayerExpanded, liquidPaletteEnabled, currentBg) {
            var ctx = view.context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is android.app.Activity) break
                ctx = ctx.baseContext
            }
            val window = (ctx as? android.app.Activity)?.window ?: return@LaunchedEffect
            val luminance = topColor.luminance()
            val useDarkIcons = luminance > 0.5f
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = useDarkIcons
            insetsController.isAppearanceLightNavigationBars = useDarkIcons
        }
    }

    var showPlaylistCreationDialog by remember { mutableStateOf(false) }
    var expandedTrackForPlaylistAssociation by remember { mutableStateOf<TrackEntity?>(null) }
    val activePlaylistForViewer by viewModel.activePlaylistForViewer.collectAsStateWithLifecycle()
    var trackToShowProperties by remember { mutableStateOf<TrackEntity?>(null) }
    var pendingExportPlaylist by remember { mutableStateOf<Pair<PlaylistEntity, List<TrackEntity>>?>(null) }

    val m3uImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPlaylistFromM3u(context, uri) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val m3uExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        if (uri != null && pendingExportPlaylist != null) {
            val (pl, tracks) = pendingExportPlaylist!!
            viewModel.exportPlaylistToM3u(context, pl, tracks, uri) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            pendingExportPlaylist = null
        }
    }

    // Permission Launcher for Media Store Sync
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted || permissions[android.Manifest.permission.READ_MEDIA_AUDIO] == true || permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            Toast.makeText(context, context.getString(R.string.toast_scanning_mediastore), Toast.LENGTH_SHORT).show()
            viewModel.scanMediaStore(context) { count ->
                if (count > 0) {
                    Toast.makeText(context, context.getString(R.string.toast_synced_songs, count), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_no_new_audio), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.toast_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Toast.makeText(context, context.getString(R.string.toast_importing_folder), Toast.LENGTH_SHORT).show()
            viewModel.importAudioFolder(context, uri) { count ->
                if (count > 0) {
                    Toast.makeText(context, context.getString(R.string.toast_imported_from_folder, count), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_no_new_in_folder), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    timber.log.Timber.tag("MainActivity").w(e, "Failed to take persistable URI permission for %s", uri)
                }
            }
            Toast.makeText(context, context.getString(R.string.toast_importing_files, uris.size), Toast.LENGTH_SHORT).show()
            viewModel.importAudioFiles(context, uris) { count ->
                if (count > 0) {
                    Toast.makeText(context, context.getString(R.string.toast_imported_songs, count), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_no_new_audio), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Only draw bottom Nav/Bars when Now Playing is not covering full overlay
            if (!isExpanded && !isPlayerExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Show progress bar if importing/refreshing
                    if (isImporting) {
                        Surface(
                            color = surfaceColor,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Importing songs...",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "${(importProgress * 100).toInt()}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = accentColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = importProgress,
                                    modifier = Modifier.fillMaxWidth().height(2.dp),
                                    color = accentColor,
                                    trackColor = accentColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    // Slim minimized bottom bar if track is loaded
                    if (currentTrack != null) {
                        val playbackProgress by viewModel.playbackController.playbackProgress.collectAsStateWithLifecycle()
                        val progressVal = if (playbackProgress.duration <= 0 || playbackProgress.isBuffering) {
                            0f
                        } else {
                            (playbackProgress.position.toFloat() / playbackProgress.duration.toFloat()).coerceIn(0f, 1f)
                        }
                        
                        ActiveBottomBar(
                            track = currentTrack!!,
                            isPlaying = isPlaying,
                            progressVal = progressVal,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            onBgColor = onBgColor,
                            onSkipPrevious = { viewModel.playbackController.playPrevious() },
                            onPlayPauseToggle = { viewModel.playbackController.togglePlayPause() },
                            onSkipNext = { viewModel.playbackController.playNext() },
                            onOpenPlayer = { viewModel.setPlayerExpanded(true) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(if (isExpanded) 0.5f else 1f)
                    .fillMaxHeight()
            ) {
                when (currentScreen) {
                Screen.LIBRARY -> LibraryScreen(
                    libraryAlbumArtStyle = libraryAlbumArtStyle,
                    tracks = searchResultTracks,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    playlists = allPlaylists,
                    playlistCrossRefs = allPlaylistTrackCrossRefs,
                    recentlyPlayed = recentlyPlayed,
                    mostPlayed = mostPlayed,
                    recentlyAdded = recentlyAdded,
                    hideSampleMusicButton = hideSampleMusicButton,
                    sortOption = sortOption,
                    onSortOptionChange = { viewModel.updateSortOption(it) },
                    onAddTracksToPlaylist = { playlistId, trackIds ->
                        viewModel.addTracksToPlaylist(playlistId, trackIds)
                    },
                    onTrackOptionClick = { expandedTrackForPlaylistAssociation = it },
                    onTrackPlay = { list, selected ->
                        viewModel.playbackController.setPlaybackList(list, selected)
                    },
                    onPlayNextTracks = { tracks ->
                        viewModel.playbackController.playTracksNext(tracks)
                        Toast.makeText(context, context.getString(R.string.toast_playing_next_multi, tracks.size), Toast.LENGTH_SHORT).show()
                    },
                    onAddToQueueTracks = { tracks ->
                        viewModel.playbackController.addToQueue(tracks)
                        Toast.makeText(context, context.getString(R.string.toast_added_queue_multi, tracks.size), Toast.LENGTH_SHORT).show()
                    },
                    onPlayNext = { track ->
                        viewModel.playbackController.playTrackNext(track)
                        Toast.makeText(context, context.getString(R.string.toast_playing_next, track.title), Toast.LENGTH_SHORT).show()
                    },
                    onAddToQueue = { track ->
                        viewModel.playbackController.addToQueue(track)
                        Toast.makeText(context, context.getString(R.string.toast_added_to_queue, track.title), Toast.LENGTH_SHORT).show()
                    },
                    onImportClick = {
                        filePickerLauncher.launch(arrayOf("audio/*"))
                    },
                    onImportFolderClick = {
                        folderPickerLauncher.launch(null)
                    },
                    onReaddSamplesClick = {
                        viewModel.readdSampleTracks(context) {
                            Toast.makeText(context, context.getString(R.string.toast_sample_tracks_added), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteTrack = { track ->
                        viewModel.deleteTrack(context, track)
                    },
                    onDeleteTracks = { tracks ->
                        viewModel.deleteTracks(context, tracks)
                    },
                    onShowProperties = { track ->
                        trackToShowProperties = track
                    },
                    onSettingsClick = { viewModel.selectScreen(Screen.SETTINGS) },
                    onCreatePlaylistClick = { showPlaylistCreationDialog = true },
                    libraryTabs = libraryTabs,
                    onReorderLibrary = { viewModel.updateLibraryOrder(it) }
                )

                Screen.PLAYLISTS -> {
                    if (activePlaylistForViewer != null) {
                        val currentActivePlaylist = activePlaylistForViewer!!
                        val tracksInPlaylist by viewModel.playlistTracks.collectAsStateWithLifecycle()
                        PlaylistDetailScreen(
                            playlist = currentActivePlaylist,
                            tracksInPlaylist = tracksInPlaylist,
                            allAvailableTracks = allTracks,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            accentColor = accentColor,
                            onBgColor = onBgColor,
                            surfaceColor = surfaceColor,
                            onBack = { viewModel.setActivePlaylistForViewer(null) },
                            onPlayTrack = { song ->
                                viewModel.playbackController.setPlaybackList(tracksInPlaylist, song)
                                viewModel.setPlayerExpanded(true)
                            },
                            onShufflePlay = {
                                viewModel.playbackController.playShuffledList(tracksInPlaylist)
                                viewModel.setPlayerExpanded(true)
                            },
                            onRemoveTrack = { song ->
                                viewModel.removeTrackFromPlaylist(currentActivePlaylist.id, song.id)
                            },
                            onShowProperties = { song ->
                                trackToShowProperties = song
                            },
                            onDeletePlaylist = {
                                viewModel.deletePlaylist(currentActivePlaylist.id)
                                viewModel.setActivePlaylistForViewer(null)
                            },
                            onRenamePlaylist = { newName ->
                                viewModel.renamePlaylist(currentActivePlaylist, newName)
                                viewModel.setActivePlaylistForViewer(currentActivePlaylist.copy(name = newName))
                            },
                            onUpdateCoverArt = { uri ->
                                viewModel.updatePlaylistCoverArt(currentActivePlaylist, uri)
                                viewModel.setActivePlaylistForViewer(currentActivePlaylist.copy(coverArtUri = uri))
                            },
                            onAddTracks = { selectedTracks ->
                                val dupes = viewModel.getDuplicateTrackCount(currentActivePlaylist.id, selectedTracks.map { it.id })
                                selectedTracks.forEach { track ->
                                    viewModel.addTrackToPlaylist(currentActivePlaylist.id, track.id)
                                }
                                if (dupes > 0) {
                                    Toast.makeText(
                                        context,
                                        "Added ${selectedTracks.size} songs ($dupes were already in playlist)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_added_songs_to_playlist, selectedTracks.size, currentActivePlaylist.name),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onExportPlaylist = {
                                val playlistTrackIds = allPlaylistTrackCrossRefs
                                    .filter { it.playlistId == currentActivePlaylist.id }
                                    .map { it.trackId }
                                val pTracks = allTracks.filter { it.id in playlistTrackIds }
                                pendingExportPlaylist = Pair(currentActivePlaylist, pTracks)
                                m3uExportLauncher.launch("${currentActivePlaylist.name}.m3u8")
                            },
                            onPlayNext = {
                                val playlistTrackIds = allPlaylistTrackCrossRefs
                                    .filter { it.playlistId == currentActivePlaylist.id }
                                    .map { it.trackId }
                                val pTracks = allTracks.filter { it.id in playlistTrackIds }
                                if (pTracks.isNotEmpty()) {
                                    pTracks.asReversed().forEach { track ->
                                        viewModel.playbackController.playTrackNext(track)
                                    }
                                    Toast.makeText(context, "Playing next: ${currentActivePlaylist.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onAddToQueue = {
                                val playlistTrackIds = allPlaylistTrackCrossRefs
                                    .filter { it.playlistId == currentActivePlaylist.id }
                                    .map { it.trackId }
                                val pTracks = allTracks.filter { it.id in playlistTrackIds }
                                if (pTracks.isNotEmpty()) {
                                    viewModel.playbackController.addToQueue(pTracks)
                                    Toast.makeText(context, "Added ${pTracks.size} songs to queue", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onPlayTrackNext = { track ->
                                viewModel.playbackController.playTrackNext(track)
                                Toast.makeText(context, context.getString(R.string.toast_playing_next, track.title), Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        PlaylistsScreen(
                            playlists = allPlaylists,
                            crossRefs = allPlaylistTrackCrossRefs,
                            allTracks = allTracks,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            onCreatePlaylistClick = { showPlaylistCreationDialog = true },
                            onImportPlaylistClick = {
                                m3uImportLauncher.launch(arrayOf("*/*"))
                            },
                            onPlaylistSelect = { playlist ->
                                viewModel.setActivePlaylistForViewer(playlist)
                                viewModel.selectPlaylist(playlist.id)
                            },
                            onPlaylistPlay = { playlist ->
                                val playlistTrackIds = allPlaylistTrackCrossRefs
                                    .filter { it.playlistId == playlist.id }
                                    .map { it.trackId }
                                val playlistTracks = allTracks.filter { it.id in playlistTrackIds }
                                if (playlistTracks.isNotEmpty()) {
                                    viewModel.playbackController.setPlaybackList(playlistTracks, playlistTracks.first())
                                    viewModel.setPlayerExpanded(true)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.toast_playlist_empty), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onPlaylistDelete = { playlist ->
                                viewModel.deletePlaylist(playlist.id)
                                Toast.makeText(context, context.getString(R.string.toast_playlist_deleted, playlist.name), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Screen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    accentColor = accentColor,
                    surfaceColor = surfaceColor,
                    onBgColor = onBgColor
                )
            }

            // Floating Shuffle Button above minimized player in bottom right
            if (!isExpanded && !isPlayerExpanded && allTracks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
                        .clickable {
                            if (allTracks.isNotEmpty()) {
                                viewModel.playbackController.playShuffledList(allTracks)
                                Toast.makeText(context, context.getString(R.string.toast_queue_shuffled_played), Toast.LENGTH_SHORT).show()
                                viewModel.setPlayerExpanded(true)
                            }
                        }
                        .testTag("floating_shuffle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    ShuffleIcon(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } // ends Box(weight(1f))

        if (isExpanded) {
            // Secondary detail pane
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .background(surfaceColor)
            ) {
                if (currentTrack != null) {
                    FocusedNowPlayingScreen(
                        viewModel = viewModel,
                        track = currentTrack!!,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        onBgColor = onBgColor,
                        onShowTrackProperties = { trackToShowProperties = it }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No song playing",
                            color = onBgColor.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } // ends secondary pane Box
    } // ends Row
} // ends Scaffold content lambda

// Modal Sheet: Create Playlist modal
    if (showPlaylistCreationDialog) {
        com.example.ui.components.NewPlaylistDialog(
            initialName = "",
            accentColor = accentColor,
            onBgColor = onBgColor,
            onDismiss = { showPlaylistCreationDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showPlaylistCreationDialog = false
            }
        )
    }

    // Modal Sheet: Add song to playlist
    if (expandedTrackForPlaylistAssociation != null) {
        val selectedTrack = expandedTrackForPlaylistAssociation!!
        Dialog(onDismissRequest = { expandedTrackForPlaylistAssociation = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Add to Playlist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        "Select target playlist for '${selectedTrack.title}'",
                        fontSize = 12.sp,
                        color = onBgColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (allPlaylists.isEmpty()) {
                        Text(
                            "No playlists created yet.",
                            color = onBgColor.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(allPlaylists) { plist ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addTrackToPlaylist(plist.id, selectedTrack.id)
                                            Toast.makeText(context, context.getString(R.string.toast_added_to_playlist, plist.name), Toast.LENGTH_SHORT).show()
                                            expandedTrackForPlaylistAssociation = null
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    Text(
                                        plist.name,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(onBgColor.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { expandedTrackForPlaylistAssociation = null },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Close",
                            color = MaterialTheme.colorScheme.background,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (trackToShowProperties != null) {
        TrackPropertiesDialog(
            track = trackToShowProperties!!,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onDismiss = { trackToShowProperties = null }
        )
    }

    // Now Playing cover full overlay view
    if (!isExpanded) {
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f)),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (currentTrack != null) {
                FocusedNowPlayingScreen(
                    viewModel = viewModel,
                    track = currentTrack!!,
                    accentColor = accentColor,
                    surfaceColor = surfaceColor,
                    onBgColor = onBgColor,
                    onShowTrackProperties = { trackToShowProperties = it }
                )
            }
        }
    }
}
