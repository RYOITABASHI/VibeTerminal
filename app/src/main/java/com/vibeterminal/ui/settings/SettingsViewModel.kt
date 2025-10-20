package com.vibeterminal.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeterminal.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(context: Context) : ViewModel() {
    private val repository = SettingsRepository(context)

    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val fontSize: StateFlow<Int> = repository.fontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 14)

    val translationEnabled: StateFlow<Boolean> = repository.translationEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val llmApiKey: StateFlow<String> = repository.llmApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val useAiTranslation: StateFlow<Boolean> = repository.useAiTranslation
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val openAiApiKey: StateFlow<String> = repository.openAiApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val openAiModel: StateFlow<String> = repository.openAiModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, "gpt-4o-mini")

    val terminalFontSize: StateFlow<Int> = repository.terminalFontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 14)

    val terminalColorScheme: StateFlow<String> = repository.terminalColorScheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    val keyboardHeight: StateFlow<Int> = repository.keyboardHeight
        .stateIn(viewModelScope, SharingStarted.Eagerly, 90)

    val showLineNumbers: StateFlow<Boolean> = repository.showLineNumbers
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(enabled)
        }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch {
            repository.setFontSize(size)
        }
    }

    fun setTranslationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTranslationEnabled(enabled)
        }
    }

    fun setLlmApiKey(key: String) {
        viewModelScope.launch {
            repository.setLlmApiKey(key)
        }
    }

    fun setUseAiTranslation(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUseAiTranslation(enabled)
        }
    }

    fun setOpenAiApiKey(key: String) {
        viewModelScope.launch {
            repository.setOpenAiApiKey(key)
        }
    }

    fun setOpenAiModel(model: String) {
        viewModelScope.launch {
            repository.setOpenAiModel(model)
        }
    }

    fun setTerminalFontSize(size: Int) {
        viewModelScope.launch {
            repository.setTerminalFontSize(size)
        }
    }

    fun setTerminalColorScheme(scheme: String) {
        viewModelScope.launch {
            repository.setTerminalColorScheme(scheme)
        }
    }

    fun setKeyboardHeight(height: Int) {
        viewModelScope.launch {
            repository.setKeyboardHeight(height)
        }
    }

    fun setShowLineNumbers(show: Boolean) {
        viewModelScope.launch {
            repository.setShowLineNumbers(show)
        }
    }
}
