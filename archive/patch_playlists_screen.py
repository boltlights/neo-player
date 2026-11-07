import re
with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'r') as f:
    content = f.read()

target = r'''@Composable
fun PlaylistDetailScreen\(.*?\}\) \{.*?(?=@Composable\nfun PlaylistTrackItem)'''
repl = r'''@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    tracksInPlaylist: List<TrackEntity>,
    accentColor: Color,
    onBgColor: Color,
    surfaceColor: Color,
    onBack: () -> Unit,
    onPlayTrack: (TrackEntity) -> Unit,
    onShufflePlay: () -> Unit,
    onRemoveTrack: (TrackEntity) -> Unit,
    onShowProperties: (TrackEntity) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onUpdateCoverArt: (String?) -> Unit = {}
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200 }
    }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(playlist.name) }

    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { onUpdateCoverArt(it.toString()) }
        }
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val totalDurationMs = tracksInPlaylist.sumOf { it.durationMs }
    val durationHours = totalDurationMs / (1000 * 60 * 60)
    val durationMinutes = (totalDurationMs / (1000 * 60)) % 60
    val durationStr = if (durationHours > 0) "${durationHours}h ${durationMinutes}m" else "${durationMinutes}m"

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenamePlaylist(renameInput)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("OK", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = onBgColor.copy(alpha = 0.7f))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Top)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = onBgColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        if (playlist.id > 0) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            photoPicker.launch(
                                                androidx.activity.result.PickVisualMediaRequest(
                                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }
                                    }
                                )
                        ) {
                            if (playlist.coverArtUri != null) {
                                coil.compose.AsyncImage(
                                    model = playlist.coverArtUri,
                                    contentDescription = "Playlist Cover",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = coil.size.Scale.FILL.let { androidx.compose.ui.layout.ContentScale.Crop }
                                )
                            } else {
                                com.example.ui.components.MosaicCoverArt(
                                    imageUris = tracksInPlaylist.take(4).map { it.artPath },
                                    modifier = Modifier.fillMaxSize(),
                                    accentColor = accentColor,
                                    iconIfEmpty = Icons.Default.MusicNote
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = playlist.name,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = onBgColor,
                            lineHeight = 36.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language, // Globe icon approx
                            contentDescription = null,
                            tint = onBgColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = durationStr,
                            color = onBgColor.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onShufflePlay) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = onBgColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .clickable {
                                    if (tracksInPlaylist.isNotEmpty()) onPlayTrack(tracksInPlaylist.first())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (playlist.id > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Add functionality */ }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = onBgColor.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Add to this playlist",
                            fontSize = 16.sp,
                            color = onBgColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            items(tracksInPlaylist) { song ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaylistTrackItem(
                        song = song,
                        accentColor = accentColor,
                        onPlay = { onPlayTrack(song) },
                        onShowProperties = { onShowProperties(song) }
                    )
                }
            }
        }

        // Custom TopAppBar Overlay
        val appBarColor by animateColorAsState(if (isScrolled) MaterialTheme.colorScheme.surface else Color.Transparent)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(appBarColor)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = onBgColor)
                    }
                    AnimatedVisibility(visible = isScrolled, enter = fadeIn(), exit = fadeOut()) {
                        Text(playlist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onBgColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = isScrolled && tracksInPlaylist.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onPlayTrack(tracksInPlaylist.first()) }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = onBgColor)
                            }
                        }
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = onBgColor)
                    }
                }
            }
        }

        if (showOptionsMenu) {
            val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = { showOptionsMenu = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playlist.coverArtUri != null) {
                            coil.compose.AsyncImage(
                                model = playlist.coverArtUri,
                                contentDescription = "Playlist Cover",
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            com.example.ui.components.MosaicCoverArt(
                                imageUris = tracksInPlaylist.take(4).map { it.artPath },
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                accentColor = accentColor,
                                iconIfEmpty = Icons.Default.MusicNote
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Playlist", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                            Text(playlist.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("${tracksInPlaylist.size} songs", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val items = listOf(
                        "Play" to Icons.Default.PlayArrow,
                        "Shuffle" to Icons.Default.Shuffle,
                        "Play next" to Icons.AutoMirrored.Filled.PlaylistPlay,
                        "Add to queue" to Icons.AutoMirrored.Filled.QueueMusic,
                        "Rename" to Icons.Default.Edit,
                        "Delete" to Icons.Default.Delete,
                        "Share" to Icons.Default.Share
                    ).filter { (label, _) ->
                        if (playlist.id < 0) {
                            label != "Rename" && label != "Delete"
                        } else {
                            true
                        }
                    }
                    
                    items.forEach { (label, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (label == "Delete") {
                                        onDeletePlaylist()
                                    } else if (label == "Play" && tracksInPlaylist.isNotEmpty()) {
                                        onPlayTrack(tracksInPlaylist.first())
                                    } else if (label == "Shuffle" && tracksInPlaylist.isNotEmpty()) {
                                        onShufflePlay()
                                    } else if (label == "Rename") {
                                        renameInput = playlist.name
                                        showRenameDialog = true
                                    }
                                    showOptionsMenu = false
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
'''
content = re.sub(target, repl, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt', 'w') as f:
    f.write(content)
