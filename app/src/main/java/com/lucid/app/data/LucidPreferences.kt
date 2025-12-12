package com.lucid.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lucid_settings")

/**
 * Persistent preferences for LUCID state
 */
class LucidPreferences(private val context: Context) {

    companion object {
        private val IS_LUCID_MODE = booleanPreferencesKey("is_lucid_mode")
        private val LAST_INTENT = stringPreferencesKey("last_intent")
        private val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    val isLucidMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LUCID_MODE] ?: true // Default to LUCID mode
    }

    val lastIntent: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[LAST_INTENT]
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FIRST_LAUNCH] ?: true
    }

    suspend fun setLucidMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LUCID_MODE] = enabled
        }
    }

    suspend fun setLastIntent(intent: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_INTENT] = intent
        }
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { prefs ->
            prefs[FIRST_LAUNCH] = false
        }
    }
}
