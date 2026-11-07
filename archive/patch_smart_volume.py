import re

with open('app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = accentColor
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Zone", tint = Color.Black)
            }
        }
    ) { padding ->''',
'''    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->'''
)

content = content.replace(
'''            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = accentColor)
                }
                Text(
                    "DRIVER MODE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )
            }''',
'''            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = accentColor)
                }
                Text(
                    "DRIVER MODE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    modifier = Modifier.offset(x = (-12).dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        "ADD",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }'''
)

content = content.replace(
'''                    Text(
                        "No zones configured.\nTap + to add one.",
                        color = onBgColor.copy(alpha = 0.5f),''',
'''                    Text(
                        "No zones configured.\nTap ADD to add one.",
                        color = onBgColor.copy(alpha = 0.5f),'''
)

with open('app/src/main/java/com/example/ui/screens/SmartVolumeScreen.kt', 'w') as f:
    f.write(content)
