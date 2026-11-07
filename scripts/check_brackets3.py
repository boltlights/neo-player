with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    lines = f.readlines()

count = 0
in_func = False
for i, line in enumerate(lines):
    if i >= 590 and i <= 770:
        if '{' in line and not in_func:
            in_func = True
        if in_func:
            count += line.count('{') - line.count('}')
            print(f"Line {i+1} count: {count} - {line.strip()}")
