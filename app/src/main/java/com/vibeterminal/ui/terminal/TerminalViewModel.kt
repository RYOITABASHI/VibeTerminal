package com.vibeterminal.ui.terminal

import android.content.Context
import android.net.Uri
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

    // File picker state
    private val _filePickerTrigger = MutableStateFlow(FilePickerType.NONE)
    val filePickerTrigger: StateFlow<FilePickerType> = _filePickerTrigger.asStateFlow()

    private val _cameraUri = MutableStateFlow<Uri?>(null)
    val cameraUri: StateFlow<Uri?> = _cameraUri.asStateFlow()

    // Virtual keyboard visibility
    private val _isKeyboardVisible = MutableStateFlow(true)
    val isKeyboardVisible: StateFlow<Boolean> = _isKeyboardVisible.asStateFlow()

    private var appContext: Context? = null
    private var geminiApiKey: String? = null
    private var useAiTranslation: Boolean = false

    fun initialize(patternsDir: File, context: Context? = null, apiKey: String? = null, useAi: Boolean = false) {
        translationEngine = TranslationEngine(
            patternsDir = patternsDir,
            llmApiKey = apiKey
        )
        // Use Application Context to avoid memory leaks
        appContext = context?.applicationContext
        geminiApiKey = apiKey
        useAiTranslation = useAi
    }

    fun updateSettings(apiKey: String?, useAi: Boolean) {
        geminiApiKey = apiKey
        useAiTranslation = useAi

        // Reinitialize translation engine with new settings
        if (::translationEngine.isInitialized && appContext != null) {
            val patternsDir = File(appContext!!.filesDir, "translations")
            translationEngine = TranslationEngine(
                patternsDir = patternsDir,
                llmApiKey = apiKey
            )
        }
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
            } catch (e: SecurityException) {
                _terminalOutput.value += "❌ Permission denied: ${e.message}\n"
            } catch (e: java.io.IOException) {
                _terminalOutput.value += "❌ I/O Error: ${e.message}\n"
            } catch (e: Exception) {
                _terminalOutput.value += "❌ Error: ${e.javaClass.simpleName} - ${e.message}\n"
                e.printStackTrace()
            }
        }
    }

    /**
     * Execute shell command
     */
    private suspend fun executeShellCommand(command: String): String {
        return try {
            val args = command.split(" ").toTypedArray()
            val process = ProcessBuilder(*args)
                .redirectErrorStream(true)
                .start()

            // Wait for process with timeout
            val output = StringBuilder()
            val reader = process.inputStream.bufferedReader()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                output.append("\n⚠️  Exit code: $exitCode")
            }

            output.toString()
        } catch (e: SecurityException) {
            throw e
        } catch (e: java.io.IOException) {
            throw e
        } catch (e: Exception) {
            "⚠️  Command execution failed: ${e.message}"
        }
    }

    /**
     * Translate terminal output
     */
    private fun translateOutput(command: String, output: String) {
        viewModelScope.launch {
            try {
                if (!::translationEngine.isInitialized) {
                    return@launch
                }

                val result = translationEngine.translate(
                    command = command,
                    output = output,
                    useLLM = useAiTranslation && !geminiApiKey.isNullOrBlank()
                )
                _currentTranslation.value = result
            } catch (e: Exception) {
                // Log error but don't crash
                _terminalOutput.value += "\n⚠️  Translation failed: ${e.message}\n"
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
        _isKeyboardVisible.value = !_isKeyboardVisible.value
    }

    /**
     * Send special key to terminal
     */
    fun sendSpecialKey(key: String) {
        viewModelScope.launch {
            // Add key to output for visual feedback
            _terminalOutput.value += key
        }
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

    /**
     * Request file picker
     */
    fun requestFilePicker() {
        _filePickerTrigger.value = FilePickerType.FILE
        // Reset immediately
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            _filePickerTrigger.value = FilePickerType.NONE
        }
    }

    /**
     * Request image picker
     */
    fun requestImagePicker() {
        _filePickerTrigger.value = FilePickerType.IMAGE
        // Reset immediately
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            _filePickerTrigger.value = FilePickerType.NONE
        }
    }

    /**
     * Request camera
     */
    fun requestCamera() {
        appContext?.let { context ->
            // Create a temporary file for the camera photo
            val photoFile = File.createTempFile(
                "JPEG_${System.currentTimeMillis()}_",
                ".jpg",
                context.cacheDir
            )

            // Create content URI using FileProvider
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )

            _cameraUri.value = photoUri
            _filePickerTrigger.value = FilePickerType.CAMERA

            // Reset immediately
            viewModelScope.launch {
                kotlinx.coroutines.delay(100)
                _filePickerTrigger.value = FilePickerType.NONE
            }
        }
    }

    /**
     * Handle file selection
     */
    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // Get file path from URI
                val filePath = getPathFromUri(uri)

                // Insert file path into terminal
                val currentOutput = _terminalOutput.value
                _terminalOutput.value = "$currentOutput\n📎 File: $filePath"

                // Optionally, add to command input
                // You might want to expose inputText as a state to append to it
            } catch (e: Exception) {
                _terminalOutput.value += "\n❌ Failed to process file: ${e.message}"
            }
        }
    }

    /**
     * Handle camera photo taken
     */
    fun onCameraPhotoTaken() {
        _cameraUri.value?.let { uri ->
            onFileSelected(uri)
        }
    }

    /**
     * Get file path from URI
     */
    private fun getPathFromUri(uri: Uri): String {
        // For content:// URIs, return the URI string
        // In a real implementation, you might want to copy the file to app storage
        return when (uri.scheme) {
            "file" -> uri.path ?: uri.toString()
            "content" -> {
                // For content URIs, you might want to copy to cache dir
                // For now, just return the URI
                uri.toString()
            }
            else -> uri.toString()
        }
    }
}
