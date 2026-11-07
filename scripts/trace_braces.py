with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    lines = f.readlines()

count = 0
for i, line in enumerate(lines):
    for char in line:
        if char == '{':
            count += 1
        elif char == '}':
            count -= 1
    if count < 0:
        print(f"Error: unmatched closing brace at line {i+1}: {line.strip()}")
        break
    if count == 0 and "class MusicViewModel" in "".join(lines[:i+1]):
        print(f"Class closed prematurely at line {i+1}: {line.strip()}")
        break

print(f"Final brace count: {count}")
