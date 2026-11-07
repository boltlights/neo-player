import re
with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

# Replace header colors
content = re.sub(
    r'''Text\(\s*"UPCOMING QUEUE",\s*fontSize = 16\.sp,\s*fontWeight = FontWeight\.Bold,\s*fontFamily = FontFamily\.Monospace,\s*color = [^\n]+''',
    r'''Text(\n                                "UPCOMING QUEUE",\n                                fontSize = 16.sp,\n                                fontWeight = FontWeight.Bold,\n                                fontFamily = FontFamily.Monospace,\n                                color = androidx.compose.ui.graphics.Color.White''',
    content
)

content = re.sub(
    r'''Text\(\s*"\$\{queueTracks\.size\} tracks • Drag handle to reorder",\s*fontSize = 11\.sp,\s*fontFamily = FontFamily\.Monospace,\s*color = [^\n]+''',
    r'''Text(\n                                "${queueTracks.size} tracks • Drag handle to reorder",\n                                fontSize = 11.sp,\n                                fontFamily = FontFamily.Monospace,\n                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)''',
    content
)

content = re.sub(
    r'''Text\(\s*"Queue is empty",\s*color = onBgColor\.copy\(alpha = 0\.5f\),\s*fontSize = 14\.sp,\s*fontFamily = FontFamily\.Monospace\s*\)''',
    r'''Text(\n                                "Queue is empty",\n                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),\n                                fontSize = 14.sp,\n                                fontFamily = FontFamily.Monospace\n                            )''',
    content
)

# Surface background
content = re.sub(
    r'''color = if \(isCurrentlyPlaying\) accentColor\.copy\(alpha = 0\.2f\) else surfaceColor\.copy\(alpha = 0\.7f\),''',
    r'''color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),''',
    content
)

content = re.sub(
    r'''border = if \(isCurrentlyPlaying\) androidx\.compose\.foundation\.BorderStroke\(1\.dp, accentColor\.copy\(alpha = 0\.6f\)\) else null,''',
    r'''border = if (isCurrentlyPlaying) androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)) else null,''',
    content
)

# Playing indicator / track number background
content = re.sub(
    r'''\.background\(if \(isCurrentlyPlaying\) accentColor else onBgColor\.copy\(alpha = 0\.1f\)\),''',
    r'''.background(if (isCurrentlyPlaying) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),''',
    content
)

# Playing indicator icon
content = re.sub(
    r'''Icon\(\s*Icons\.Default\.GraphicEq,\s*contentDescription = "Playing",\s*tint = Color\.Black,\s*modifier = Modifier\.size\(18\.dp\)\s*\)''',
    r'''Icon(\n                                                            Icons.Default.GraphicEq,\n                                                            contentDescription = "Playing",\n                                                            tint = androidx.compose.ui.graphics.Color.Black,\n                                                            modifier = Modifier.size(18.dp)\n                                                        )''',
    content
)

# Track number text color
content = re.sub(
    r'''color = onBgColor\.copy\(alpha = 0\.7f\)\s*\)''',
    r'''color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)\n                                                        )''',
    content
)

# Title text color
content = re.sub(
    r'''color = if \(isCurrentlyPlaying\) accentColor else onBgColor,''',
    r'''color = androidx.compose.ui.graphics.Color.White,''',
    content
)

# Artist text color
content = re.sub(
    r'''color = onBgColor\.copy\(alpha = 0\.6f\),''',
    r'''color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),''',
    content
)

# Drag handle tint
content = re.sub(
    r'''tint = if \(isCurrentlyPlaying\) accentColor else onBgColor\.copy\(alpha = 0\.6f\),''',
    r'''tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),''',
    content
)


with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
