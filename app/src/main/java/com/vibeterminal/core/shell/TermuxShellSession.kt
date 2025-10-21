package com.vibeterminal.core.shell

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Persistent Termux bash shell session
 * Provides continuous shell interaction with Termux environment
 */
class TermuxShellSession(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var shellProcess: Process? = null

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentDirectory = MutableStateFlow("")
    val currentDirectory: StateFlow<String> = _currentDirectory.asStateFlow()

    companion object {
        private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"
        private const val TERMUX_HOME = "/data/data/com.termux/files/home"
        private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    }

    /**
     * Start a persistent Termux bash session
     */
    fun start(): Boolean {
        return try {
            val processBuilder = ProcessBuilder(TERMUX_BASH, "-l")

            // Set Termux environment variables
            processBuilder.environment().apply {
                put("HOME", TERMUX_HOME)
                put("PREFIX", TERMUX_PREFIX)
                put("PATH", "$TERMUX_PREFIX/bin:$TERMUX_PREFIX/bin/applets:${get("PATH") ?: ""}")
                put("TMPDIR", "$TERMUX_PREFIX/tmp")
                put("SHELL", TERMUX_BASH)
                put("TERM", "xterm-256color")
                put("LANG", "en_US.UTF-8")
            }

            // Try to use Termux home, fallback to app files dir
            val workDir = if (canAccessDirectory(TERMUX_HOME)) {
                File(TERMUX_HOME)
            } else {
                context.filesDir
            }
            processBuilder.directory(workDir)
            _currentDirectory.value = workDir.absolutePath

            processBuilder.redirectErrorStream(true)

            // Start the shell process
            shellProcess = processBuilder.start()
            _isRunning.value = true

            // Start reading output in background
            startOutputReader()

            // Add welcome message
            _output.value = "VibeTerminal - Termux Shell Session\n"
            _output.value += "Working directory: ${workDir.absolutePath}\n\n"

            true
        } catch (e: Exception) {
            _output.value = "❌ Failed to start Termux shell: ${e.message}\n"
            _output.value += "⚠️  Make sure Termux is installed and up to date.\n\n"

            // Try fallback to system shell
            startFallbackShell()
        }
    }

    /**
     * Fallback to system shell if Termux is not available
     */
    private fun startFallbackShell(): Boolean {
        return try {
            val processBuilder = ProcessBuilder("/system/bin/sh")

            processBuilder.environment().apply {
                put("HOME", context.filesDir.absolutePath)
                put("TMPDIR", context.cacheDir.absolutePath)
                put("PATH", "${context.filesDir.absolutePath}/bin:${get("PATH") ?: ""}")
            }

            processBuilder.directory(context.filesDir)
            processBuilder.redirectErrorStream(true)

            shellProcess = processBuilder.start()
            _isRunning.value = true
            _currentDirectory.value = context.filesDir.absolutePath

            startOutputReader()

            _output.value += "⚠️  Using system shell (limited functionality)\n"
            _output.value += "Working directory: ${context.filesDir.absolutePath}\n\n"

            true
        } catch (e: Exception) {
            _output.value += "❌ Failed to start fallback shell: ${e.message}\n"
            false
        }
    }

    /**
     * Check if directory is accessible
     */
    private fun canAccessDirectory(path: String): Boolean {
        return try {
            val dir = File(path)
            dir.exists() && dir.canRead() && dir.canWrite()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Read shell output continuously in background
     */
    private fun startOutputReader() {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(shellProcess?.inputStream))
                val buffer = StringBuilder()
                var char: Int

                while (reader.read().also { char = it } != -1) {
                    buffer.append(char.toChar())

                    // Flush output periodically for real-time display
                    if (char == '\n'.code || buffer.length > 1000) {
                        val output = buffer.toString()
                        _output.value += output
                        buffer.clear()
                    }
                }

                // Flush any remaining output
                if (buffer.isNotEmpty()) {
                    _output.value += buffer.toString()
                }

            } catch (e: Exception) {
                _output.value += "\n❌ Output reader error: ${e.message}\n"
            } finally {
                _isRunning.value = false
            }
        }
    }

    /**
     * Execute a command in the shell session
     */
    fun executeCommand(command: String) {
        if (!_isRunning.value) {
            _output.value += "❌ Shell session is not running\n"
            return
        }

        try {
            shellProcess?.outputStream?.apply {
                write("$command\n".toByteArray())
                flush()
            }
        } catch (e: Exception) {
            _output.value += "❌ Command execution error: ${e.message}\n"
        }
    }

    /**
     * Get current working directory
     */
    suspend fun updateCurrentDirectory() {
        try {
            // This would require parsing pwd output
            // For now, we keep track based on cd commands
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Stop the shell session gracefully
     */
    fun stop() {
        try {
            // Send exit command
            shellProcess?.outputStream?.apply {
                write("exit\n".toByteArray())
                flush()
            }

            // Wait for graceful shutdown
            shellProcess?.waitFor(1000, java.util.concurrent.TimeUnit.MILLISECONDS)

        } catch (e: Exception) {
            // Ignore
        } finally {
            // Force kill if still running
            shellProcess?.destroyForcibly()
            shellProcess = null
            _isRunning.value = false
        }
    }

    /**
     * Check if Termux is available
     */
    fun isTermuxAvailable(): Boolean {
        return try {
            File(TERMUX_BASH).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get shell information
     */
    fun getShellInfo(): String {
        return if (isTermuxAvailable()) {
            "Termux Bash (${TERMUX_BASH})"
        } else {
            "System Shell (/system/bin/sh)"
        }
    }
}
