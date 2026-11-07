package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class CachedAudioFile(
    val uriString: String,
    val filePath: String?,
    val lastModified: Long
)

data class CachedDirEntry(
    val lastModified: Long,
    val audioFiles: List<CachedAudioFile>,
    val subDirIds: List<String>
)

object FileTreeCache {
    private const val CACHE_FILE_NAME = "file_tree_cache.json"
    private val memoryCache = ConcurrentHashMap<String, CachedDirEntry>()
    @Volatile private var isLoaded = false
    @Volatile private var isDirty = false

    @Synchronized
    fun load(context: Context) {
        if (isLoaded) return
        try {
            val file = File(context.cacheDir, CACHE_FILE_NAME)
            if (file.exists()) {
                val jsonStr = file.readText()
                val json = JSONObject(jsonStr)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val dirObj = json.getJSONObject(key)
                    val lastModified = dirObj.optLong("lastModified", 0L)
                    val filesArr = dirObj.optJSONArray("files") ?: JSONArray()
                    val filesList = mutableListOf<CachedAudioFile>()
                    for (i in 0 until filesArr.length()) {
                        val fObj = filesArr.getJSONObject(i)
                        filesList.add(
                            CachedAudioFile(
                                uriString = fObj.getString("uri"),
                                filePath = if (fObj.has("path") && !fObj.isNull("path")) fObj.getString("path") else null,
                                lastModified = fObj.optLong("lastModified", 0L)
                            )
                        )
                    }
                    val subDirsArr = dirObj.optJSONArray("subDirs") ?: JSONArray()
                    val subDirList = mutableListOf<String>()
                    for (i in 0 until subDirsArr.length()) {
                        subDirList.add(subDirsArr.getString(i))
                    }
                    memoryCache[key] = CachedDirEntry(lastModified, filesList, subDirList)
                }
                Timber.tag("FileTreeCache").d("Loaded %d directory cache entries from disk", memoryCache.size)
            }
        } catch (e: Exception) {
            Timber.tag("FileTreeCache").w(e, "Failed to load file tree cache from disk")
        } finally {
            isLoaded = true
        }
    }

    fun get(dirId: String): CachedDirEntry? {
        return memoryCache[dirId]
    }

    fun put(dirId: String, entry: CachedDirEntry) {
        memoryCache[dirId] = entry
        isDirty = true
    }

    fun clear() {
        memoryCache.clear()
        isDirty = true
    }

    @Synchronized
    fun save(context: Context) {
        if (!isDirty) return
        try {
            val json = JSONObject()
            for ((key, entry) in memoryCache) {
                val dirObj = JSONObject()
                dirObj.put("lastModified", entry.lastModified)
                val filesArr = JSONArray()
                for (f in entry.audioFiles) {
                    val fObj = JSONObject()
                    fObj.put("uri", f.uriString)
                    fObj.put("path", f.filePath)
                    fObj.put("lastModified", f.lastModified)
                    filesArr.put(fObj)
                }
                dirObj.put("files", filesArr)
                val subDirsArr = JSONArray()
                for (subDir in entry.subDirIds) {
                    subDirsArr.put(subDir)
                }
                dirObj.put("subDirs", subDirsArr)
                json.put(key, dirObj)
            }
            val file = File(context.cacheDir, CACHE_FILE_NAME)
            file.writeText(json.toString())
            isDirty = false
            Timber.tag("FileTreeCache").d("Saved %d directory cache entries to disk", memoryCache.size)
        } catch (e: Exception) {
            Timber.tag("FileTreeCache").w(e, "Failed to save file tree cache to disk")
        }
    }
}
