package com.nadremote.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nad_prefs")

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    ESTONIAN("et", "Eesti"),
    GERMAN("de", "Deutsch"),
    FINNISH("fi", "Suomi"),
    SWEDISH("sv", "Svenska"),
    NORWEGIAN("no", "Norsk"),
    DANISH("da", "Dansk"),
    FRENCH("fr", "FranÃ§ais"),
    ITALIAN("it", "Italiano"),
    SPANISH("es", "EspaÃ±ol")
}

class Prefs(private val context: Context) {

    companion object {
        private val KEY_IP = stringPreferencesKey("device_ip")
        private val KEY_NAME = stringPreferencesKey("device_name")
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_FAVORITE_SOURCES = stringPreferencesKey("favorite_sources")
        private val KEY_FAVORITE_PRESETS = stringPreferencesKey("favorite_presets")
        private val KEY_AUTO_RECONNECT = stringPreferencesKey("auto_reconnect")
    }

    val savedIp: Flow<String> = context.dataStore.data.map { it[KEY_IP] ?: "" }
    val savedName: Flow<String> = context.dataStore.data.map { it[KEY_NAME] ?: "" }
    
    // Lemmik sisendid (max 4) - salvestatud kui "1,3,5,7"
    val favoriteSources: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITE_SOURCES]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.take(4)
            ?: emptyList()
    }
    
    // Lemmik presetid (max 4)
    val favoritePresets: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITE_PRESETS]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.take(4)
            ?: emptyList()
    }
    
    val autoReconnect: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_RECONNECT]?.toBoolean() ?: true // Vaikimisi sees
    }
    
    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        try {
            AppTheme.valueOf(prefs[KEY_THEME] ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }
    
    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        try {
            AppLanguage.valueOf(prefs[KEY_LANGUAGE] ?: AppLanguage.ENGLISH.name)
        } catch (e: Exception) {
            AppLanguage.ENGLISH
        }
    }

    suspend fun saveDevice(ip: String, name: String = "") {
        context.dataStore.edit {
            it[KEY_IP] = ip
            it[KEY_NAME] = name
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit {
            it[KEY_THEME] = theme.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit {
            it[KEY_LANGUAGE] = language.name
        }
    }

    suspend fun setFavoriteSources(sourceIds: List<Int>) {
        context.dataStore.edit {
            it[KEY_FAVORITE_SOURCES] = sourceIds.take(4).joinToString(",")
        }
    }

    suspend fun setFavoritePresets(presetIds: List<Int>) {
        context.dataStore.edit {
            it[KEY_FAVORITE_PRESETS] = presetIds.take(4).joinToString(",")
        }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_AUTO_RECONNECT] = enabled.toString()
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(KEY_IP)
            it.remove(KEY_NAME)
        }
    }
}


