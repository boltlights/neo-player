import re
with open('app/src/main/java/com/example/util/CoilBitmapLoader.kt', 'r') as f:
    content = f.read()

target = r'''\.size\(512\) // Optimize for notification'''
repl = r'''.size(320) // Use 320 to avoid TransactionTooLargeException in Samsung Quick Settings IPC'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/util/CoilBitmapLoader.kt', 'w') as f:
    f.write(content)
