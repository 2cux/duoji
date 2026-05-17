package com.duoji.app.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class SettingsData(
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    val modelName: String = "",
    val useWarmReminder: Boolean = true
)

class SettingsRepository(private val appSettings: AppSettings) {

    val settings: Flow<SettingsData> = combineSettings()

    private fun combineSettings() = combine(
        appSettings.apiBaseUrl,
        appSettings.apiKey,
        appSettings.modelName,
        appSettings.useWarmReminder
    ) { apiBaseUrl, apiKey, modelName, useWarmReminder ->
        SettingsData(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            modelName = modelName,
            useWarmReminder = useWarmReminder
        )
    }

    suspend fun saveSettings(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        useWarmReminder: Boolean
    ) {
        appSettings.saveSettings(apiBaseUrl, apiKey, modelName, useWarmReminder)
    }

    suspend fun clearApiKey() {
        appSettings.clearApiKey()
    }
}
