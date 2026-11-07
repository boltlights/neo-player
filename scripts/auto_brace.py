import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    lines = f.readlines()

out_lines = []
count = 0
for i, line in enumerate(lines):
    # Check if this line is a top-level declaration in the class
    stripped = line.lstrip()
    indent = len(line) - len(stripped)
    
    if indent == 4 and (stripped.startswith('fun ') or stripped.startswith('private fun ') or stripped.startswith('val ') or stripped.startswith('private val ') or stripped.startswith('override fun ') or stripped.startswith('@OptIn')):
        # We should be at count == 1 here.
        while count > 1:
            out_lines.append(('    ' * (count - 1)) + '}\n')
            count -= 1
            
    # Also if it's the end of the class
    if line.startswith('}'):
        while count > 1:
            out_lines.append(('    ' * (count - 1)) + '}\n')
            count -= 1

    out_lines.append(line)
    
    for char in line:
        if char == '{':
            count += 1
        elif char == '}':
            count -= 1

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.writelines(out_lines)
