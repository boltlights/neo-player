with open('app/src/main/java/com/example/ui/components/TrackRowItem.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) accentColor else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )''',
'''            Text(
                text = track.title,
                fontSize = 15.sp,
                color = if (isActive) accentColor else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )'''
)

with open('app/src/main/java/com/example/ui/components/TrackRowItem.kt', 'w') as f:
    f.write(content)
