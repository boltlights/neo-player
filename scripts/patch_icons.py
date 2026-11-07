with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('Icons.Default.Sort', 'Icons.AutoMirrored.Filled.Sort')
content = content.replace('Icons.Default.PlaylistAdd', 'Icons.AutoMirrored.Filled.PlaylistAdd')
content = content.replace('Icons.Default.QueueMusic', 'Icons.AutoMirrored.Filled.QueueMusic')
content = content.replace('Icons.Default.ArrowBack', 'Icons.AutoMirrored.Filled.ArrowBack')
content = content.replace('Icons.Default.LibraryMusic', 'Icons.Filled.LibraryMusic')

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
