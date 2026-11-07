with open('app/src/main/java/com/example/util/DirectoryScanner.kt', 'r') as f:
    content = f.read()

helper = """
    private fun scanDocumentFileFast(
        context: Context,
        treeUri: Uri,
        documentId: String,
        excludedUris: Set<String>,
        results: MutableList<Pair<Uri, String?>>
    ) {
        val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val files = mutableListOf<Triple<String, String, String>>()
        val dirs = mutableListOf<String>()

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)
                    val mime = cursor.getString(mimeIndex)

                    if (mime == android.provider.DocumentsContract.Document.MIME_TYPE_DIR) {
                        dirs.add(id)
                    } else {
                        files.add(Triple(id, name, mime))
                    }
                }
            }
        } catch (e: Exception) {
            return
        }

        files.sortByWith(compareBy(NaturalOrderComparator) { it.second ?: "" })
        dirs.sortWith(compareBy(NaturalOrderComparator) { it ?: "" })

        for (file in files) {
            val name = file.second
            val mime = file.third
            if ((name != null && isAudioFile(name)) || mime.startsWith("audio/") || mime == "application/ogg") {
                val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, file.first)
                results.add(Pair(docUri, null))
            }
        }

        for (dirId in dirs) {
            val dirUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, dirId)
            if (!excludedUris.contains(dirUri.toString())) {
                scanDocumentFileFast(context, treeUri, dirId, excludedUris, results)
            }
        }
    }
"""

content = content.replace('    private fun scanDocumentFileRecursively(', helper + '\n    private fun scanDocumentFileRecursively(')

content = content.replace(
    'scanDocumentFileRecursively(root, excludedUris, foundFiles)',
    '''val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                        scanDocumentFileFast(context, uri, docId, excludedUris, foundFiles)'''
)

with open('app/src/main/java/com/example/util/DirectoryScanner.kt', 'w') as f:
    f.write(content)

