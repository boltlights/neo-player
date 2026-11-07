import re

with open('app/src/main/java/com/example/util/CoilBitmapLoader.kt', 'r') as f:
    content = f.read()

target = r'''class CoilBitmapLoader\(private val context: Context\) : BitmapLoader \{
    private val scope = CoroutineScope\(Dispatchers\.IO\)'''

replacement = r'''class CoilBitmapLoader(private val context: Context) : BitmapLoader {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun supportsMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }'''

new_content = re.sub(target, replacement, content)

with open('app/src/main/java/com/example/util/CoilBitmapLoader.kt', 'w') as f:
    f.write(new_content)
