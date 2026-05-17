package com.duoji.app.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppSettings(
    val apiBaseUrl: String = "https://api.deepseek.com",
    val apiKey: String = "",
    val modelName: String = "deepseek-v4-flash",
    val useRealAI: Boolean = false,
    val useWarmReminder: Boolean = true
)

class SettingsRepository(private val settingsDataStore: SettingsDataStore) {

    val settingsFlow: Flow<AppSettings> = combine(
        settingsDataStore.apiBaseUrl,
        settingsDataStore.apiKey,
        settingsDataStore.modelName,
        settingsDataStore.useRealAI,
        settingsDataStore.useWarmReminder
    ) { apiBaseUrl, apiKey, modelName, useRealAI, useWarmReminder ->
        AppSettings(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            useRealAI = useRealAI,
            useWarmReminder = useWarmReminder
        )
    }

    suspend fun saveAISettings(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        useRealAI: Boolean
    ) {
        settingsDataStore.saveAISettings(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            useRealAI = useRealAI
        )
    }

    suspend fun saveWarmReminder(enabled: Boolean) {
        settingsDataStore.saveWarmReminder(enabled)
    }

    suspend fun clearApiKey() {
        settingsDataStore.clearApiKey()
    }
}
