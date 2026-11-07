with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

import re

# Inject flow collection for the new settings
content = content.replace(
'''    val hideSampleMusicButton by viewModel.hideSampleMusicButton.collectAsStateWithLifecycle()''',
'''    val hideSampleMusicButton by viewModel.hideSampleMusicButton.collectAsStateWithLifecycle()
    val intelligentSorting by viewModel.intelligentSorting.collectAsStateWithLifecycle()
    val separators by viewModel.separators.collectAsStateWithLifecycle()
    val useFileTreeCache by viewModel.useFileTreeCache.collectAsStateWithLifecycle()
    val locationMode by viewModel.locationMode.collectAsStateWithLifecycle()
    val autoSyncMediaStore by viewModel.autoSyncMediaStore.collectAsStateWithLifecycle()'''
)

# Append the UI options right after the Smart Volume section in OthersSettingsScreen
# Let's find where Smart Volume is
