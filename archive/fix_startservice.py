import re
with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

target = r'''            try \{
                val serviceIntent = Intent\(context, PlaybackService::class\.java\)
                context\.startService\(serviceIntent\)
            \} catch \(e: Exception\) \{
                e\.printStackTrace\(\)
            \}'''
repl = r'''            try {
                val serviceIntent = Intent(context, PlaybackService::class.java)
                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }'''
content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
