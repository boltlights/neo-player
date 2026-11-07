with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "_includedFolders.value = newSet" in line or "_excludedFolders.value = newSet" in line:
        continue
    if 'sharedPrefs.edit().putStringSet' in line:
        continue
    # skip the closing brace after apply()
    if skip:
        skip = False
        continue
    if 'apply()' in line:
        skip = True
        continue
    
    # Wait, the closing brace is on the next line.
    new_lines.append(line)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.writelines(new_lines)
