#!/bin/bash
sed -i 's/import kotlinx.coroutines.GlobalScope/import kotlinx.coroutines.CoroutineScope\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.SupervisorJob\nimport kotlinx.coroutines.cancel/g' app/src/main/java/com/example/PlaybackService.kt
sed -i 's/private var mediaSession: MediaLibrarySession? = null/private var mediaSession: MediaLibrarySession? = null\n    private val serviceJob = SupervisorJob()\n    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)/g' app/src/main/java/com/example/PlaybackService.kt
sed -i 's/GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO)/serviceScope.launch(Dispatchers.IO)/g' app/src/main/java/com/example/PlaybackService.kt
sed -i 's/super.onDestroy()/serviceJob.cancel()\n        super.onDestroy()/g' app/src/main/java/com/example/PlaybackService.kt
