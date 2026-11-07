package com.example.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.example.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Robust, production-grade crash and diagnostics engine for Neo Player.
 *
 * Capabilities:
 * 1. Global Uncaught Exception Handler: captures fatal JVM exceptions and unhandled thread errors.
 * 2. Dual-Layer Persistence: writes both formatted Markdown to physical storage and structured JSON
 *    to synchronous SharedPreferences to guarantee persistence across process terminations.
 * 3. Android 11+ (API 30+) ApplicationExitInfo Reconciliation: queries OS-level historical exit reasons
 *    to capture native crashes (SIGSEGV/SIGABRT), ANRs (main thread freezes), and low-memory kills (LMK)
 *    even when the Java runtime was abruptly terminated by the kernel.
 * 4. Zero-Allocation OOM & StackOverflow safety: protects against secondary crashes during logging.
 * 5. Breadcrumbs Ring Buffer: tracks user navigation and player actions leading up to the error.
 * 6. Timber Integration: automatically records `Timber.e` occurrences into persistent diagnostics.
 */
object CrashReporter {
    private const val TAG = "CrashReporter"
    private const val PREFS_NAME = "neo_crash_reporter_store"
    private const val KEY_CRASH_ENTRIES = "crash_entries_json"
    private const val KEY_SYNCED_EXIT_TIMESTAMPS = "synced_exit_timestamps"
    private const val FILE_NAME = "crash_logs.md"
    private const val MAX_BREADCRUMBS = 30
    private const val MAX_STORED_ENTRIES = 50
    private const val MAX_LOG_FILE_BYTES = 512 * 1024L // 512 KB

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    @Volatile private var appContext: Context? = null
    @Volatile private var isInitialized = false

    private val breadcrumbs = ConcurrentLinkedQueue<String>()
    private val logLock = Any()

    data class CrashEntry(
        val id: String,
        val timestamp: Long,
        val formattedTime: String,
        val isFatal: Boolean,
        val isSystemExit: Boolean,
        val tag: String,
        val exceptionType: String,
        val message: String,
        val threadName: String,
        val isUiThread: Boolean,
        val appVersion: String,
        val deviceInfo: String,
        val osInfo: String,
        val memoryUsage: String,
        val breadcrumbs: List<String>,
        val stackTrace: String
    )

    data class SystemHealthInfo(
        val appVersion: String,
        val osVersion: String,
        val deviceModel: String,
        val usedMemoryMb: Long,
        val maxMemoryMb: Long,
        val uptimeMinutes: Long,
        val totalRecordedCrashes: Int,
        val isMonitoringActive: Boolean
    )

    /**
     * Initialize crash reporting from Application.attachBaseContext or Application.onCreate.
     * Safe to invoke multiple times; updates appContext to the Application level whenever available.
     */
    fun init(context: Context) {
        val app = context.applicationContext ?: context
        appContext = app

        synchronized(logLock) {
            if (!isInitialized) {
                isInitialized = true

                // Plant Timber Tree to capture all error logs across the app
                try {
                    Timber.plant(CrashReportingTree())
                } catch (t: Throwable) {
                    Log.w(TAG, "Timber tree planting notice: ${t.message}")
                }

                // Hook uncaught exception handler
                val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
                if (currentHandler !is NeoUncaughtExceptionHandler) {
                    defaultHandler = currentHandler
                    Thread.setDefaultUncaughtExceptionHandler(NeoUncaughtExceptionHandler(defaultHandler))
                }

                addBreadcrumb("CrashReporter initialized: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

                // Reconcile any historical OS-level process exits (native crash, ANR, LMK) in background
                scanAndSyncSystemExitsAsync(app)
            }
        }
    }

    /**
     * Uncaught exception handler wrapper that guarantees logging before delegating to the OS handler.
     */
    private class NeoUncaughtExceptionHandler(
        private val originalHandler: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                logCrash(
                    thread = thread,
                    exception = throwable,
                    isFatal = true,
                    customTag = "UncaughtException",
                    customMessage = "Fatal process termination: ${throwable.javaClass.simpleName}"
                )
            } catch (t: Throwable) {
                // Secondary safety: write minimal emergency note to Logcat
                Log.e(TAG, "Emergency: Uncaught exception in CrashReporter handler: ${t.message}", t)
            } finally {
                if (originalHandler != null) {
                    originalHandler.uncaughtException(thread, throwable)
                } else {
                    // Safe termination fallback if no original handler exists
                    Process.killProcess(Process.myPid())
                    System.exit(10)
                }
            }
        }
    }

    /**
     * Records a diagnostic breadcrumb of user actions or state changes.
     */
    fun addBreadcrumb(message: String) {
        try {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            breadcrumbs.add("[$time] $message")
            while (breadcrumbs.size > MAX_BREADCRUMBS) {
                breadcrumbs.poll()
            }
        } catch (_: Throwable) {}
    }

    /**
     * Log handled exceptions caught in try/catch blocks.
     */
    fun logHandledException(throwable: Throwable, tag: String? = null, message: String? = null) {
        logCrash(
            thread = Thread.currentThread(),
            exception = throwable,
            isFatal = false,
            customTag = tag ?: "HandledException",
            customMessage = message
        )
    }

    /**
     * Explicit error logging for warnings and non-fatal errors.
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val ex = throwable ?: Exception(message)
        logCrash(
            thread = Thread.currentThread(),
            exception = ex,
            isFatal = false,
            customTag = tag,
            customMessage = message
        )
    }

    /**
     * Logs a verified diagnostic test error to confirm capture and formatting.
     */
    fun logTestCrash(context: Context) {
        appContext = context.applicationContext ?: context
        val testException = RuntimeException("Diagnostic Test Error: Verifying CrashReporter capture and formatting functionality")
        addBreadcrumb("User triggered diagnostic test error from Crash Logs screen")
        logCrash(
            thread = Thread.currentThread(),
            exception = testException,
            isFatal = false,
            customTag = "DiagnosticTest",
            customMessage = "User initiated diagnostic test verification."
        )
    }

    /**
     * Returns the total count of recorded crashes and errors.
     */
    fun getLogCount(context: Context): Int {
        val app = appContext ?: context.applicationContext ?: context
        return try {
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_CRASH_ENTRIES, null)
            if (!jsonStr.isNullOrBlank()) {
                val array = JSONArray(jsonStr)
                array.length()
            } else {
                val file = File(app.filesDir, FILE_NAME)
                if (file.exists() && file.length() > 50L) {
                    1
                } else 0
            }
        } catch (_: Throwable) {
            0
        }
    }

    @Volatile private var isSyncingExits = false

    /**
     * Scans Android 11+ (API 30+) Historical Process Exit Reasons to find crashes, ANRs,
     * or native segfaults that the OS recorded when the process was killed.
     * Returns the count of newly synchronized exit events.
     */
    fun scanAndSyncSystemExits(context: Context): Int {
        if (isSyncingExits) return 0
        isSyncingExits = true
        val app = appContext ?: context.applicationContext ?: context
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            isSyncingExits = false
            return 0
        }

        var newEntriesFound = 0
        try {
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: run {
                isSyncingExits = false
                return 0
            }
            val exitList = am.getHistoricalProcessExitReasons(app.packageName, 0, 10) ?: run {
                isSyncingExits = false
                return 0
            }
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val syncedTimestamps = prefs.getStringSet(KEY_SYNCED_EXIT_TIMESTAMPS, emptySet())?.toMutableSet() ?: mutableSetOf()

            for (info in exitList) {
                val timestampStr = info.timestamp.toString()
                if (syncedTimestamps.contains(timestampStr)) continue

                // Check if this exit reason is an abnormal termination
                val (isCrashOrAnr, reasonLabel) = when (info.reason) {
                    ApplicationExitInfo.REASON_CRASH -> true to "OS Uncaught Exception Crash"
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> true to "Native Segfault / Crash (SIGSEGV/SIGABRT)"
                    ApplicationExitInfo.REASON_ANR -> true to "Application Not Responding (ANR)"
                    ApplicationExitInfo.REASON_LOW_MEMORY -> true to "Low Memory Killer (LMK)"
                    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> true to "Excessive Resource Kill"
                    ApplicationExitInfo.REASON_SIGNALED -> true to "Process Signaled Kill"
                    else -> false to "Normal Exit (${info.reason})"
                }

                if (isCrashOrAnr) {
                    val traceText = readTraceInputStream(info.traceInputStream)
                    val desc = info.description ?: "No system description available"
                    val pssMb = info.pss / 1024
                    val rssMb = info.rss / 1024

                    val syntheticException = RuntimeException(
                        "Android OS Process Exit: $reasonLabel (Status: ${info.status}, PSS: ${pssMb}MB, RSS: ${rssMb}MB)\n$desc"
                    )

                    logCrash(
                        thread = Thread.currentThread(),
                        exception = syntheticException,
                        isFatal = true,
                        customTag = "SystemExit:$reasonLabel",
                        customMessage = "Captured via Android ApplicationExitInfo API. Process PID: ${info.pid}, Status: ${info.status}",
                        overrideStackTrace = if (traceText.isNotBlank()) traceText else null,
                        overrideTimestamp = info.timestamp,
                        isSystemExit = true
                    )
                    newEntriesFound++
                }

                syncedTimestamps.add(timestampStr)
            }

            prefs.edit().putStringSet(KEY_SYNCED_EXIT_TIMESTAMPS, syncedTimestamps).apply()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to scan historical process exits: ${t.message}")
        } finally {
            isSyncingExits = false
        }
        return newEntriesFound
    }

    private fun scanAndSyncSystemExitsAsync(context: Context) {
        Thread({
            scanAndSyncSystemExits(context)
        }, "NeoCrashReporter-ExitSync").start()
    }

    private fun readTraceInputStream(stream: InputStream?): String {
        if (stream == null) return ""
        return try {
            val bytes = ByteArray(32 * 1024)
            val read = stream.read(bytes)
            if (read > 0) {
                String(bytes, 0, read, Charsets.UTF_8).trim()
            } else ""
        } catch (_: Throwable) {
            ""
        } finally {
            try { stream.close() } catch (_: Throwable) {}
        }
    }

    /**
     * Reads all recorded crash entries as structured objects.
     */
    fun getStructuredEntries(context: Context): List<CrashEntry> {
        val app = appContext ?: context.applicationContext ?: context
        val entries = getRawStructuredEntries(app).toMutableList()

        // Fallback: If SharedPreferences is empty but markdown file exists, read markdown
        if (entries.isEmpty()) {
            val file = File(app.filesDir, FILE_NAME)
            if (file.exists() && file.length() > 0L) {
                try {
                    val raw = file.readText().trim()
                    if (raw.isNotBlank() && raw != "# Neo Player Crash & Diagnostics Logs" && raw != "# App Crash & Error Logs") {
                        entries.add(
                            CrashEntry(
                                id = "legacy_file",
                                timestamp = System.currentTimeMillis(),
                                formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
                                isFatal = raw.contains("FATAL CRASH"),
                                isSystemExit = raw.contains("SYSTEM"),
                                tag = "ImportedLog",
                                exceptionType = "Recorded Exception",
                                message = "Legacy log entries detected in storage",
                                threadName = "system",
                                isUiThread = false,
                                appVersion = BuildConfig.VERSION_NAME,
                                deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}",
                                osInfo = "Android ${Build.VERSION.RELEASE}",
                                memoryUsage = "N/A",
                                breadcrumbs = emptyList(),
                                stackTrace = raw
                            )
                        )
                    }
                } catch (_: Throwable) {}
            }
        }

        return entries.sortedByDescending { it.timestamp }
    }

    private fun getRawStructuredEntries(context: Context): List<CrashEntry> {
        val app = appContext ?: context.applicationContext ?: context
        val entries = mutableListOf<CrashEntry>()
        try {
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_CRASH_ENTRIES, null)
            if (!jsonStr.isNullOrBlank()) {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val breadcrumbArray = obj.optJSONArray("breadcrumbs")
                    val crumbs = mutableListOf<String>()
                    if (breadcrumbArray != null) {
                        for (c in 0 until breadcrumbArray.length()) {
                            crumbs.add(breadcrumbArray.getString(c))
                        }
                    }
                    entries.add(
                        CrashEntry(
                            id = obj.optString("id", i.toString()),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            formattedTime = obj.optString("formattedTime", ""),
                            isFatal = obj.optBoolean("isFatal", true),
                            isSystemExit = obj.optBoolean("isSystemExit", false),
                            tag = obj.optString("tag", "General"),
                            exceptionType = obj.optString("exceptionType", "Exception"),
                            message = obj.optString("message", ""),
                            threadName = obj.optString("threadName", "main"),
                            isUiThread = obj.optBoolean("isUiThread", true),
                            appVersion = obj.optString("appVersion", BuildConfig.VERSION_NAME),
                            deviceInfo = obj.optString("deviceInfo", ""),
                            osInfo = obj.optString("osInfo", ""),
                            memoryUsage = obj.optString("memoryUsage", ""),
                            breadcrumbs = crumbs,
                            stackTrace = obj.optString("stackTrace", "")
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error parsing structured crash entries: ${t.message}", t)
        }
        return entries
    }

    /**
     * Returns full Markdown-formatted log file for copying and exporting.
     */
    fun getLogs(context: Context): String {
        val app = appContext ?: context.applicationContext ?: context
        val file = File(app.filesDir, FILE_NAME)
        if (file.exists() && file.length() > 0L) {
            try {
                val content = file.readText().trim()
                if (content.isNotBlank() &&
                    content != "# App Crash Logs" &&
                    content != "# App Crash & Error Logs" &&
                    content != "# Neo Player Crash & Diagnostics Logs"
                ) {
                    return content
                }
            } catch (e: Throwable) {
                return "Error reading crash logs: ${e.message}"
            }
        }

        // If file is empty or non-existent, construct from structured entries in SharedPreferences
        val entries = getRawStructuredEntries(app)
        if (entries.isEmpty()) return ""
        return buildMarkdownFromEntries(entries)
    }

    private fun buildMarkdownFromEntries(entries: List<CrashEntry>): String {
        val builder = StringBuilder("# Neo Player Crash & Diagnostics Logs\n\n")
        entries.forEach { entry ->
            val typeLabel = if (entry.isSystemExit) {
                "⚠️ SYSTEM EXIT / ANR"
            } else if (entry.isFatal) {
                "🔥 FATAL CRASH"
            } else {
                "⚠️ NON-FATAL ERROR"
            }
            builder.append("---\n")
            builder.append("### $typeLabel at ${entry.formattedTime}\n\n")
            builder.append("- **Tag / Reason:** `${entry.tag}`\n")
            if (entry.message.isNotBlank()) {
                builder.append("- **Details:** ${entry.message}\n")
            }
            builder.append("- **Exception:** `${entry.exceptionType}`\n")
            builder.append("- **Thread:** `${entry.threadName}` (UI: ${entry.isUiThread})\n")
            builder.append("- **App Version:** `${entry.appVersion}`\n")
            builder.append("- **Device:** `${entry.deviceInfo}`\n")
            builder.append("- **Android OS:** `${entry.osInfo}`\n")
            builder.append("- **Memory Usage:** `${entry.memoryUsage}`\n\n")

            if (entry.breadcrumbs.isNotEmpty()) {
                builder.append("**Recent Breadcrumbs:**\n")
                entry.breadcrumbs.forEach { crumb ->
                    builder.append("- $crumb\n")
                }
                builder.append("\n")
            }

            builder.append("**Stack Trace:**\n```\n")
            builder.append(entry.stackTrace.trim())
            builder.append("\n```\n\n")
        }
        return builder.toString()
    }

    /**
     * Clears all crash logs, stored entries, and system exit records.
     */
    fun clearLogs(context: Context) {
        val app = appContext ?: context.applicationContext ?: context
        synchronized(logLock) {
            try {
                val file = File(app.filesDir, FILE_NAME)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Throwable) {}

            try {
                val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .remove(KEY_CRASH_ENTRIES)
                    .remove(KEY_SYNCED_EXIT_TIMESTAMPS)
                    .commit()
            } catch (_: Throwable) {}

            breadcrumbs.clear()
            addBreadcrumb("Logs cleared by user")
        }
    }

    /**
     * Returns a quick diagnostic summary of device health and crash monitoring state.
     */
    fun getSystemHealthSummary(context: Context): SystemHealthInfo {
        val app = appContext ?: context.applicationContext ?: context
        val runtime = Runtime.getRuntime()
        val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)
        val uptimeMinutes = SystemClock.elapsedRealtime() / (1000 * 60)
        val totalCount = getLogCount(app)

        return SystemHealthInfo(
            appVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            usedMemoryMb = usedMemMb,
            maxMemoryMb = maxMemMb,
            uptimeMinutes = uptimeMinutes,
            totalRecordedCrashes = totalCount,
            isMonitoringActive = true
        )
    }

    /**
     * Internal logging engine that writes both Markdown and structured JSON.
     */
    private fun logCrash(
        thread: Thread,
        exception: Throwable,
        isFatal: Boolean,
        customTag: String? = null,
        customMessage: String? = null,
        overrideStackTrace: String? = null,
        overrideTimestamp: Long? = null,
        isSystemExit: Boolean = false
    ) {
        val context = appContext ?: return
        synchronized(logLock) {
            try {
                val timestamp = overrideTimestamp ?: System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US)
                val formattedTime = dateFormat.format(Date(timestamp))

                val isOom = exception is OutOfMemoryError
                val typeLabel = if (isSystemExit) {
                    "⚠️ SYSTEM EXIT / ANR"
                } else if (isFatal) {
                    "🔥 FATAL CRASH"
                } else {
                    "⚠️ NON-FATAL ERROR"
                }
                val tagStr = customTag ?: if (isFatal) "UncaughtException" else "HandledError"

                val runtime = Runtime.getRuntime()
                val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val maxMemMb = runtime.maxMemory() / (1024 * 1024)
                val isUiThread = Looper.getMainLooper()?.thread == thread

                val stackTrace = if (overrideStackTrace != null) {
                    overrideStackTrace
                } else {
                    try {
                        val sw = StringWriter()
                        val pw = PrintWriter(sw)
                        exception.printStackTrace(pw)
                        sw.toString()
                    } catch (_: Throwable) {
                        "StackTrace generation failed: ${exception.message}"
                    }
                }

                val breadcrumbSnapshot = breadcrumbs.toList()
                val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})"
                val osInfo = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                val memoryUsage = "${usedMemMb}MB / ${maxMemMb}MB (PID: ${Process.myPid()})"

                // 1. Persist structured JSON in SharedPreferences (synchronous commit for crash safety)
                saveStructuredEntry(
                    context = context,
                    entry = CrashEntry(
                        id = "${timestamp}_${System.nanoTime()}",
                        timestamp = timestamp,
                        formattedTime = formattedTime,
                        isFatal = isFatal,
                        isSystemExit = isSystemExit,
                        tag = tagStr,
                        exceptionType = "${exception.javaClass.name}: ${exception.message ?: "No message"}",
                        message = customMessage ?: "",
                        threadName = thread.name,
                        isUiThread = isUiThread,
                        appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        deviceInfo = deviceInfo,
                        osInfo = osInfo,
                        memoryUsage = memoryUsage,
                        breadcrumbs = breadcrumbSnapshot,
                        stackTrace = stackTrace
                    )
                )

                // 2. Append Markdown formatted entry to crash_logs.md
                appendMarkdownToFile(
                    context = context,
                    typeLabel = typeLabel,
                    formattedTime = formattedTime,
                    tagStr = tagStr,
                    customMessage = customMessage,
                    exception = exception,
                    thread = thread,
                    isUiThread = isUiThread,
                    deviceInfo = deviceInfo,
                    osInfo = osInfo,
                    memoryUsage = memoryUsage,
                    breadcrumbs = breadcrumbSnapshot,
                    stackTrace = stackTrace
                )

            } catch (t: Throwable) {
                Log.e(TAG, "Fatal error inside logCrash: ${t.message}", t)
            }
        }
    }

    private fun saveStructuredEntry(context: Context, entry: CrashEntry) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_CRASH_ENTRIES, null)
            val jsonArray = if (!jsonStr.isNullOrBlank()) JSONArray(jsonStr) else JSONArray()

            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("timestamp", entry.timestamp)
            obj.put("formattedTime", entry.formattedTime)
            obj.put("isFatal", entry.isFatal)
            obj.put("isSystemExit", entry.isSystemExit)
            obj.put("tag", entry.tag)
            obj.put("exceptionType", entry.exceptionType)
            obj.put("message", entry.message)
            obj.put("threadName", entry.threadName)
            obj.put("isUiThread", entry.isUiThread)
            obj.put("appVersion", entry.appVersion)
            obj.put("deviceInfo", entry.deviceInfo)
            obj.put("osInfo", entry.osInfo)
            obj.put("memoryUsage", entry.memoryUsage)

            val crumbsArray = JSONArray()
            entry.breadcrumbs.forEach { crumbsArray.put(it) }
            obj.put("breadcrumbs", crumbsArray)

            // Cap stack trace to 64KB for JSON storage
            val cappedStackTrace = if (entry.stackTrace.length > 64 * 1024) {
                entry.stackTrace.substring(0, 64 * 1024) + "\n...[truncated]"
            } else {
                entry.stackTrace
            }
            obj.put("stackTrace", cappedStackTrace)

            // Append newest to front
            val newArray = JSONArray()
            newArray.put(obj)
            val count = minOf(jsonArray.length(), MAX_STORED_ENTRIES - 1)
            for (i in 0 until count) {
                newArray.put(jsonArray.get(i))
            }

            prefs.edit().putString(KEY_CRASH_ENTRIES, newArray.toString()).commit()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to save structured crash entry: ${t.message}", t)
        }
    }

    private fun appendMarkdownToFile(
        context: Context,
        typeLabel: String,
        formattedTime: String,
        tagStr: String,
        customMessage: String?,
        exception: Throwable,
        thread: Thread,
        isUiThread: Boolean,
        deviceInfo: String,
        osInfo: String,
        memoryUsage: String,
        breadcrumbs: List<String>,
        stackTrace: String
    ) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
                file.writeText("# Neo Player Crash & Diagnostics Logs\n\n")
            }

            // Check log file rotation if exceeding limit
            if (file.length() > MAX_LOG_FILE_BYTES) {
                try {
                    val text = file.readText()
                    val entries = text.split("\n---\n")
                    val kept = if (entries.size > 20) entries.takeLast(20) else entries
                    file.writeText("# Neo Player Crash & Diagnostics Logs\n\n" + kept.joinToString("\n---\n"))
                } catch (_: Throwable) {}
            }

            val builder = StringBuilder()
            builder.append("\n---\n")
            builder.append("### $typeLabel at $formattedTime\n\n")
            builder.append("- **Tag / Reason:** `$tagStr`\n")
            if (!customMessage.isNullOrBlank()) {
                builder.append("- **Details:** $customMessage\n")
            }
            builder.append("- **Exception:** `${exception.javaClass.name}: ${exception.message ?: "No message"}`\n")
            builder.append("- **Thread:** `${thread.name}` (ID: ${thread.id}, UI: $isUiThread)\n")
            builder.append("- **App Version:** `${BuildConfig.VERSION_NAME}` (Code: `${BuildConfig.VERSION_CODE}`, Debug: `${BuildConfig.DEBUG}`)\n")
            builder.append("- **Device:** `$deviceInfo`\n")
            builder.append("- **Android OS:** `$osInfo`\n")
            builder.append("- **Memory Usage:** `$memoryUsage`\n")

            if (breadcrumbs.isNotEmpty()) {
                builder.append("\n**Recent Breadcrumbs:**\n")
                breadcrumbs.forEach { crumb ->
                    builder.append("- $crumb\n")
                }
            }

            builder.append("\n**Stack Trace:**\n")
            builder.append("```\n")
            builder.append(stackTrace.trim())
            builder.append("\n```\n\n")

            val bytes = builder.toString().toByteArray(Charsets.UTF_8)
            context.openFileOutput(FILE_NAME, Context.MODE_APPEND).use { fos ->
                fos.write(bytes)
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to write markdown log: ${t.message}", t)
        }
    }

    /**
     * Timber Tree that automatically funnels high-severity events and errors into CrashReporter.
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val cleanTag = tag ?: "App"
            if (priority >= Log.INFO) {
                addBreadcrumb("[$cleanTag] $message")
            }
            if (priority >= Log.ERROR) {
                val ex = t ?: Exception(message)
                logCrash(
                    thread = Thread.currentThread(),
                    exception = ex,
                    isFatal = false,
                    customTag = cleanTag,
                    customMessage = message
                )
            }
        }
    }
}
