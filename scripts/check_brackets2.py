with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    lines = f.readlines()

count = 0
in_func = False
for i, line in enumerate(lines):
    if i >= 590:
        if '{' in line and not in_func:
            in_func = True
        if in_func:
            count += line.count('{') - line.count('}')
            if count == 0:
                print(f"Function closed at line {i+1}")
                break
