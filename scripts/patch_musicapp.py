with open('app/src/main/java/com/example/MusicApplication.kt', 'r') as f:
    content = f.read()

content = content.replace("super.onCreate()", "super.onCreate()\n        com.example.util.CrashReporter.init(this)")

with open('app/src/main/java/com/example/MusicApplication.kt', 'w') as f:
    f.write(content)
