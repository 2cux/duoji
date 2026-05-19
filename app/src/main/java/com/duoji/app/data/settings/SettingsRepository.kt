package com.duoji.app.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppSettings(
    val apiBaseUrl: String = "https://api.deepseek.com",
    val apiKey: String = "",
    val modelName: String = "deepseek-v4-flash",
    val useRealAI: Boolean = false,
    val useWarmReminder: Boolean = true,
    val monthlyBudget: Double = -1.0 // -1 means not set
)

class SettingsRepository(private val settingsDataStore: SettingsDataStore) {

    val settingsFlow: Flow<AppSettings> = combine(
        settingsDataStore.apiBaseUrl,
        settingsDataStore.apiKey,
        settingsDataStore.modelName,
        settingsDataStore.useRealAI,
        settingsDataStore.useWarmReminder,
        settingsDataStore.monthlyBudget
    ) { args ->
        AppSettings(
            apiBaseUrl = args[0] as String,
            apiKey = args[1] as String,
            modelName = args[2] as String,
            useRealAI = args[3] as Boolean,
            useWarmReminder = args[4] as Boolean,
            monthlyBudget = args[5] as Double
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

    suspend fun saveMonthlyBudget(budget: Double) {
        settingsDataStore.saveMonthlyBudget(budget)
    }
}
