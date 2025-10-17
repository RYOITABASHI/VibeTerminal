package com.vibeterminal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _fontSize = MutableStateFlow(14)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _translationEnabled = MutableStateFlow(true)
    val translationEnabled: StateFlow<Boolean> = _translationEnabled.asStateFlow()

    private val _llmApiKey = MutableStateFlow("")
    val llmApiKey: StateFlow<String> = _llmApiKey.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            _isDarkTheme.value = enabled
            // TODO: Save to DataStore
        }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch {
            _fontSize.value = size
            // TODO: Save to DataStore
        }
    }

    fun setTranslationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _translationEnabled.value = enabled
            // TODO: Save to DataStore
        }
    }

    fun setLlmApiKey(key: String) {
        viewModelScope.launch {
            _llmApiKey.value = key
            // TODO: Save to encrypted DataStore
        }
    }
}
