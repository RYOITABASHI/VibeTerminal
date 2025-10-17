package com.vibeterminal.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeterminal.core.translator.TranslationEngine
import com.vibeterminal.core.translator.TranslatedOutput
import com.vibeterminal.ui.ime.ComposingTextState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for Terminal screen
 * Manages terminal state, translation, and IME
 */
class TerminalViewModel : ViewModel() {

    // Terminal output state
    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    // Translation state
    private val _translation = MutableStateFlow<TranslatedOutput?>(null)
    val translation: StateFlow<TranslatedOutput?> = _translation.asStateFlow()

    private val _showTranslation = MutableStateFlow(true)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    // IME composing text state
    private val _composingText = MutableStateFlow(ComposingTextState())
    val composingText: StateFlow<ComposingTextState> = _composingText.asStateFlow()

    // Current command being executed
    private val _currentCommand = MutableStateFlow("")
    val currentCommand: StateFlow<String> = _currentCommand.asStateFlow()

    // Translation engine
    private lateinit var translationEngine: TranslationEngine

    fun initialize(patternsDir: File) {
        translationEngine = TranslationEngine(
            patternsDir = patternsDir,
            llmApiKey = null // TODO: Load from settings
        )
    }

    /**
     * Called when terminal receives new output
     */
    fun onTerminalOutput(output: String, command: String = "") {
        _terminalOutput.value = output
        _currentCommand.value = command

        // Translate output
        if (_showTranslation.value) {
            translateOutput(command, output)
        }
    }

    /**
     * Translate terminal output
     */
    private fun translateOutput(command: String, output: String) {
        viewModelScope.launch {
            try {
                val result = translationEngine.translate(
                    command = command,
                    output = output,
                    useLLM = false // TODO: Check user's Pro status
                )
                _translation.value = result
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }

    /**
     * Toggle translation visibility
     */
    fun toggleTranslation() {
        _showTranslation.value = !_showTranslation.value
    }

    /**
     * Called when IME composing text updates
     */
    fun onComposingTextUpdate(text: CharSequence) {
        _composingText.value = ComposingTextState(
            text = text.toString(),
            isActive = text.isNotEmpty(),
            cursorPosition = text.length
        )
    }

    /**
     * Called when IME commits text
     */
    fun onTextCommit(text: CharSequence) {
        // Clear composing state
        _composingText.value = ComposingTextState()

        // Send text to terminal
        // TODO: Integrate with actual terminal emulator
        sendToTerminal(text.toString())
    }

    /**
     * Send text to terminal
     */
    private fun sendToTerminal(text: String) {
        // TODO: Implement actual terminal input
        println("Terminal input: $text")
    }

    /**
     * Clear translation cache
     */
    fun clearTranslationCache() {
        // TODO: Implement
    }
}
