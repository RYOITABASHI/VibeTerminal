package com.vibeterminal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for app settings persistence
 */
class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val FONT_SIZE = intPreferencesKey("font_size")
        val TRANSLATION_ENABLED = booleanPreferencesKey("translation_enabled")
        val LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val USE_AI_TRANSLATION = booleanPreferencesKey("use_ai_translation")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val TERMINAL_FONT_SIZE = intPreferencesKey("terminal_font_size")
        val TERMINAL_COLOR_SCHEME = stringPreferencesKey("terminal_color_scheme")
        val KEYBOARD_HEIGHT = intPreferencesKey("keyboard_height")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
    }

    // Dark theme
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_THEME] ?: true
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = enabled
        }
    }

    // Font size
    val fontSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FONT_SIZE] ?: 14
    }

    suspend fun setFontSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = size
        }
    }

    // Translation enabled
    val translationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TRANSLATION_ENABLED] ?: true
    }

    suspend fun setTranslationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRANSLATION_ENABLED] = enabled
        }
    }

    // LLM API Key
    val llmApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LLM_API_KEY] ?: ""
    }

    suspend fun setLlmApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_API_KEY] = key
        }
    }

    // Use AI translation
    val useAiTranslation: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USE_AI_TRANSLATION] ?: false
    }

    suspend fun setUseAiTranslation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_AI_TRANSLATION] = enabled
        }
    }

    // OpenAI API Key
    val openAiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.OPENAI_API_KEY] ?: ""
    }

    suspend fun setOpenAiApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENAI_API_KEY] = key
        }
    }

    // OpenAI Model
    val openAiModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.OPENAI_MODEL] ?: "gpt-4o-mini"
    }

    suspend fun setOpenAiModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENAI_MODEL] = model
        }
    }

    // Terminal font size
    val terminalFontSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TERMINAL_FONT_SIZE] ?: 14
    }

    suspend fun setTerminalFontSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TERMINAL_FONT_SIZE] = size
        }
    }

    // Terminal color scheme
    val terminalColorScheme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TERMINAL_COLOR_SCHEME] ?: "dark"
    }

    suspend fun setTerminalColorScheme(scheme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TERMINAL_COLOR_SCHEME] = scheme
        }
    }

    // Keyboard height
    val keyboardHeight: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEYBOARD_HEIGHT] ?: 90
    }

    suspend fun setKeyboardHeight(height: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEYBOARD_HEIGHT] = height
        }
    }

    // Show line numbers
    val showLineNumbers: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_LINE_NUMBERS] ?: false
    }

    suspend fun setShowLineNumbers(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_LINE_NUMBERS] = show
        }
    }
}
