package com.vibeterminal.ui.terminal

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeterminal.core.translator.TranslationEngine
import com.vibeterminal.core.translator.TranslatedOutput
import com.vibeterminal.core.shell.TermuxShellSession
import com.vibeterminal.core.termux.TermuxCommandExecutor
import com.vibeterminal.ui.ime.ComposingTextState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for Terminal screen
 * Manages terminal state, translation, and IME
 * Uses persistent Termux shell session for full terminal functionality
 */
class TerminalViewModel : ViewModel() {

    // Terminal output state (now synced with shell session)
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

    // Termux shell session (replaces ShellExecutor)
    private var shellSession: TermuxShellSession? = null

    // Termux command executor for CLI tools
    private var termuxExecutor: TermuxCommandExecutor? = null

    // File picker state
    private val _filePickerTrigger = MutableStateFlow(FilePickerType.NONE)
    val filePickerTrigger: StateFlow<FilePickerType> = _filePickerTrigger.asStateFlow()

    private val _cameraUri = MutableStateFlow<Uri?>(null)
    val cameraUri: StateFlow<Uri?> = _cameraUri.asStateFlow()

    // Virtual keyboard visibility
    private val _isKeyboardVisible = MutableStateFlow(true)
    val isKeyboardVisible: StateFlow<Boolean> = _isKeyboardVisible.asStateFlow()

    // Search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchVisible = MutableStateFlow(false)
    val searchVisible: StateFlow<Boolean> = _searchVisible.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _currentSearchIndex = MutableStateFlow(0)
    val currentSearchIndex: StateFlow<Int> = _currentSearchIndex.asStateFlow()

    private var appContext: Context? = null
    private var geminiApiKey: String? = null
    private var useAiTranslation: Boolean = false

    fun initialize(patternsDir: File, context: Context? = null, apiKey: String? = null, useAi: Boolean = false) {
        // Clean up existing session if any
        shellSession?.stop()

        translationEngine = TranslationEngine(
            patternsDir = patternsDir,
            llmApiKey = apiKey
        )
        // Use Application Context to avoid memory leaks
        appContext = context?.applicationContext
        geminiApiKey = apiKey
        useAiTranslation = useAi

        // Initialize Termux shell session
        context?.applicationContext?.let { ctx ->
            _terminalOutput.value = ""  // Clear previous output

            shellSession = TermuxShellSession(ctx, viewModelScope)
            termuxExecutor = TermuxCommandExecutor(ctx, viewModelScope)

            // Start collecting output BEFORE starting the shell
            // This ensures we capture all initialization messages
            viewModelScope.launch {
                shellSession?.output?.collect { newOutput ->
                    _terminalOutput.value = newOutput
                }
            }

            // Now start the shell session
            if (!shellSession!!.start()) {
                _terminalOutput.value += "⚠️  Failed to start shell session\n"
            }
        }
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
     * Execute a command in the persistent shell session
     */
    fun executeCommand(command: String) {
        viewModelScope.launch {
            _currentCommand.value = command

            // Check if this is a Termux CLI command (claude, codex, etc.)
            if (termuxExecutor?.shouldUseTermux(command) == true) {
                // Try to execute in Termux first
                val success = termuxExecutor?.executeInTermux(command)
                if (success == true) {
                    // Command sent to Termux
                    // Note: We may not get output back directly
                    _terminalOutput.value += "📱 Executing in Termux: $command\n"
                    _terminalOutput.value += "⚠️  Output will appear in Termux app\n"
                    _terminalOutput.value += "💡 Tip: Check Termux notifications for results\n\n"
                } else {
                    // Fallback to shell session
                    _terminalOutput.value += "⚠️  Termux not available, trying in system shell...\n"
                    shellSession?.executeCommand(command)
                }
            } else {
                // Regular shell command
                shellSession?.executeCommand(command)
            }

            // Translate output if enabled
            // Note: Translation now happens after output appears
            if (_isTranslationVisible.value) {
                // Delay to let output accumulate
                kotlinx.coroutines.delay(500)
                val recentOutput = _terminalOutput.value.takeLast(1000)
                translateOutput(command, recentOutput)
            }
        }
    }

    /**
     * Get current shell type info
     */
    fun getShellInfo(): String {
        return shellSession?.getShellInfo() ?: "Shell not initialized"
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

    // Search functionality
    fun toggleSearch() {
        _searchVisible.value = !_searchVisible.value
        if (!_searchVisible.value) {
            clearSearch()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch(query)
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _currentSearchIndex.value = 0
            return
        }

        val output = _terminalOutput.value
        val results = mutableListOf<SearchResult>()
        var startIndex = 0

        while (startIndex < output.length) {
            val index = output.indexOf(query, startIndex, ignoreCase = true)
            if (index == -1) break

            results.add(
                SearchResult(
                    startIndex = index,
                    endIndex = index + query.length,
                    text = query
                )
            )
            startIndex = index + 1
        }

        _searchResults.value = results
        _currentSearchIndex.value = if (results.isNotEmpty()) 0 else -1
    }

    fun nextSearchResult() {
        if (_searchResults.value.isEmpty()) return
        _currentSearchIndex.value = (_currentSearchIndex.value + 1) % _searchResults.value.size
    }

    fun previousSearchResult() {
        if (_searchResults.value.isEmpty()) return
        val newIndex = _currentSearchIndex.value - 1
        _currentSearchIndex.value = if (newIndex < 0) _searchResults.value.size - 1 else newIndex
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _currentSearchIndex.value = 0
    }

    /**
     * Clean up shell session when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        shellSession?.stop()
    }
}

/**
 * Search result data class
 */
data class SearchResult(
    val startIndex: Int,
    val endIndex: Int,
    val text: String
)
