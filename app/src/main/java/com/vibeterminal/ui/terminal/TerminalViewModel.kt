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
    private val _currentTranslation = MutableStateFlow<TranslatedOutput?>(null)
    val currentTranslation: StateFlow<TranslatedOutput?> = _currentTranslation.asStateFlow()

    private val _isTranslationVisible = MutableStateFlow(true)
    val isTranslationVisible: StateFlow<Boolean> = _isTranslationVisible.asStateFlow()

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
     * Execute a command
     */
    fun executeCommand(command: String) {
        viewModelScope.launch {
            _currentCommand.value = command

            // Add command to output
            _terminalOutput.value += "\n$ $command\n"

            try {
                // Execute command (simplified for now)
                val output = executeShellCommand(command)
                _terminalOutput.value += output + "\n"

                // Translate output
                if (_isTranslationVisible.value) {
                    translateOutput(command, output)
                }
            } catch (e: Exception) {
                _terminalOutput.value += "Error: ${e.message}\n"
            }
        }
    }

    /**
     * Execute shell command
     */
    private suspend fun executeShellCommand(command: String): String {
        return try {
            val process = ProcessBuilder(*command.split(" ").toTypedArray())
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Command execution failed: ${e.message}"
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
                _currentTranslation.value = result
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
        _isTranslationVisible.value = !_isTranslationVisible.value
    }

    /**
     * Toggle keyboard
     */
    fun toggleKeyboard() {
        // TODO: Implement keyboard toggle
    }

    /**
     * Dismiss translation overlay
     */
    fun dismissTranslation() {
        _currentTranslation.value = null
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
