package com.kroslabs.recipemanager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kroslabs.recipemanager.domain.model.Language
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val CLOUD_SYNC_ENABLED_KEY = booleanPreferencesKey("cloud_sync_enabled")
        private val LAST_SYNC_KEY = longPreferencesKey("last_sync")
        private const val API_KEY_KEY = "claude_api_key"
    }

    val language: Flow<Language> = dataStore.data.map { preferences ->
        Language.fromCode(preferences[LANGUAGE_KEY] ?: "en")
    }

    val cloudSyncEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CLOUD_SYNC_ENABLED_KEY] ?: false
    }

    val lastSync: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[LAST_SYNC_KEY]
    }

    suspend fun setLanguage(language: Language) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CLOUD_SYNC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setLastSync(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_SYNC_KEY] = timestamp
        }
    }

    fun getApiKey(): String? = encryptedPrefs.getString(API_KEY_KEY, null)

    fun setApiKey(apiKey: String?) {
        encryptedPrefs.edit().apply {
            if (apiKey != null) {
                putString(API_KEY_KEY, apiKey)
            } else {
                remove(API_KEY_KEY)
            }
            apply()
        }
    }
}
