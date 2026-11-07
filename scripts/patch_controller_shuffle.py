with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'setWakeMode(C.WAKE_MODE_LOCAL)',
    'setWakeMode(C.WAKE_MODE_LOCAL)\n                setShuffleOrder(com.example.util.BetterShuffleOrder(0))'
)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
