package com.vibeterminal.core.shell

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Executes commands in Termux using RUN_COMMAND intent with result callback
 * This replaces direct ProcessBuilder approach which fails due to SELinux restrictions
 */
class TermuxCommandExecutor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

        // Intent extras
        private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
        private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

        // Result bundle extras
        private const val EXTRA_PLUGIN_RESULT_BUNDLE = "com.termux.PLUGIN_RESULT_BUNDLE"
        private const val EXTRA_RESULT_STDOUT = "stdout"
        private const val EXTRA_RESULT_STDERR = "stderr"
        private const val EXTRA_RESULT_EXIT_CODE = "exitCode"
        private const val EXTRA_RESULT_ERR = "err"
        private const val EXTRA_RESULT_ERRMSG = "errmsg"

        private val executionIdCounter = AtomicInteger(1000)

        fun getNextExecutionId(): Int = executionIdCounter.incrementAndGet()
    }

    /**
     * Execute command in Termux with real-time output
     */
    fun executeCommand(command: String) {
        try {
            _isRunning.value = true
            _output.value += "$ $command\n"
            _output.value += "→ Executing in Termux environment...\n"

            // Parse command into path and arguments
            val parts = command.trim().split(Regex("\\s+"))
            val commandName = parts[0]
            val arguments = if (parts.size > 1) parts.drop(1).toTypedArray() else emptyArray()

            // Create command intent
            val intent = Intent().apply {
                setClassName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
                action = ACTION_RUN_COMMAND

                // Set command to execute
                putExtra(EXTRA_COMMAND_PATH, commandName)
                if (arguments.isNotEmpty()) {
                    putExtra(EXTRA_ARGUMENTS, arguments)
                }
                putExtra(EXTRA_WORKDIR, "/data/data/com.termux/files/home")
                putExtra(EXTRA_BACKGROUND, true) // Execute in background to get stdout/stderr
                putExtra(EXTRA_COMMAND_LABEL, "VibeTerminal: $commandName")

                // Create pending intent for result callback
                val executionId = getNextExecutionId()
                val resultIntent = Intent(context, TermuxResultReceiver::class.java).apply {
                    putExtra("execution_id", executionId)
                    putExtra("executor_instance", this@TermuxCommandExecutor.hashCode())
                }

                val pendingIntent = PendingIntent.getService(
                    context,
                    executionId,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PendingIntent.FLAG_MUTABLE
                        } else {
                            0
                        }
                )

                putExtra(EXTRA_PENDING_INTENT, pendingIntent)
            }

            // Start Termux command execution
            context.startService(intent)

            _output.value += "⏳ Command sent to Termux...\n"

            // Register this executor instance to receive results
            TermuxResultReceiver.registerExecutor(this.hashCode(), this)

        } catch (e: Exception) {
            _output.value += "❌ Failed to execute command: ${e.message}\n"
            _output.value += "💡 Make sure Termux app is installed and configured:\n"
            _output.value += "   1. Grant 'Run Command' permission to VibeTerminal\n"
            _output.value += "   2. In Termux, set: echo 'allow-external-apps=true' >> ~/.termux/termux.properties\n"
            _output.value += "   3. Restart Termux app\n\n"
            _isRunning.value = false
        }
    }

    /**
     * Handle command result from Termux
     */
    internal fun handleResult(stdout: String, stderr: String, exitCode: Int, error: Int, errorMsg: String?) {
        _isRunning.value = false

        if (error != 0) {
            _output.value += "❌ Termux execution error ($error): ${errorMsg ?: "Unknown error"}\n\n"
            return
        }

        // Display stdout
        if (stdout.isNotEmpty()) {
            _output.value += stdout
            if (!stdout.endsWith("\n")) {
                _output.value += "\n"
            }
        }

        // Display stderr
        if (stderr.isNotEmpty()) {
            _output.value += stderr
            if (!stderr.endsWith("\n")) {
                _output.value += "\n"
            }
        }

        // Display exit code
        if (exitCode == 0) {
            _output.value += "✅ Command completed successfully (exit code: 0)\n\n"
        } else {
            _output.value += "❌ Command failed (exit code: $exitCode)\n\n"
        }
    }
}

/**
 * Service to receive command execution results from Termux
 */
class TermuxResultReceiver : Service() {

    companion object {
        private val executors = mutableMapOf<Int, TermuxCommandExecutor>()

        fun registerExecutor(id: Int, executor: TermuxCommandExecutor) {
            executors[id] = executor
        }

        private const val EXTRA_PLUGIN_RESULT_BUNDLE = "com.termux.PLUGIN_RESULT_BUNDLE"
        private const val EXTRA_RESULT_STDOUT = "stdout"
        private const val EXTRA_RESULT_STDERR = "stderr"
        private const val EXTRA_RESULT_EXIT_CODE = "exitCode"
        private const val EXTRA_RESULT_ERR = "err"
        private const val EXTRA_RESULT_ERRMSG = "errmsg"
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleResult(it) }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun handleResult(intent: Intent) {
        val executorId = intent.getIntExtra("executor_instance", 0)
        val executor = executors[executorId]

        if (executor == null) {
            return
        }

        val resultBundle = intent.getBundleExtra(EXTRA_PLUGIN_RESULT_BUNDLE) ?: return

        val stdout = resultBundle.getString(EXTRA_RESULT_STDOUT, "")
        val stderr = resultBundle.getString(EXTRA_RESULT_STDERR, "")
        val exitCode = resultBundle.getInt(EXTRA_RESULT_EXIT_CODE, -1)
        val error = resultBundle.getInt(EXTRA_RESULT_ERR, 0)
        val errorMsg = resultBundle.getString(EXTRA_RESULT_ERRMSG)

        executor.handleResult(stdout, stderr, exitCode, error, errorMsg)
    }
}
