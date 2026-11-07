import re

with open('app/src/main/java/com/example/util/MetadataExtractor.kt', 'r') as f:
    content = f.read()

old_temp = """                        val extension = uriString.substringAfterLast('.', "mp3")
                        tempFile = File.createTempFile("metadata_temp", ".$extension", context.cacheDir)"""

new_temp = """                        val extension = fallbackPath?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() } ?: uriString.substringAfterLast('.', "mp3")
                        tempFile = File.createTempFile("metadata_temp", ".$extension", context.cacheDir)"""

content = content.replace(old_temp, new_temp)

with open('app/src/main/java/com/example/util/MetadataExtractor.kt', 'w') as f:
    f.write(content)
