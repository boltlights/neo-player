with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

import re

# Insert states
content = content.replace(
'''    val hideSampleMusicButton by viewModel.hideSampleMusicButton.collectAsStateWithLifecycle()''',
'''    val hideSampleMusicButton by viewModel.hideSampleMusicButton.collectAsStateWithLifecycle()
    val intelligentSorting by viewModel.intelligentSorting.collectAsStateWithLifecycle()
    val useFileTreeCache by viewModel.useFileTreeCache.collectAsStateWithLifecycle()
    val locationMode by viewModel.locationMode.collectAsStateWithLifecycle()
    val autoSyncMediaStore by viewModel.autoSyncMediaStore.collectAsStateWithLifecycle()'''
)

# Insert the new settings in the "Library" section
library_settings_str = '''        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.setHideSampleMusicButton(!hideSampleMusicButton) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Hide sample music button",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor
            )
            Switch(
                checked = hideSampleMusicButton,
                onCheckedChange = { viewModel.setHideSampleMusicButton(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = onBgColor.copy(alpha = 0.6f),
                    uncheckedTrackColor = onBgColor.copy(alpha = 0.2f)
                )
            )
        }'''

new_library_settings_str = library_settings_str + '''
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.setIntelligentSorting(!intelligentSorting) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Intelligent Sorting",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor
            )
            Switch(
                checked = intelligentSorting,
                onCheckedChange = { viewModel.setIntelligentSorting(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = onBgColor.copy(alpha = 0.6f),
                    uncheckedTrackColor = onBgColor.copy(alpha = 0.2f)
                )
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.setUseFileTreeCache(!useFileTreeCache) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Use File-System Cache",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor
            )
            Switch(
                checked = useFileTreeCache,
                onCheckedChange = { viewModel.setUseFileTreeCache(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = onBgColor.copy(alpha = 0.6f),
                    uncheckedTrackColor = onBgColor.copy(alpha = 0.2f)
                )
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.setAutoSyncMediaStore(!autoSyncMediaStore) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Auto-Sync Media Store",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = onBgColor
            )
            Switch(
                checked = autoSyncMediaStore,
                onCheckedChange = { viewModel.setAutoSyncMediaStore(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = onBgColor.copy(alpha = 0.6f),
                    uncheckedTrackColor = onBgColor.copy(alpha = 0.2f)
                )
            )
        }
        
        Text(
            "Location Mode",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        listOf("SAF", "MEDIA_STORE").forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.setLocationMode(mode) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = locationMode == mode,
                    onClick = { viewModel.setLocationMode(mode) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = accentColor,
                        unselectedColor = onBgColor.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (mode == "SAF") "Storage Access Framework" else "Media Store Database",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = onBgColor
                )
            }
        }
'''

content = content.replace(library_settings_str, new_library_settings_str)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
