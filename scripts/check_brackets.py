with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    lines = f.readlines()

count = 0
for i, line in enumerate(lines):
    if i >= 590:
        count += line.count('{') - line.count('}')
        if count == 0 and i > 592:
            print(f"Brackets closed at line {i+1}")
            break
