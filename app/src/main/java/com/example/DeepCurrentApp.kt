package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.DefaultProfiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeepCurrentApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        setupCrashHandler()
        seedInitialProfiles()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val logText = "=== CRASH ON $time ===\nThread: ${thread.name}\n$stackTrace\n\n"

                val crashFile = File(filesDir, "crash_log.txt")
                crashFile.appendText(logText)
                Log.e("DeepCurrentCrash", logText)
            } catch (e: Exception) {
                Log.e("DeepCurrentCrash", "Failed to save crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun seedInitialProfiles() {
        applicationScope.launch {
            val existing = database.tunnelDao().getAllProfiles().first()
            if (existing.isEmpty()) {
                DefaultProfiles.createDefaults().forEach { profile ->
                    database.tunnelDao().insertProfile(profile)
                }
            }
        }
    }

    companion object {
        lateinit var instance: DeepCurrentApp
            private set

        fun getCrashLog(context: Context): String {
            val file = File(context.filesDir, "crash_log.txt")
            return if (file.exists()) {
                file.readText()
            } else {
                "No crash logs recorded. System healthy."
            }
        }

        fun clearCrashLog(context: Context) {
            val file = File(context.filesDir, "crash_log.txt")
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
