import re
with open('app/src/main/java/com/example/PlaybackService.kt', 'r') as f:
    content = f.read()

target = r'''            \.setChannelName\(R\.string\.app_name\)
            \.setSmallIcon\(R\.drawable\.ic_launcher_foreground\) // Required for Samsung quick settings
            \.build\(\)'''

repl = r'''            .setChannelName(R.string.app_name)
            .build()'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackService.kt', 'w') as f:
    f.write(content)
