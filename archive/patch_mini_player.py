with open('app/src/main/java/com/example/ui/components/NavigationComponents.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, clip = false)
            .height(64.dp)
            .clickable { onOpenPlayer() },''',
'''    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, clip = false)
            .height(72.dp)
            .clickable { onOpenPlayer() },'''
)

content = content.replace(
'''                TrackArtworkWidget(
                    artPath = track.artPath,
                    trackTitle = track.title,
                    accentColor = accentColor,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    sizeLimit = 180
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Metadata block
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist,
                        fontSize = 11.sp,
                        color = onBgColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }''',
'''                TrackArtworkWidget(
                    artPath = track.artPath,
                    trackTitle = track.title,
                    accentColor = accentColor,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    sizeLimit = 180
                )
                Spacer(modifier = Modifier.width(16.dp))
                // Metadata block
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        track.artist,
                        fontSize = 12.sp,
                        color = onBgColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }'''
)

with open('app/src/main/java/com/example/ui/components/NavigationComponents.kt', 'w') as f:
    f.write(content)
