package com.duoji.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "duoji_settings")

class AppSettings(private val context: Context) {

    companion object {
        private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        private val KEY_USE_WARM_REMINDER = booleanPreferencesKey("use_warm_reminder")
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_BASE_URL] ?: ""
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val modelName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL_NAME] ?: ""
    }

    val useWarmReminder: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_WARM_REMINDER] ?: true
    }

    // TODO: 后续正式发布，应使用 EncryptedSharedPreferences 或更安全的密钥管理方式存储 API Key
    suspend fun saveSettings(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
        useWarmReminder: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_BASE_URL] = apiBaseUrl
            if (apiKey.isNotBlank()) {
                prefs[KEY_API_KEY] = apiKey
            }
            prefs[KEY_MODEL_NAME] = modelName
            prefs[KEY_USE_WARM_REMINDER] = useWarmReminder
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_API_KEY)
        }
    }
}
