package com.duoji.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "duoji_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        private val KEY_USE_REAL_AI = booleanPreferencesKey("use_real_ai")
        private val KEY_USE_WARM_REMINDER = booleanPreferencesKey("use_warm_reminder")
        private val KEY_MONTHLY_BUDGET = floatPreferencesKey("monthly_budget")
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_BASE_URL] ?: "https://api.deepseek.com"
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val modelName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL_NAME] ?: "deepseek-v4-flash"
    }

    val useRealAI: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_REAL_AI] ?: false
    }

    val useWarmReminder: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_WARM_REMINDER] ?: true
    }

    val monthlyBudget: Flow<Double> = context.dataStore.data.map { prefs ->
        val value = prefs[KEY_MONTHLY_BUDGET] ?: -1f
        if (value < 0) -1.0 else value.toDouble()
    }

    // TODO: 正式发布前应改为 Android Keystore 或服务端代理，不要明文保存 API Key
    suspend fun saveSettings(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        useRealAI: Boolean,
        useWarmReminder: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_BASE_URL] = apiBaseUrl
            prefs[KEY_API_KEY] = apiKey
            prefs[KEY_MODEL_NAME] = modelName
            prefs[KEY_USE_REAL_AI] = useRealAI
            prefs[KEY_USE_WARM_REMINDER] = useWarmReminder
        }
    }

    suspend fun saveAISettings(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        useRealAI: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_BASE_URL] = apiBaseUrl
            prefs[KEY_API_KEY] = apiKey
            prefs[KEY_MODEL_NAME] = modelName
            prefs[KEY_USE_REAL_AI] = useRealAI
        }
    }

    suspend fun saveWarmReminder(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_WARM_REMINDER] = enabled
        }
    }

    suspend fun saveMonthlyBudget(budget: Double) {
        context.dataStore.edit { prefs ->
            if (budget <= 0) {
                prefs.remove(KEY_MONTHLY_BUDGET)
            } else {
                prefs[KEY_MONTHLY_BUDGET] = budget.toFloat()
            }
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_API_KEY)
        }
    }
}
