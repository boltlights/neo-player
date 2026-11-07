with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                                    Text(
                                        if (searchQuery.isNotEmpty()) "No tracks found matching query" else "It\'s quite lonely in here.Add some songs to get started.",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 16.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.width(256.dp)
                                    )''',
'''                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "No tracks found matching query" else "It is quite lonely in here.\\nAdd some songs to get started.",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 16.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.width(256.dp)
                                    )'''
)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
