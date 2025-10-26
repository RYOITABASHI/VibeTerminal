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
            _output.value += "🔧 Starting shell session...\n"
            _output.value += "📍 Trying Termux bash: $TERMUX_BASH\n"

            // Don't check File.exists() - it returns false on Android 10+ due to sandboxing
            // Instead, try to execute directly and fallback if it fails

            // Start bash without interactive mode to avoid tty errors
            val processBuilder = ProcessBuilder(TERMUX_BASH)

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

            _output.value += "🚀 Launching Termux bash...\n"

            // Start the shell process
            shellProcess = processBuilder.start()
            _isRunning.value = true

            _output.value += "✅ Shell process started\n"
            _output.value += "📂 Working directory: ${workDir.absolutePath}\n"
            _output.value += "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"

            // Start reading output in background
            startOutputReader()

            // Set a simple prompt (no extra newlines to avoid duplicate prompts)
            shellProcess?.outputStream?.apply {
                write("PS1='$ '\n".toByteArray())
                flush()
            }

            true
        } catch (e: Exception) {
            _output.value += "⚠️  Termux bash not available: ${e.message}\n"
            _output.value += "💡 Termux is optional. Falling back to system shell...\n\n"

            // Try fallback to system shell
            startFallbackShell()
        }
    }

    /**
     * Fallback to system shell if Termux is not available
     */
    private fun startFallbackShell(): Boolean {
        return try {
            _output.value += "🔄 Starting system shell...\n"

            // Start sh without interactive mode to avoid tty errors
            val processBuilder = ProcessBuilder("/system/bin/sh")

            processBuilder.environment().apply {
                put("HOME", context.filesDir.absolutePath)
                put("TMPDIR", context.cacheDir.absolutePath)
                // Add Node.js, Termux and app bin directories to PATH
                val nodeBin = "${context.filesDir.absolutePath}/nodejs/bin"
                val termuxBin = "$TERMUX_PREFIX/bin:$TERMUX_PREFIX/bin/applets"
                val appBin = "${context.filesDir.absolutePath}/bin"
                put("PATH", "$nodeBin:$termuxBin:$appBin:${get("PATH") ?: ""}")
                // Add Node.js module paths
                val nodeModules = "${context.filesDir.absolutePath}/nodejs/lib/node_modules"
                put("NODE_PATH", "$nodeModules:$TERMUX_PREFIX/lib/node_modules")
            }

            processBuilder.directory(context.filesDir)
            processBuilder.redirectErrorStream(true)

            _output.value += "🚀 Launching system shell...\n"

            shellProcess = processBuilder.start()
            _isRunning.value = true
            _currentDirectory.value = context.filesDir.absolutePath

            _output.value += "✅ System shell started\n"
            _output.value += "📂 Working directory: ${context.filesDir.absolutePath}\n"
            _output.value += "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"

            startOutputReader()

            // Set a simple prompt (no extra newlines to avoid duplicate prompts)
            shellProcess?.outputStream?.apply {
                write("PS1='$ '\n".toByteArray())
                flush()
            }

            true
        } catch (e: Exception) {
            _output.value += "❌ Critical error: Failed to start shell\n"
            _output.value += "📋 Error details: ${e.message}\n"
            _output.value += "💡 Please ensure your Android version is supported (API 26+)\n\n"
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
                val inputStream = shellProcess?.inputStream ?: return@launch
                val buffer = ByteArray(1024)

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    val output = String(buffer, 0, bytesRead)
                    _output.value += output
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
            _output.value += "$ $command\n"  // Echo command to terminal
            shellProcess?.outputStream?.apply {
                write("$command\n".toByteArray())
                flush()
            }
        } catch (e: Exception) {
            _output.value += "❌ Command execution error: ${e.message}\n"
            _output.value += "📋 Stack trace: ${e.stackTraceToString()}\n"
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
