package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.AlbumArtStyle
import com.example.AppThemeMode
import com.example.LibraryAlbumArtStyle
import com.example.MusicThemeStyle
import com.example.MusicViewModel

@Composable
fun AppearanceSettingsScreen(
    accentColor: Color,
    surfaceColor: Color,
    onBack: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onNavigateToAlbumArt: () -> Unit,
    onNavigateToLibraryTabs: () -> Unit
) {
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
                    "APPEARANCE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Text(
                "CUSTOMIZE THEMES, ALBUM ART & LIBRARY TABS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Themes sub-option
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToThemes() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ColorLens,
                    contentDescription = "Themes",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Themes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        "Neo Classic, Neo Retro & color customization",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Album Art sub-option
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToAlbumArt() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Album,
                    contentDescription = "Album Art",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Album Art",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        "Player style, rotating CD & palette engine",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Library Tabs sub-option
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToLibraryTabs() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Tab,
                    contentDescription = "Library Tabs",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Library Tabs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        "Customize library tabs visibility and order",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun AlbumArtSettingsScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onBack: () -> Unit
) {
    val albumArtStyle by viewModel.albumArtStyle.collectAsStateWithLifecycle()
    val libraryAlbumArtStyle by viewModel.libraryAlbumArtStyle.collectAsStateWithLifecycle()
    val rotateCd by viewModel.rotateCd.collectAsStateWithLifecycle()
    val liquidPaletteEnabled by viewModel.liquidPaletteEnabled.collectAsStateWithLifecycle()

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
                    "ALBUM ART",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Text(
                "CONFIGURE ALBUM ART APPEARANCE",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Text(
            "Currently Playing Album Shape",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        AlbumArtStyle.entries.forEach { style ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.setAlbumArtStyle(style) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = albumArtStyle == style,
                    onClick = { viewModel.setAlbumArtStyle(style) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = accentColor,
                        unselectedColor = onBgColor.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    style.displayName,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Library Album Art Shape",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        LibraryAlbumArtStyle.entries.forEach { style ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.setLibraryAlbumArtStyle(style) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = libraryAlbumArtStyle == style,
                    onClick = { viewModel.setLibraryAlbumArtStyle(style) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = accentColor,
                        unselectedColor = onBgColor.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    style.displayName,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        val isCdSelected = albumArtStyle == AlbumArtStyle.CD

        Text(
            "Animation",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isCdSelected) Color.White else onBgColor.copy(alpha = 0.3f),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = isCdSelected) { viewModel.setRotateCd(!rotateCd) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Rotate CD",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isCdSelected) onBgColor else onBgColor.copy(alpha = 0.3f)
            )
            Checkbox(
                checked = rotateCd,
                onCheckedChange = { viewModel.setRotateCd(it) },
                enabled = isCdSelected,
                colors = CheckboxDefaults.colors(
                    checkedColor = accentColor,
                    uncheckedColor = onBgColor.copy(alpha = 0.4f),
                    disabledCheckedColor = accentColor.copy(alpha = 0.3f),
                    disabledUncheckedColor = onBgColor.copy(alpha = 0.1f)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Appearance",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.setLiquidPaletteEnabled(!liquidPaletteEnabled) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Liquid Palette Engine",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Switch(
                checked = liquidPaletteEnabled,
                onCheckedChange = { viewModel.setLiquidPaletteEnabled(it) },
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

@Composable
fun ThemesScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onBack: () -> Unit
) {
    val activeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
    val activeThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val useBlackTheme by viewModel.useBlackTheme.collectAsStateWithLifecycle()

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
                    "THEMES",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Text(
                "SELECT APP THEME & INTERFACE COLORWAYS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = onBgColor.copy(alpha = 0.6f)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. THEMES SECTION
            item {
                Text(
                    "THEMES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Text(
                    "Select overall interface theme (tap to activate)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Neo Classic card
            item {
                val isSelected = activeThemeMode == AppThemeMode.NEO_CLASSIC
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAppThemeMode(AppThemeMode.NEO_CLASSIC) }
                        .border(
                            2.dp,
                            if (isSelected) accentColor else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentColor.copy(alpha = 0.2f) else onBgColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                tint = if (isSelected) accentColor else onBgColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "NEO CLASSIC",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) accentColor else Color.White
                                )
                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accentColor.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
                                    ) {
                                        Text(
                                            "ACTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                "Default modern sleek audio player interface",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = onBgColor.copy(alpha = 0.6f)
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                    }
                }
            }

            // Neo Retro card
            item {
                val isSelected = activeThemeMode == AppThemeMode.NEO_RETRO
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAppThemeMode(AppThemeMode.NEO_RETRO) }
                        .border(
                            2.dp,
                            if (isSelected) accentColor else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentColor.copy(alpha = 0.2f) else onBgColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isSelected) accentColor else onBgColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "NEO RETRO",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) accentColor else Color.White
                                )
                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accentColor.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
                                    ) {
                                        Text(
                                            "ACTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                "Vintage tape cassette player with rotating reels & tactile keys",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = onBgColor.copy(alpha = 0.6f)
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                    }
                }
            }

            // 2. COLORS SUBSECTION HEADER
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = onBgColor.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "COLORS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Customize accent color palettes & background tint",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // PURE AMOLED BLACK
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "PURE AMOLED BLACK",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            "Use pitch black backgrounds in dark mode",
                            fontSize = 11.sp,
                            color = onBgColor.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = useBlackTheme,
                        onCheckedChange = { viewModel.setUseBlackTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = onBgColor.copy(alpha = 0.5f),
                            uncheckedTrackColor = surfaceColor
                        )
                    )
                }
            }
            items(MusicThemeStyle.entries.toTypedArray()) { style ->
                val isSelected = style == activeStyle
                val context = LocalContext.current
                val isSAndAbove = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                val isDark = isSystemInDarkTheme()

                val (bg, surf, prim, acc) = if (style == MusicThemeStyle.DYNAMIC_SYSTEM && isSAndAbove) {
                    val dynScheme = if (isDark) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                    listOf(dynScheme.background, dynScheme.surface, dynScheme.primary, dynScheme.secondary)
                } else {
                    listOf(Color(style.background), Color(style.surface), Color(style.primary), Color(style.accent))
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.changeThemeStyle(style) }
                        .border(
                            2.dp,
                            if (isSelected) prim else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                style.displayName.uppercase(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = prim
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(acc)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Theme horizontal palette demo row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(bg).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp)))
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(surf))
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(prim))
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(acc))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryTabsSettingsScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onBack: () -> Unit
) {
    val libraryTabs by viewModel.libraryTabs.collectAsStateWithLifecycle()
    // "Genre" matches the default saved tab name; this list used to say "Genres", so the same tab
    // was offered twice ("Genre" visible + "Genres" hidden) and enabling both showed it twice.
    val allAvailableTabs = listOf("Songs", "Albums", "Artists", "Genre", "Playlists")
    
    // We use a local state to handle drag & drop visually before saving
    var currentTabs by remember(libraryTabs) { mutableStateOf(libraryTabs) }
    
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
                    "LIBRARY TABS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
            Text(
                "CUSTOMIZE TAB VISIBILITY AND ORDER",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Visible Tabs",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .padding(16.dp)
            ) {
                itemsIndexed(items = currentTabs) { index: Int, tab: String ->
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                currentTabs = currentTabs.toMutableList().apply {
                                                    val temp = get(index - 1)
                                                    set(index - 1, get(index))
                                                    set(index, temp)
                                                }
                                                viewModel.updateLibraryTabs(currentTabs)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp),
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (index > 0) Color.White else Color.Transparent)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < currentTabs.size - 1) {
                                                currentTabs = currentTabs.toMutableList().apply {
                                                    val temp = get(index + 1)
                                                    set(index + 1, get(index))
                                                    set(index, temp)
                                                }
                                                viewModel.updateLibraryTabs(currentTabs)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp),
                                        enabled = index < currentTabs.size - 1
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (index < currentTabs.size - 1) Color.White else Color.Transparent)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tab,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                            Switch(
                                checked = true,
                                onCheckedChange = { isChecked ->
                                    if (!isChecked) {
                                        val newTabs = currentTabs.filter { it != tab }
                                        currentTabs = newTabs
                                        viewModel.updateLibraryTabs(newTabs)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
                
                // Show hidden tabs at the bottom
                val hiddenTabs = allAvailableTabs.filter { !currentTabs.contains(it) }
                items(hiddenTabs) { tab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(36.dp))
                            Text(
                                text = tab,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = false,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val newTabs = currentTabs + tab
                                    currentTabs = newTabs
                                    viewModel.updateLibraryTabs(newTabs)
                                }
                            },
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
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Note: Changing tab visibility and order updates the library layout immediately.",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = onBgColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
