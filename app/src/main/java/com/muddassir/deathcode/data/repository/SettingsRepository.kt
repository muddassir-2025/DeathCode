package com.muddassir.deathcode.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muddassir.deathcode.domain.model.ContentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App-wide theme selection. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

/**
 * All user-tunable preferences.
 *
 * Everything here is local; the app never asks the backend to remember settings.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Active keyboard language, e.g. `cpp`. Drives language-specific snippets. */
    val keyboardLanguage: String = "cpp",
    val showSuggestions: Boolean = true,
    val showSymbolRow: Boolean = true,
    val keyHeightDp: Int = 52,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val searchOfficial: Boolean = true,
    val searchPrivate: Boolean = true,
    val searchCommunity: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    val communitySyncEnabled: Boolean = true,
    /**
     * Base URL of the Death Code API (Render). Empty means "no backend configured" and
     * the app simply stays fully offline.
     */
    val backendBaseUrl: String = "",
    val lastSyncMessage: String = "Up to date",
) {
    /** Sources the local search and the keyboard suggestions should consider. */
    val searchSources: Set<ContentSource>
        get() = buildSet {
            if (searchOfficial) add(ContentSource.OFFICIAL)
            if (searchPrivate) add(ContentSource.PRIVATE)
            if (searchCommunity) add(ContentSource.COMMUNITY)
        }.ifEmpty { setOf(ContentSource.OFFICIAL, ContentSource.PRIVATE) }
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "deathcode_settings",
)

/** DataStore backed preferences for both the app UI and the IME process. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val KEYBOARD_LANGUAGE = stringPreferencesKey("keyboard_language")
        val SHOW_SUGGESTIONS = booleanPreferencesKey("show_suggestions")
        val SHOW_SYMBOLS = booleanPreferencesKey("show_symbols")
        val KEY_HEIGHT = intPreferencesKey("key_height_dp")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SOUND = booleanPreferencesKey("sound")
        val SEARCH_OFFICIAL = booleanPreferencesKey("search_official")
        val SEARCH_PRIVATE = booleanPreferencesKey("search_private")
        val SEARCH_COMMUNITY = booleanPreferencesKey("search_community")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val COMMUNITY_SYNC = booleanPreferencesKey("community_sync")
        val BACKEND_URL = stringPreferencesKey("backend_base_url")
        val LAST_SYNC_MESSAGE = stringPreferencesKey("last_sync_message")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            keyboardLanguage = prefs[Keys.KEYBOARD_LANGUAGE] ?: "cpp",
            showSuggestions = prefs[Keys.SHOW_SUGGESTIONS] ?: true,
            showSymbolRow = prefs[Keys.SHOW_SYMBOLS] ?: true,
            keyHeightDp = prefs[Keys.KEY_HEIGHT] ?: 52,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            soundEnabled = prefs[Keys.SOUND] ?: false,
            searchOfficial = prefs[Keys.SEARCH_OFFICIAL] ?: true,
            searchPrivate = prefs[Keys.SEARCH_PRIVATE] ?: true,
            searchCommunity = prefs[Keys.SEARCH_COMMUNITY] ?: true,
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
            communitySyncEnabled = prefs[Keys.COMMUNITY_SYNC] ?: true,
            backendBaseUrl = prefs[Keys.BACKEND_URL] ?: "",
            lastSyncMessage = prefs[Keys.LAST_SYNC_MESSAGE] ?: "Up to date",
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }

    suspend fun setKeyboardLanguage(language: String) =
        edit { it[Keys.KEYBOARD_LANGUAGE] = language }

    suspend fun setShowSuggestions(enabled: Boolean) = edit { it[Keys.SHOW_SUGGESTIONS] = enabled }

    suspend fun setShowSymbolRow(enabled: Boolean) = edit { it[Keys.SHOW_SYMBOLS] = enabled }

    suspend fun setKeyHeightDp(height: Int) =
        edit { it[Keys.KEY_HEIGHT] = height.coerceIn(40, 76) }

    suspend fun setHaptics(enabled: Boolean) = edit { it[Keys.HAPTICS] = enabled }

    suspend fun setSound(enabled: Boolean) = edit { it[Keys.SOUND] = enabled }

    suspend fun setSearchSource(source: ContentSource, enabled: Boolean) = edit {
        when (source) {
            ContentSource.OFFICIAL -> it[Keys.SEARCH_OFFICIAL] = enabled
            ContentSource.PRIVATE -> it[Keys.SEARCH_PRIVATE] = enabled
            ContentSource.COMMUNITY -> it[Keys.SEARCH_COMMUNITY] = enabled
        }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) = edit { it[Keys.AUTO_CHECK_UPDATES] = enabled }

    suspend fun setCommunitySync(enabled: Boolean) = edit { it[Keys.COMMUNITY_SYNC] = enabled }

    suspend fun setBackendBaseUrl(url: String) = edit { it[Keys.BACKEND_URL] = url.trim() }

    suspend fun setLastSyncMessage(message: String) = edit { it[Keys.LAST_SYNC_MESSAGE] = message }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }
}
