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
        private val KEY_MAC = stringPreferencesKey("device_mac")
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_FAVORITE_SOURCES = stringPreferencesKey("favorite_sources")
        private val KEY_FAVORITE_PRESETS = stringPreferencesKey("favorite_presets")
        private val KEY_AUTO_RECONNECT = stringPreferencesKey("auto_reconnect")
        private val KEY_QUICK_BUTTON_ORDER = stringPreferencesKey("quick_button_order")
        private val KEY_LAST_RADIO_NAME = stringPreferencesKey("last_radio_name")
        private val KEY_LAST_RADIO_URL = stringPreferencesKey("last_radio_url")
        private val KEY_LAST_RADIO_IMAGE = stringPreferencesKey("last_radio_image")
        private val KEY_LAST_RADIO_PLAY = stringPreferencesKey("last_radio_play")
    }

    // Viimati kuulatud raadiojaam ("Raadio" virtuaalne sisend jätkab sellega)
    val lastRadio: Flow<LastRadio?> = context.dataStore.data.map { prefs ->
        val url = prefs[KEY_LAST_RADIO_URL].orEmpty()
        val play = prefs[KEY_LAST_RADIO_PLAY].orEmpty()
        if (url.isBlank() && play.isBlank()) null
        else LastRadio(
            name = prefs[KEY_LAST_RADIO_NAME].orEmpty(),
            url = url,
            imageUrl = prefs[KEY_LAST_RADIO_IMAGE].orEmpty(),
            playUrl = play
        )
    }

    suspend fun setLastRadio(radio: LastRadio) {
        context.dataStore.edit {
            it[KEY_LAST_RADIO_NAME] = radio.name
            it[KEY_LAST_RADIO_URL] = radio.url
            it[KEY_LAST_RADIO_IMAGE] = radio.imageUrl
            it[KEY_LAST_RADIO_PLAY] = radio.playUrl
        }
    }

    val savedIp: Flow<String> = context.dataStore.data.map { it[KEY_IP] ?: "" }
    val savedName: Flow<String> = context.dataStore.data.map { it[KEY_NAME] ?: "" }
    val savedMac: Flow<String> = context.dataStore.data.map { it[KEY_MAC] ?: "" }
    
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

    val quickButtonOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUICK_BUTTON_ORDER]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.take(32)
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

    suspend fun saveDevice(ip: String, name: String = "", mac: String = "") {
        context.dataStore.edit {
            it[KEY_IP] = ip
            it[KEY_NAME] = name
            it[KEY_MAC] = mac
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

    suspend fun setQuickButtonOrder(order: List<String>) {
        context.dataStore.edit {
            it[KEY_QUICK_BUTTON_ORDER] = order
                .map { it.trim() }
                .filter { token -> token.isNotBlank() }
                .take(32)
                .joinToString(",")
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(KEY_IP)
            it.remove(KEY_NAME)
            it.remove(KEY_MAC)
        }
    }
}


