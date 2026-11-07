import re
import os

files_to_check = [
    'app/src/main/java/com/example/MusicViewModel.kt',
    'app/src/main/java/com/example/ui/screens/LibraryScreen.kt',
    'app/src/main/java/com/example/ui/screens/SettingsScreen.kt'
]

for file_path in files_to_check:
    if not os.path.exists(file_path):
        continue
    with open(file_path, 'r') as f:
        content = f.read()

    if 'MusicViewModel.kt' in file_path:
        content = re.sub(r'fun setThemeStyle\([^)]*\)\s*\{[^}]*\}', '', content)
        content = re.sub(r'fun setSortOption\([^)]*\)\s*\{[^}]*\}', '', content)
        content = re.sub(r'fun setLibraryTabs\([^)]*\)\s*\{[^}]*\}', '', content)

    content = content.replace('viewModel.setThemeStyle', 'viewModel.changeThemeStyle')
    content = content.replace('viewModel.setSortOption', 'viewModel.updateSortOption')
    content = content.replace('viewModel.setLibraryTabs', 'viewModel.updateLibraryTabs')

    with open(file_path, 'w') as f:
        f.write(content)

