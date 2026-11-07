with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    lines = f.readlines()

count = 0
for i, line in enumerate(lines):
    for char in line:
        if char == '{':
            count += 1
        elif char == '}':
            count -= 1
    if count > 1:
        print(f"Line {i+1} indent {len(line) - len(line.lstrip())} count {count}: {line.strip()}")

print(f"Final brace count: {count}")
