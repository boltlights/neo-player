import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                            if (filteredTracks.isEmpty()) {
                                Text(
                                    "No tracks found matching query",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp)
                                        .padding(top = totalTopPadding + 32.dp),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {''',
'''                            if (filteredTracks.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = totalTopPadding + 100.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(96.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = com.example.ui.components.CookieShape()
                                            )
                                            .padding(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        if (searchQuery.isNotEmpty()) "No tracks found matching query" else "It\'s quite lonely in here.\nAdd some songs to get started.",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 16.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.width(256.dp)
                                    )
                                    if (searchQuery.isEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { /* Instruct user */ Toast.makeText(context, "Use the + button at the top to add songs", Toast.LENGTH_LONG).show() }) {
                                            Text(
                                                "ADD LOCATIONS",
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            } else {'''
)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
