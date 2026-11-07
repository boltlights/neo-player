import re

with open('app/src/main/java/com/example/ui/components/NavigationComponents.kt', 'r') as f:
    content = f.read()

# Fix the duplicate progress declarations in FloatingWidgetOverlay
content = re.sub(
r'''    val playbackProgress by viewModel\.playbackController\.playbackProgress\.collectAsStateWithLifecycle\(\)
    val progressVal = if \(playbackProgress\.duration <= 0 \|\| playbackProgress\.isBuffering\) \{
        0f
    \} else \{
        \(playbackProgress\.position\.toFloat\(\) / playbackProgress\.duration\.toFloat\(\)\)\.coerceIn\(0f, 1f\)
    \}

    val playbackProgress by viewModel\.playbackController\.playbackProgress\.collectAsStateWithLifecycle\(\)
    val progressVal = if \(playbackProgress\.duration <= 0 \|\| playbackProgress\.isBuffering\) \{
        0f
    \} else \{
        \(playbackProgress\.position\.toFloat\(\) / playbackProgress\.duration\.toFloat\(\)\)\.coerceIn\(0f, 1f\)
    \}''',
r'''    val playbackProgress by viewModel.playbackController.playbackProgress.collectAsStateWithLifecycle()
    val progressVal = if (playbackProgress.duration <= 0 || playbackProgress.isBuffering) {
        0f
    } else {
        (playbackProgress.position.toFloat() / playbackProgress.duration.toFloat()).coerceIn(0f, 1f)
    }''', content)

# Replace the old ActiveBottomBar with the new one
old_active_bottom_bar = re.search(r'@Composable\nfun ActiveBottomBar\(.*?^\}', content, re.DOTALL | re.MULTILINE)
if old_active_bottom_bar:
    new_active_bottom_bar = '''@Composable
fun ActiveBottomBar(
    track: TrackEntity,
    isPlaying: Boolean,
    progressVal: Float,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onSkipPrevious: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, clip = false)
            .clickable { onOpenPlayer() },
        color = surfaceColor,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Art
                TrackArtworkWidget(
                    artPath = track.artPath,
                    trackTitle = track.title,
                    accentColor = accentColor,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    sizeLimit = 180
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                // Metadata block
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist,
                        fontSize = 12.sp,
                        color = onBgColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = onBgColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isPlaying) {
                            PauseIcon(color = accentColor, modifier = Modifier.size(16.dp))
                        } else {
                            PlayIcon(color = accentColor, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = onBgColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Linear Progress Indicator at the bottom edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(onBgColor.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressVal)
                        .background(accentColor)
                )
            }
        }
    }
}'''
    content = content[:old_active_bottom_bar.start()] + new_active_bottom_bar + content[old_active_bottom_bar.end():]
    
with open('app/src/main/java/com/example/ui/components/NavigationComponents.kt', 'w') as f:
    f.write(content)
