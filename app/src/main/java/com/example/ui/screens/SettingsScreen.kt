package com.example.ui.screens

import com.example.R
import android.net.Uri
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AlbumArtStyle
import com.example.AppThemeMode
import com.example.LibraryAlbumArtStyle
import com.example.MusicThemeStyle
import com.example.MusicViewModel

@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color = MaterialTheme.colorScheme.onBackground,
    onBack: () -> Unit = { viewModel.selectScreen(com.example.Screen.LIBRARY) }
) {
    var currentSubScreen by remember { mutableStateOf("Main") }

    BackHandler(enabled = currentSubScreen != "Main") {
        if (currentSubScreen in listOf("Themes", "Album Art", "Library Tabs")) {
            currentSubScreen = "Appearance"
        } else {
            currentSubScreen = "Main"
        }
    }

    if (currentSubScreen == "Appearance") {
        AppearanceSettingsScreen(
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBack = { currentSubScreen = "Main" },
            onNavigateToThemes = { currentSubScreen = "Themes" },
            onNavigateToAlbumArt = { currentSubScreen = "Album Art" },
            onNavigateToLibraryTabs = { currentSubScreen = "Library Tabs" }
        )
    } else if (currentSubScreen == "Themes") {
        ThemesScreen(
            viewModel = viewModel,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onBack = { currentSubScreen = "Appearance" }
        )
    } else if (currentSubScreen == "Album Art") {
        AlbumArtSettingsScreen(
            viewModel = viewModel,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onBack = { currentSubScreen = "Appearance" }
        )
    } else if (currentSubScreen == "Library Tabs") {
        LibraryTabsSettingsScreen(
            viewModel = viewModel,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onBack = { currentSubScreen = "Appearance" }
        )
    } else if (currentSubScreen == "Playback & Audio") {
        PlaybackAudioSettingsScreen(
            viewModel = viewModel,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onBack = { currentSubScreen = "Main" }
        )
    } else if (currentSubScreen == "Others") {
        OthersSettingsScreen(
            viewModel = viewModel,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onBack = { currentSubScreen = "Main" }
        )
    } else if (currentSubScreen == "Music Sources") {
        MusicSourcesScreen(
            viewModel = viewModel,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onBack = { currentSubScreen = "Main" }
        )
    } else if (currentSubScreen == "About") {
        AboutScreen(
            viewModel = viewModel,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onBack = { currentSubScreen = "Main" }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .offset(x = (-12).dp)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = accentColor
                    )
                }
                Text(
                    "SETTINGS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    modifier = Modifier.offset(x = (-4).dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentSubScreen = "Appearance" }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ColorLens,
                        contentDescription = "Appearance",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Appearance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "Themes, album art & library tabs",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentSubScreen = "Playback & Audio" }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Playback & Audio",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Playback & Audio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "Volume normalization, audio focus & effects",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentSubScreen = "Music Sources" }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = "Music Sources",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Music Sources",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "Select and manage library folder sources",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentSubScreen = "Others" }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Others",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Others",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "Miscellaneous settings",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentSubScreen = "About" }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "About",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "About",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "Library statistics and app info",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun OthersSettingsScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = accentColor)
                }
                Text(
                    "OTHERS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Debugging",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        var showErrorLogs by remember { mutableStateOf(false) }
        var logCount by remember { mutableStateOf(com.example.util.CrashReporter.getLogCount(context)) }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showErrorLogs = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = "Error Logs",
                    tint = onBgColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Crash Logs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        if (logCount == 0) "0 events logged (monitoring active)" else "$logCount event(s) recorded (tap to view)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor.copy(alpha = 0.6f)
                    )
                }
                if (logCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$logCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Red
                        )
                    }
                }
            }
        }
        
        if (showErrorLogs) {
            ErrorLogsDialog(
                onDismiss = { 
                    showErrorLogs = false 
                    logCount = com.example.util.CrashReporter.getLogCount(context)
                },
                accentColor = accentColor,
                surfaceColor = surfaceColor,
                onBgColor = onBgColor
            )
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}


fun formatStatsDuration(totalMs: Long): String {
    val totalSeconds = (totalMs / 1000).coerceAtLeast(0L)
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

@Composable
fun AboutScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onBack: () -> Unit
) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    // Accurately calculate statistics matching Library tab categorizations
    val totalSongs = allTracks.size
    val totalArtists = remember(allTracks) {
        if (allTracks.isEmpty()) 0
        else {
            allTracks.groupBy {
                val effective = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                effective.trim().ifBlank { "Unknown Artist" }
            }.size
        }
    }
    val totalAlbums = remember(allTracks) {
        if (allTracks.isEmpty()) 0
        else {
            allTracks.groupBy {
                it.album.trim().ifBlank { "Unknown Album" }
            }.size
        }
    }
    val totalPlaylists = allPlaylists.size
    val totalGenres = remember(allTracks) {
        if (allTracks.isEmpty()) 0
        else {
            allTracks.map { getGenreForTrack(it) }.distinct().size
        }
    }
    val totalPlaytimeMs = remember(allTracks) {
        allTracks.sumOf { it.durationMs.coerceAtLeast(0L) }
    }

    val playTimeStr = remember(totalPlaytimeMs) {
        formatStatsDuration(totalPlaytimeMs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = accentColor)
                }
                Text(
                    "ABOUT",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Text(
                "LIBRARY SPECS & APPLICATION INFO",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    "Library Statistics",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatRow("Total Songs", totalSongs.toString(), Icons.Default.MusicNote, accentColor, onBgColor)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(onBgColor.copy(alpha = 0.08f)))
                        StatRow("Artists", totalArtists.toString(), Icons.Default.Person, accentColor, onBgColor)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(onBgColor.copy(alpha = 0.08f)))
                        StatRow("Albums", totalAlbums.toString(), Icons.Default.Album, accentColor, onBgColor)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(onBgColor.copy(alpha = 0.08f)))
                        StatRow("Playlists", totalPlaylists.toString(), Icons.Default.PlaylistPlay, accentColor, onBgColor)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(onBgColor.copy(alpha = 0.08f)))
                        StatRow("Genres", totalGenres.toString(), Icons.Default.Category, accentColor, onBgColor)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(onBgColor.copy(alpha = 0.08f)))
                        StatRow("Total Playtime", playTimeStr, Icons.Default.AccessTime, accentColor, onBgColor)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "App Information",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoRow("App Name", "Neo", onBgColor)
                        InfoRow("Version", "1.3.0", onBgColor)
                        InfoRow("Framework", "Android (Jetpack Compose)", onBgColor)
                        InfoRow("Audio SDK", "Jetpack Media3 (ExoPlayer)", onBgColor)
                        InfoRow("Local Storage", "Room SQLite Database", onBgColor)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Crafted with monospace discipline & clean code.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onBgColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor
            )
        }
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = accentColor
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    onBgColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = onBgColor.copy(alpha = 0.6f)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = onBgColor
        )
    }
}


@Composable
fun ErrorLogsDialog(
    onDismiss: () -> Unit,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var logs by remember { mutableStateOf(com.example.util.CrashReporter.getLogs(context)) }
    var entries by remember { mutableStateOf(com.example.util.CrashReporter.getStructuredEntries(context)) }
    var healthInfo by remember { mutableStateOf(com.example.util.CrashReporter.getSystemHealthSummary(context)) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Events, 1: Raw Markdown, 2: System Health

    fun refreshAll() {
        logs = com.example.util.CrashReporter.getLogs(context)
        entries = com.example.util.CrashReporter.getStructuredEntries(context)
        healthInfo = com.example.util.CrashReporter.getSystemHealthSummary(context)
    }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(logs.toByteArray())
                }
                android.widget.Toast.makeText(context, context.getString(R.string.toast_exported_success), android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.toast_export_failed), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Crash & Diagnostics",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = onBgColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Automatic JVM & OS Process Exit monitoring",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = onBgColor.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = onBgColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Test error & Scan OS exits
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            com.example.util.CrashReporter.logTestCrash(context)
                            refreshAll()
                            android.widget.Toast.makeText(context, "Diagnostic test error logged", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = accentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Error", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = accentColor)
                    }

                    OutlinedButton(
                        onClick = {
                            val newFound = com.example.util.CrashReporter.scanAndSyncSystemExits(context)
                            refreshAll()
                            val msg = if (newFound > 0) "Found $newFound OS exit events" else "No new OS exits detected"
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = onBgColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan OS Exits", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = onBgColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Events | Raw Markdown | System Health
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = accentColor,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Events (${entries.size})",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedTab == 0) accentColor else onBgColor.copy(alpha = 0.6f)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Markdown",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedTab == 1) accentColor else onBgColor.copy(alpha = 0.6f)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Health",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedTab == 2) accentColor else onBgColor.copy(alpha = 0.6f)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Events tab
                            if (entries.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Safe",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No Crashes Recorded",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = onBgColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "All systems running smoothly.\nNeo Player actively monitors:\nâ€¢ Uncaught fatal JVM exceptions\nâ€¢ Android OS system exits (ANRs, native crashes, LMK)\nâ€¢ Unhandled coroutines and player errors",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = onBgColor.copy(alpha = 0.7f),
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            com.example.util.CrashReporter.logTestCrash(context)
                                            refreshAll()
                                            android.widget.Toast.makeText(context, "Test crash entry generated", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) {
                                        Text("Simulate Test Error", color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(entries) { entry ->
                                        CrashEntryCard(
                                            entry = entry,
                                            accentColor = accentColor,
                                            onBgColor = onBgColor,
                                            onCopyStack = { stack ->
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(stack))
                                                android.widget.Toast.makeText(context, context.getString(R.string.toast_copied_clipboard), android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Raw Markdown tab
                            val scrollState = rememberScrollState()
                            if (logs.isBlank()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "No logs available. Tap 'Test Error' to generate a sample log.",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = onBgColor.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                Text(
                                    text = logs,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = onBgColor.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                )
                            }
                        }
                        2 -> {
                            // System Health tab
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HealthRow("App Version", healthInfo.appVersion, onBgColor)
                                HealthRow("Android OS", healthInfo.osVersion, onBgColor)
                                HealthRow("Device Model", healthInfo.deviceModel, onBgColor)
                                HealthRow("RAM Usage", "${healthInfo.usedMemoryMb} MB / ${healthInfo.maxMemoryMb} MB", onBgColor)
                                HealthRow("Process Uptime", "${healthInfo.uptimeMinutes} minutes", onBgColor)
                                HealthRow("Total Logged Events", "${healthInfo.totalRecordedCrashes}", onBgColor)
                                HealthRow("Crash Monitoring", if (healthInfo.isMonitoringActive) "Active (UncaughtHandler + OS ExitSync)" else "Inactive", Color(0xFF4CAF50))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Strip: Clear, Copy, Export
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { 
                            com.example.util.CrashReporter.clearLogs(context)
                            refreshAll()
                            android.widget.Toast.makeText(context, "Crash logs cleared", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Clear", color = Color.Red.copy(alpha = 0.85f), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }

                    Row {
                        TextButton(
                            onClick = { 
                                val exportText = if (logs.isNotBlank()) logs else "No logs recorded."
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exportText))
                                android.widget.Toast.makeText(context, context.getString(R.string.toast_copied_clipboard), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy", color = accentColor, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { 
                                val date = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault()).format(java.util.Date())
                                exportLauncher.launch("neo_crash_logs_$date.md")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Export", color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.7f))
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun CrashEntryCard(
    entry: com.example.util.CrashReporter.CrashEntry,
    accentColor: Color,
    onBgColor: Color,
    onCopyStack: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val (badgeBg, badgeText, badgeLabel) = when {
        entry.isSystemExit -> Triple(Color(0xFFE65100).copy(alpha = 0.25f), Color(0xFFFF9800), "âš ï¸ SYSTEM EXIT / ANR")
        entry.isFatal -> Triple(Color.Red.copy(alpha = 0.25f), Color(0xFFFF5252), "ðŸ”¥ FATAL CRASH")
        entry.tag.contains("Diagnostic") -> Triple(Color(0xFF0288D1).copy(alpha = 0.25f), Color(0xFF4FC3F7), "ðŸ§ª TEST DIAGNOSTIC")
        else -> Triple(Color(0xFFFBC02D).copy(alpha = 0.25f), Color(0xFFFFD54F), "âš ï¸ ERROR")
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Badge & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = badgeText
                    )
                }
                Text(
                    entry.formattedTime,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Exception & Tag
            Text(
                text = entry.exceptionType,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = onBgColor
            )

            if (entry.message.isNotBlank()) {
                Text(
                    text = entry.message,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata row
            Text(
                text = "Thread: ${entry.threadName} (UI: ${entry.isUiThread}) | Mem: ${entry.memoryUsage}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor.copy(alpha = 0.5f)
            )

            // Breadcrumbs if present
            if (entry.breadcrumbs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Recent Actions: ${entry.breadcrumbs.takeLast(2).joinToString(" â†’ ")}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stack trace expandable section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (isExpanded) "Hide Stack Trace â–²" else "View Stack Trace â–¼",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    )
                }

                IconButton(
                    onClick = { onCopyStack(entry.stackTrace) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy Stack Trace",
                        tint = onBgColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    val scroll = rememberScrollState()
                    Text(
                        text = entry.stackTrace.trim(),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(scroll)
                    )
                }
            }
        }
    }
}
