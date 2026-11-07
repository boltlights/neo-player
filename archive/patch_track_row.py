with open('app/src/main/java/com/example/ui/components/TrackRowItem.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''            .padding(horizontal = 16.dp, vertical = 8.dp),''',
'''            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),'''
)

# Also let's make the text slightly better.
content = content.replace(
'''            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) accentColor else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} • ${track.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )''',
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
            )'''
)

with open('app/src/main/java/com/example/ui/components/TrackRowItem.kt', 'w') as f:
    f.write(content)
