import re

with open('app/src/main/java/com/example/ui/components/NavigationComponents.kt', 'r') as f:
    content = f.read()

content = re.sub(
r'''@Composable
fun ActiveBottomBar\(
    track: TrackEntity,
    isPlaying: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onPlayPauseToggle: \(\) -> Unit,
    onOpenPlayer: \(\) -> Unit
\) \{.*
    Surface\(
        modifier = Modifier
            \.fillMaxWidth\(\)
            \.shadow\(elevation = 8\.dp, clip = false\)
            \.height\(72\.dp\)
            \.clickable \{ onOpenPlayer\(\) \},
        color = surfaceColor,
    \) \{
        Column \{
            // High-precision horizontal slider divider representing playing buffer track ratio
            Box\(
                modifier = Modifier
                    \.fillMaxWidth\(\)
                    \.height\(2\.dp\)
                    \.background\(Color\.White\.copy\(alpha = 0\.1f\)\)
            \)
            Row\(
                modifier = Modifier
                    \.fillMaxSize\(\)
                    \.padding\(horizontal = 16\.dp\),
                verticalAlignment = Alignment\.CenterVertically
            \) \{
                // Mini Art
                TrackArtworkWidget\(
                    artPath = track\.artPath,
                    trackTitle = track\.title,
                    accentColor = accentColor,
                    modifier = Modifier
                        \.size\(38\.dp\)
                        \.clip\(RoundedCornerShape\(8\.dp\)\),
                    sizeLimit = 180
                \)
                Spacer\(modifier = Modifier\.width\(12\.dp\)\)
                // Metadata block
                Column\(modifier = Modifier\.weight\(1f\)\) \{
                    Text\(
                        track\.title,
                        fontSize = 13\.sp,
                        fontWeight = FontWeight\.Bold,
                        maxLines = 1,
                        overflow = TextOverflow\.Ellipsis
                    \)
                    Text\(
                        track\.artist,
                        fontSize = 10\.sp,
                        color = onBgColor\.copy\(alpha = 0\.6f\),
                        maxLines = 1,
                        overflow = TextOverflow\.Ellipsis
                    \)
                \}
                // Controls
                IconButton\(onClick = onPlayPauseToggle\) \{
                    if \(isPlaying\) \{
                        PauseIcon\(color = accentColor, modifier = Modifier\.size\(14\.dp\)\)
                    \} else \{
                        PlayIcon\(color = accentColor, modifier = Modifier\.size\(14\.dp\)\)
                    \}
                \}
            \}
        \}
    \}
\}''',
'''@Composable
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
}''', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/components/NavigationComponents.kt', 'w') as f:
    f.write(content)
