with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if i == 765:
        continue # Skip the extra }
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.writelines(new_lines)
