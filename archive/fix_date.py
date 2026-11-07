with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                                                    cal.timeInMillis = song.dateAdded * 1000L''',
'''                                                    cal.timeInMillis = song.addedAt'''
)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
