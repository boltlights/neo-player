with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    val activeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()''',
'''    val activeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
    val useBlackTheme by viewModel.useBlackTheme.collectAsStateWithLifecycle()'''
)

content = content.replace(
'''        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(MusicThemeStyle.entries.toTypedArray()) { style ->''',
'''        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
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
                            color = onBgColor
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
                            checkedThumbColor = surfaceColor,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = onBgColor.copy(alpha = 0.5f),
                            uncheckedTrackColor = surfaceColor
                        )
                    )
                }
            }
            items(MusicThemeStyle.entries.toTypedArray()) { style ->'''
)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
