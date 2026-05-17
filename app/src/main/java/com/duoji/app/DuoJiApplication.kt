package com.duoji.app

import android.app.Application
import com.duoji.app.data.local.DuojiDatabase
import com.duoji.app.data.repository.TransactionRepository
import com.duoji.app.data.settings.SettingsDataStore
import com.duoji.app.data.settings.SettingsRepository

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
    }

    companion object {
        lateinit var instance: DuoJiApplication
            private set
    }
}
