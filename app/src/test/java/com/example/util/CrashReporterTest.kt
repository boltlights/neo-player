package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashReporterTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        CrashReporter.init(context)
        CrashReporter.clearLogs(context)
    }

    @Test
    fun testInitializationAndEmptyLogs() {
        val count = CrashReporter.getLogCount(context)
        assertEquals(0, count)
        val logs = CrashReporter.getLogs(context)
        assertTrue(logs.isBlank())
    }

    @Test
    fun testLogTestCrashRecordsEntry() {
        CrashReporter.logTestCrash(context)

        val count = CrashReporter.getLogCount(context)
        assertTrue("Log count should be at least 1 after test crash", count >= 1)

        val entries = CrashReporter.getStructuredEntries(context)
        assertFalse("Entries should not be empty", entries.isEmpty())

        val firstEntry = entries.first()
        assertEquals("DiagnosticTest", firstEntry.tag)
        assertTrue(firstEntry.exceptionType.contains("Diagnostic Test Error"))
        assertTrue(firstEntry.stackTrace.contains("CrashReporter.logTestCrash"))

        val rawMarkdown = CrashReporter.getLogs(context)
        assertTrue(rawMarkdown.contains("Neo Player Crash & Diagnostics Logs"))
        assertTrue(rawMarkdown.contains("Diagnostic Test Error"))
    }

    @Test
    fun testLogHandledException() {
        val ex = IllegalStateException("Mock test exception for validation")
        CrashReporter.logHandledException(ex, tag = "TestTag", message = "Handled error occurred")

        val entries = CrashReporter.getStructuredEntries(context)
        assertTrue(entries.any { it.tag == "TestTag" && it.message == "Handled error occurred" })
    }

    @Test
    fun testClearLogsRemovesAllEntries() {
        CrashReporter.logTestCrash(context)
        assertTrue(CrashReporter.getLogCount(context) > 0)

        CrashReporter.clearLogs(context)
        assertEquals(0, CrashReporter.getLogCount(context))
        assertTrue(CrashReporter.getLogs(context).isBlank())
    }

    @Test
    fun testSystemHealthSummary() {
        val summary = CrashReporter.getSystemHealthSummary(context)
        assertNotNull(summary)
        assertTrue(summary.appVersion.isNotBlank())
        assertTrue(summary.osVersion.isNotBlank())
        assertTrue(summary.isMonitoringActive)
    }
}
