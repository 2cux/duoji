package com.duoji.app

import android.app.Application
import android.util.Log
import com.duoji.app.data.local.DuojiDatabase
import com.duoji.app.data.repository.TransactionRepository
import com.duoji.app.data.settings.SettingsDataStore
import com.duoji.app.data.settings.SettingsRepository
import java.io.StringWriter
import java.io.PrintWriter

class AppContainer(context: android.content.Context) {
    private val database = DuojiDatabase.getInstance(context)
    val transactionRepository = TransactionRepository(database.transactionDao())
    val settingsRepository = SettingsRepository(SettingsDataStore(context))
}

class DuoJiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            pw.flush()
            Log.e("GlobalCrashHandler", """
                ============ GLOBAL CRASH ============
                Thread: ${thread.name} (id=${thread.id})
                Exception: ${throwable.javaClass.name}
                Message: ${throwable.message ?: "(no message)"}
                Stacktrace:
                ${sw.toString()}
                ======================================
            """.trimIndent())
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        lateinit var instance: DuoJiApplication
            private set
    }
}
