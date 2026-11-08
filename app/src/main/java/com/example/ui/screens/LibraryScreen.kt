package com.example.ui.screens

import com.example.ui.components.TrackRowItem
import com.example.ui.components.FastScrollbar

import java.io.File
import com.example.util.NaturalOrderComparator
import android.widget.Toast


import com.example.ui.components.rememberReorderableLazyListState
import com.example.ui.components.reorderableItem
import com.example.ui.components.reorderDragHandle
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.itemsIndexed

import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import com.example.LibraryAlbumArtStyle
import com.example.data.PlaylistEntity
import com.example.data.PlaylistTrackCrossRef
import com.example.data.TrackEntity
import com.example.ui.components.TrackArtworkWidget
import com.example.util.formatDuration

// Manrope is bundled (res/font, SIL OFL 1.1 — see docs/licenses/Manrope-OFL.txt). It used to be
// downloaded at runtime from Google Play services, which fails on de-Googled phones and is not
// allowed on F-Droid. One variable-weight file serves both weights (variations need API 26+).
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val ManropeFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(
        R.font.manrope_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(800))
    ),
    androidx.compose.ui.text.font.Font(
        R.font.manrope_variable,
        weight = FontWeight.Bold,
        variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(700))
    )
)

fun getGenreForTrack(track: TrackEntity): String {
    val metaGenre = track.genre?.trim()
    if (!metaGenre.isNullOrBlank() && !metaGenre.equals("Unknown", ignoreCase = true)) {
        return metaGenre.split("/").firstOrNull()?.trim()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: metaGenre
    }
    // Untagged songs are grouped honestly. This used to invent genres from words in the title/artist
    // (e.g. "Don't Dream It's Over" -> "Ambient & Lofi", everything else -> "Acoustic & Others"),
    // mixing made-up categories in with real ones.
    return UNKNOWN_GENRE
}

const val UNKNOWN_GENRE = "Unknown Genre"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<TrackEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    accentColor: Color,
    playlists: List<PlaylistEntity>,
    playlistCrossRefs: List<PlaylistTrackCrossRef>,
    recentlyPlayed: List<TrackEntity> = emptyList(),
    mostPlayed: List<TrackEntity> = emptyList(),
    recentlyAdded: List<TrackEntity> = emptyList(),
    hideSampleMusicButton: Boolean,
    libraryAlbumArtStyle: LibraryAlbumArtStyle,
    sortOption: String,
    onSortOptionChange: (String) -> Unit,
    onAddTracksToPlaylist: (Int, List<Int>) -> Unit,
    onTrackPlay: (List<TrackEntity>, TrackEntity) -> Unit,
    onTrackOptionClick: (TrackEntity) -> Unit,
    onPlayNextTracks: (List<TrackEntity>) -> Unit = {},
    onAddToQueueTracks: (List<TrackEntity>) -> Unit = {},
    onPlayNext: (TrackEntity) -> Unit,
    onAddToQueue: (TrackEntity) -> Unit,
    onImportClick: () -> Unit,
    onImportFolderClick: () -> Unit,
    onReaddSamplesClick: () -> Unit,
    onDeleteTrack: (TrackEntity) -> Unit,
    onDeleteTracks: (List<TrackEntity>) -> Unit,
    onShowProperties: (TrackEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    libraryTabs: List<String>,
    indexedAlbums: List<com.example.data.IndexedAlbum> = emptyList(),
    indexedArtists: List<com.example.data.IndexedArtist> = emptyList(),
    indexedGenres: List<com.example.data.IndexedGenre> = emptyList(),
    onReorderLibrary: (List<TrackEntity>) -> Unit = {},
    /** Opens the full playlist manager (rename/delete/remove songs...) for a user playlist. */
    onOpenPlaylistManager: (PlaylistEntity) -> Unit = {},
    onImportPlaylistClick: () -> Unit = {}
) {
    var isMultiSelectActive by remember { mutableStateOf(false) }
    val selectedTracks = remember { mutableStateListOf<TrackEntity>() }
    var isSearchActive by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    var showSortMenu by remember { mutableStateOf(false) }

    if (isMultiSelectActive) {
        BackHandler {
            isMultiSelectActive = false
            selectedTracks.clear()
        }
    }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var currentSelectedTab by remember { mutableStateOf("Songs") }
    var selectedTab by remember(libraryTabs) { 
        mutableStateOf(if (libraryTabs.contains(currentSelectedTab)) currentSelectedTab else libraryTabs.firstOrNull() ?: "Songs") 
    }
    // keep currentSelectedTab synced
    LaunchedEffect(selectedTab) { currentSelectedTab = selectedTab }

    var activeAlbumDetails by remember { mutableStateOf<String?>(null) }
    var activeArtistDetails by remember { mutableStateOf<String?>(null) }
    var activeGenreDetails by remember { mutableStateOf<String?>(null) }
    var activePlaylistDetails by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showPlaylistSelectDialogForGroupAdd by remember { mutableStateOf(false) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var isReordering by remember { mutableStateOf(false) }
    
    // Sorting logic (searching is now handled externally)
    val filteredTracks = tracks

    if (activeAlbumDetails != null) {
        val albumTracks = remember(activeAlbumDetails, tracks) {
            tracks.filter { it.album == activeAlbumDetails }.sortedWith(NaturalOrderComparator.ALBUM_TRACKS_COMPARATOR)
        }
        LibraryCategoryDetailView(
            title = activeAlbumDetails!!,
            subtitle = com.example.util.songCountCaps(albumTracks.size, "IN ALBUM"),
            tracksList = albumTracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            accentColor = accentColor,
            libraryAlbumArtStyle = libraryAlbumArtStyle,
            onBack = { activeAlbumDetails = null },
            onTrackPlay = { track -> onTrackPlay(albumTracks, track) },
            onTrackOptionClick = onTrackOptionClick,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onDeleteTrack = onDeleteTrack,
            onShowProperties = onShowProperties
        )
    } else if (activeArtistDetails != null) {
        val artistTracks = remember(activeArtistDetails, tracks) {
            tracks.filter { 
                val effectiveArtist = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                effectiveArtist == activeArtistDetails 
            }.sortedWith(NaturalOrderComparator.ARTIST_TRACKS_COMPARATOR)
        }
        LibraryCategoryDetailView(
            title = activeArtistDetails!!,
            subtitle = com.example.util.songCountCaps(artistTracks.size, "BY ARTIST"),
            tracksList = artistTracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            accentColor = accentColor,
            libraryAlbumArtStyle = libraryAlbumArtStyle,
            onBack = { activeArtistDetails = null },
            onTrackPlay = { track -> onTrackPlay(artistTracks, track) },
            onTrackOptionClick = onTrackOptionClick,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onDeleteTrack = onDeleteTrack,
            onShowProperties = onShowProperties
        )
    } else if (activeGenreDetails != null) {
        val genreTracks = remember(activeGenreDetails, tracks) {
            tracks.filter { getGenreForTrack(it) == activeGenreDetails }.sortedWith(NaturalOrderComparator.GENRE_TRACKS_COMPARATOR)
        }
        LibraryCategoryDetailView(
            title = activeGenreDetails!!,
            subtitle = com.example.util.songCountCaps(genreTracks.size, "IN GENRE"),
            tracksList = genreTracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            accentColor = accentColor,
            libraryAlbumArtStyle = libraryAlbumArtStyle,
            onBack = { activeGenreDetails = null },
            onTrackPlay = { track -> onTrackPlay(genreTracks, track) },
            onTrackOptionClick = onTrackOptionClick,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onDeleteTrack = onDeleteTrack,
            onShowProperties = onShowProperties
        )
    } else if (activePlaylistDetails != null) {
        val pl = activePlaylistDetails!!
        val playlistTracks = remember(pl.id, playlistCrossRefs, tracks, recentlyPlayed, mostPlayed, recentlyAdded) {
            when (pl.id) {
                -1 -> recentlyPlayed
                -2 -> mostPlayed
                -3 -> recentlyAdded
                else -> {
                    val trackIdsInPl = playlistCrossRefs.filter { it.playlistId == pl.id }.map { it.trackId }
                    trackIdsInPl.mapNotNull { id -> tracks.find { it.id == id } }
                }
            }
        }
        LibraryCategoryDetailView(
            title = pl.name,
            subtitle = com.example.util.songCountCaps(playlistTracks.size, "IN PLAYLIST"),
            tracksList = playlistTracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            accentColor = accentColor,
            libraryAlbumArtStyle = libraryAlbumArtStyle,
            onBack = { activePlaylistDetails = null },
            onTrackPlay = { track -> onTrackPlay(playlistTracks, track) },
            onTrackOptionClick = onTrackOptionClick,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onDeleteTrack = onDeleteTrack,
            onShowProperties = onShowProperties
        )
    } else {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
            snapAnimationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
        )

        val titleHeight = 64.dp
        val tabsHeight = 48.dp
        val density = LocalDensity.current
        val titleHeightPx = remember(density) { with(density) { titleHeight.toPx() } }

        LaunchedEffect(titleHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -titleHeightPx
        }

        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                scrollBehavior.state.heightOffset = 0f
            }
        }

        val heightOffset = if (isSearchActive) 0f else scrollBehavior.state.heightOffset
        val clampedHeightOffset = if (isSearchActive) 0f else heightOffset.coerceAtLeast(-titleHeightPx)
        val currentCollapsedFraction = if (isSearchActive) 0f else (-clampedHeightOffset / titleHeightPx).coerceIn(0f, 1f)

        // dynamicListBottomPadding removed to avoid jitter

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight + titleHeight + tabsHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationY = clampedHeightOffset }
                            .background(MaterialTheme.colorScheme.background)
                            .statusBarsPadding()
                    ) {
                        // Title / Search Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(titleHeight)
                                .graphicsLayer {
                                    alpha = (1f - currentCollapsedFraction).coerceIn(0f, 1f)
                                    clip = true
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (isSearchActive) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        placeholder = { Text("Search matches...", fontSize = 14.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = accentColor) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                onSearchQueryChange("")
                                                isSearchActive = false
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Close search", tint = Color.White)
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Neo",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight(800),
                                        fontFamily = ManropeFontFamily,
                                        color = Color.White,
                                        modifier = Modifier.testTag("app_title")
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(onClick = { isSearchActive = true }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                                        }

                                        Box {
                                            IconButton(onClick = { showSortMenu = true }) {
                                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Options", tint = Color.White)
                                            }
                                            DropdownMenu(
                                                expanded = showSortMenu,
                                                onDismissRequest = { showSortMenu = false }
                                            ) {
                                                listOf("Track Number", "Filename", "Title A-Z", "Title Z-A", "Artist", "Album", "Oldest", "Newest", "Custom").forEach { option ->
                                                    val displayName = if (option == "Custom") "Custom (Drag to Reorder)" else option
                                                    DropdownMenuItem(
                                                        text = { Text(displayName) },
                                                        onClick = {
                                                            onSortOptionChange(option)
                                                            showSortMenu = false
                                                        },
                                                        trailingIcon = if (option == sortOption) {
                                                            { Icon(Icons.Default.Check, contentDescription = "Active Sort") }
                                                        } else null
                                                    )
                                                }
                                            }
                                        }

                                        Box {
                                            IconButton(onClick = { showOverflowMenu = true }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
                                            }
                                            DropdownMenu(
                                                expanded = showOverflowMenu,
                                                onDismissRequest = { showOverflowMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Import Music Files") },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        onImportClick()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Import Folder") },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        onImportFolderClick()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Import Playlist (M3U)") },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        onImportPlaylistClick()
                                                    }
                                                )
                                                if (!hideSampleMusicButton) {
                                                    DropdownMenuItem(
                                                        text = { Text("Generate 5 Samples") },
                                                        onClick = {
                                                            showOverflowMenu = false
                                                            onReaddSamplesClick()
                                                        }
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text("Settings") },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        onSettingsClick()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Tabs Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(tabsHeight),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 20.dp, end = 20.dp)
                            ) {
                                libraryTabs.forEach { tab ->
                                    val isSelected = selectedTab == tab
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            selectedTab = tab
                                        }
                                    ) {
                                        if (isSelected) {
                                            Text(
                                                text = tab,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                        colors = listOf(accentColor, Color.White)
                                                    ),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.SansSerif
                                                )
                                            )
                                        } else {
                                            Text(
                                                text = tab,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(if (isSelected) 36.dp else 4.dp)
                                                .height(4.dp)
                                                .background(
                                                    brush = if (isSelected) {
                                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                            colors = listOf(accentColor, Color.White.copy(alpha = 0.8f))
                                                        )
                                                    } else {
                                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                            colors = listOf(Color.Transparent, Color.Transparent)
                                                        )
                                                    },
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                                
                                // Add extra space at the end to ensure the last tab is not hidden behind the search icon when collapsed
                                Spacer(modifier = Modifier.width(48.dp))
                            }

                            // Search icon on the right side of tabs in collapsed state
                            if (!isSearchActive) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 8.dp, bottom = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = {
                                            isSearchActive = true
                                            // Reset scroll behavior to fully expanded state when search is triggered
                                            scrollBehavior.state.heightOffset = 0f
                                        },
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = if (currentCollapsedFraction > 0.5f) {
                                                    ((currentCollapsedFraction - 0.5f) * 2f).coerceIn(0f, 1f)
                                                } else {
                                                    0f
                                                }
                                            }
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = if (currentCollapsedFraction > 0.5f) 0.9f else 0f),
                                                shape = CircleShape
                                            ),
                                        enabled = currentCollapsedFraction > 0.5f
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            val totalTopPadding = paddingValues.calculateTopPadding()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // To keep the layout nesting identical and avoid changing any braces at the end
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layout { measurable, constraints ->
                            val extraHeight = titleHeightPx.toInt()
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minHeight = constraints.maxHeight + extraHeight,
                                    maxHeight = constraints.maxHeight + extraHeight
                                )
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        .graphicsLayer { translationY = clampedHeightOffset }
                ) {
                    when (selectedTab) {
                        "Songs" -> {
                            SongsTabView(
                                tracks = tracks,
                                filteredTracks = filteredTracks,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                libraryAlbumArtStyle = libraryAlbumArtStyle,
                                sortOption = sortOption,
                                onSortOptionChange = onSortOptionChange,
                                onReorderLibrary = onReorderLibrary,
                                totalTopPadding = totalTopPadding,
                                searchQuery = searchQuery,
                                isMultiSelectActive = isMultiSelectActive,
                                selectedTracks = selectedTracks,
                                onToggleSelection = { track ->
                                    if (selectedTracks.contains(track)) selectedTracks.remove(track) else selectedTracks.add(track)
                                },
                                onStartMultiSelect = { track ->
                                    isMultiSelectActive = true
                                    selectedTracks.add(track)
                                },
                                onCancelMultiSelect = {
                                    isMultiSelectActive = false
                                    selectedTracks.clear()
                                },
                                onTrackPlay = onTrackPlay,
                                onTrackOptionClick = onTrackOptionClick,
                                onPlayNext = onPlayNext,
                                onAddToQueue = onAddToQueue,
                                onDeleteTrack = onDeleteTrack,
                                onShowProperties = onShowProperties,
                                onPlayArtist = { activeArtistDetails = it },
                                onPlayGenre = { activeGenreDetails = it },
                                onPlayNextTracks = onPlayNextTracks,
                                onAddToQueueTracks = onAddToQueueTracks,
                                onOpenPlaylistSelectForGroupAdd = { showPlaylistSelectDialogForGroupAdd = true },
                                onOpenBatchDeleteConfirm = { showBatchDeleteConfirmDialog = true }
                            )
                        }
                        "Albums" -> {
                            AlbumsTabView(
                                filteredTracks = filteredTracks,
                                indexedAlbums = indexedAlbums,
                                searchQuery = searchQuery,
                                totalTopPadding = totalTopPadding,
                                accentColor = accentColor,
                                libraryAlbumArtStyle = libraryAlbumArtStyle,
                                onAlbumClick = { activeAlbumDetails = it }
                            )
                        }
                        "Artists" -> {
                            ArtistsTabView(
                                filteredTracks = filteredTracks,
                                indexedArtists = indexedArtists,
                                searchQuery = searchQuery,
                                totalTopPadding = totalTopPadding,
                                accentColor = accentColor,
                                onArtistClick = { activeArtistDetails = it }
                            )
                        }
                        "Genre", "Genres" -> {
                            GenresTabView(
                                filteredTracks = filteredTracks,
                                totalTopPadding = totalTopPadding,
                                accentColor = accentColor,
                                libraryAlbumArtStyle = libraryAlbumArtStyle,
                                onGenreClick = { activeGenreDetails = it }
                            )
                        }
                        "Playlists" -> {
                            PlaylistsTabView(
                                playlists = playlists,
                                filteredTracks = filteredTracks,
                                playlistCrossRefs = playlistCrossRefs,
                                recentlyPlayed = recentlyPlayed,
                                mostPlayed = mostPlayed,
                                recentlyAdded = recentlyAdded,
                                totalTopPadding = totalTopPadding,
                                accentColor = accentColor,
                                libraryAlbumArtStyle = libraryAlbumArtStyle,
                                onCreatePlaylistClick = onCreatePlaylistClick,
                                // User playlists open the full manager; the automatic ones (Recently
                                // Played / Most Played / Recently Added, negative ids) keep the simple view.
                                onPlaylistClick = { pl -> if (pl.id > 0) onOpenPlaylistManager(pl) else activePlaylistDetails = pl }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPlaylistSelectDialogForGroupAdd) {
        Dialog(onDismissRequest = { showPlaylistSelectDialogForGroupAdd = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
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
                        "Select target playlist for ${com.example.util.songCount(selectedTracks.size)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (playlists.isEmpty()) {
                        Text(
                            "No playlists created yet.\nCreate one in the PLAYLISTS tab first.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 13.sp
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(playlists) { plist ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddTracksToPlaylist(plist.id, selectedTracks.map { it.id })
                                            Toast.makeText(context, context.getString(R.string.toast_added_songs_to_playlist, selectedTracks.size, plist.name), Toast.LENGTH_SHORT).show()
                                            showPlaylistSelectDialogForGroupAdd = false
                                            isMultiSelectActive = false
                                            selectedTracks.clear()
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
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showPlaylistSelectDialogForGroupAdd = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Close", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showBatchDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            title = {
                Text(
                    "Delete Tracks",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Red
                )
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete ${selectedTracks.size} selected track(s) from your device?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTracks(selectedTracks.toList())
                        Toast.makeText(context, context.getString(R.string.toast_deleted_tracks, selectedTracks.size), Toast.LENGTH_SHORT).show()
                        showBatchDeleteConfirmDialog = false
                        isMultiSelectActive = false
                        selectedTracks.clear()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
