with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    lines = f.readlines()

count = 0
for i, line in enumerate(lines):
    old_count = count
    for char in line:
        if char == '{':
            count += 1
        elif char == '}':
            count -= 1
    if old_count == 1 and count > 1:
        print(f"Opened function at line {i+1}: {line.strip()}")
    if old_count != 1 and count == 1:
        print(f"Returned to class level at line {i+1}")

print(f"Final brace count: {count}")
