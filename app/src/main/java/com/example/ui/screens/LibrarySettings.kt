package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MusicViewModel

@Composable
fun MusicSourcesScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onBack: () -> Unit
) {
    val includedFolders by viewModel.includedFolders.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val autoRefreshEnabled by viewModel.autoRefreshEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val includedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addIncludedFolder(uri.toString())
        }
    }

    val excludedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addExcludedFolder(uri.toString())
        }
    }

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
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = accentColor)
                }
                Text(
                    "MUSIC SOURCES",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Text(
                "MANAGE AUDIO FOLDERS AND AUTO-RESYNC",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Included Directories",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            IconButton(onClick = { includedLauncher.launch(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Included Folder", tint = accentColor)
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (includedFolders.isEmpty()) {
                    Text(
                        text = "Global MediaStore mode. All folders on device will be scanned. Add a folder to restrict scanning.",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor.copy(alpha = 0.6f)
                    )
                } else {
                    includedFolders.forEach { uriString ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Uri.parse(uriString).path?.split(":")?.lastOrNull() ?: uriString,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(
                                    text = uriString,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = onBgColor.copy(alpha = 0.4f),
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { viewModel.removeIncludedFolder(uriString) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Excluded Directories",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            IconButton(onClick = { excludedLauncher.launch(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Excluded Folder", tint = accentColor)
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (excludedFolders.isEmpty()) {
                    Text(
                        text = "No directories excluded. All music in included folders (or entire device) will be scanned.",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = onBgColor.copy(alpha = 0.6f)
                    )
                } else {
                    excludedFolders.forEach { uriString ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Uri.parse(uriString).path?.split(":")?.lastOrNull() ?: uriString,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(
                                    text = uriString,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = onBgColor.copy(alpha = 0.4f),
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { viewModel.removeExcludedFolder(uriString) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Synchronize Settings",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        val autoSyncMediaStore by viewModel.autoSyncMediaStore.collectAsStateWithLifecycle()

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            "Live Auto-Sync (MediaStore)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Automatically detects when files are transferred from PC/USB or saved, and syncs your library in the background without needing manual import.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = autoSyncMediaStore,
                        onCheckedChange = { viewModel.setAutoSyncMediaStore(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = onBgColor.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            "Auto Refresh on App Launch",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "When enabled, the app automatically rescans the configured folder to import new tracks on start-up.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = autoRefreshEnabled,
                        onCheckedChange = { viewModel.setAutoRefreshEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Library Options
        Text(
            "Library",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        val hideSampleMusicButton by viewModel.hideSampleMusicButton.collectAsStateWithLifecycle()
        val intelligentSorting by viewModel.intelligentSorting.collectAsStateWithLifecycle()
        val useFileTreeCache by viewModel.useFileTreeCache.collectAsStateWithLifecycle()

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setHideSampleMusicButton(!hideSampleMusicButton) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            "Hide sample music button",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Hide the quick load sample audio button on empty library screens",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = hideSampleMusicButton,
                        onCheckedChange = { viewModel.setHideSampleMusicButton(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = onBgColor.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setIntelligentSorting(!intelligentSorting) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            "Intelligent Sorting",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Automatically ignore track numbers and prefixes when sorting track titles",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = intelligentSorting,
                        onCheckedChange = { viewModel.setIntelligentSorting(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = onBgColor.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setUseFileTreeCache(!useFileTreeCache) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            "Use File-System Cache",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Cache document tree structures locally for faster folder browsing",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = useFileTreeCache,
                        onCheckedChange = { viewModel.setUseFileTreeCache(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Location Mode
        Text(
            "Location Mode",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        val locationMode by viewModel.locationMode.collectAsStateWithLifecycle()

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf(
                    "SAF" to ("Storage Access Framework" to "Standard Android document tree directory picker"),
                    "MEDIA_STORE" to ("Media Store Database" to "Direct index of media scanned by the Android OS")
                ).forEachIndexed { index, (mode, info) ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = onBgColor.copy(alpha = 0.1f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setLocationMode(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = locationMode == mode,
                            onClick = { viewModel.setLocationMode(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = onBgColor.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                info.first,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                info.second,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = onBgColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
