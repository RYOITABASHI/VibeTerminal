package com.vibeterminal.core.termux

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Execute commands in Termux environment via Termux:API
 * This allows VibeTerminal to use Termux-installed CLI tools
 * like claude, codex, node, etc.
 */
class TermuxCommandExecutor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TERMUX_RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
        private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"

        // Commands that should be executed in Termux
        private val TERMUX_COMMANDS = setOf(
            "claude",
            "codex",
            "gemini",
            "node",
            "npm",
            "npx",
            "python",
            "python3",
            "pip",
            "git"
        )
    }

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output

    /**
     * Check if a command should be executed in Termux
     */
    fun shouldUseTermux(command: String): Boolean {
        val commandName = command.trim().split(" ").firstOrNull() ?: return false
        return commandName in TERMUX_COMMANDS
    }

    /**
     * Execute command in Termux and return output
     */
    fun executeInTermux(command: String, workDir: String? = null): Boolean {
        return try {
            val intent = Intent().apply {
                setClassName("com.termux", TERMUX_SERVICE)
                action = TERMUX_RUN_COMMAND_ACTION

                // Command to execute
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))

                // Working directory
                putExtra(
                    "com.termux.RUN_COMMAND_WORKDIR",
                    workDir ?: "/data/data/com.termux/files/home"
                )

                // Background execution
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)

                // Session action - create new session
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }

            // Start Termux service
            context.startService(intent)

            _output.value += "→ Executing in Termux: $command\n"
            true
        } catch (e: Exception) {
            _output.value += "❌ Failed to execute in Termux: ${e.message}\n"
            _output.value += "⚠️  Make sure Termux app is installed\n"
            false
        }
    }

    /**
     * Check if Termux is installed
     */
    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get list of supported Termux commands
     */
    fun getSupportedCommands(): Set<String> = TERMUX_COMMANDS
}
