package com.vibeterminal.core.shell

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shell type enumeration
 */
enum class ShellType {
    TERMUX,     // Termux bash (if available and accessible)
    BUSYBOX,    // Bundled BusyBox
    SYSTEM;     // Android system shell (/system/bin/sh)

    fun getDisplayName(): String = when(this) {
        TERMUX -> "Termux"
        BUSYBOX -> "BusyBox"
        SYSTEM -> "System Shell"
    }
}

/**
 * Shell execution result
 */
data class ShellResult(
    val output: String,
    val exitCode: Int,
    val shellType: ShellType,
    val executionTimeMs: Long
)

/**
 * Hybrid shell executor that automatically selects the best available shell
 */
class ShellExecutor(private val context: Context) {

    private var _shellType: ShellType? = null

    /**
     * Get current shell type (auto-detect on first call)
     */
    val shellType: ShellType
        get() {
            if (_shellType == null) {
                _shellType = detectBestShell()
            }
            return _shellType!!
        }

    /**
     * Detect the best available shell
     */
    private fun detectBestShell(): ShellType {
        return when {
            hasTermuxAccess() -> ShellType.TERMUX
            hasBusyBox() -> ShellType.BUSYBOX
            else -> ShellType.SYSTEM
        }
    }

    /**
     * Check if Termux is accessible
     */
    private fun hasTermuxAccess(): Boolean {
        val termuxBash = "/data/data/com.termux/files/usr/bin/bash"
        return try {
            File(termuxBash).exists() && File(termuxBash).canExecute()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if BusyBox is available
     */
    private fun hasBusyBox(): Boolean {
        val busyBoxPath = File(context.filesDir, "bin/busybox")
        return busyBoxPath.exists() && busyBoxPath.canExecute()
    }

    /**
     * Execute a shell command
     */
    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        val result = when (shellType) {
            ShellType.TERMUX -> executeViaTermux(command)
            ShellType.BUSYBOX -> executeViaBusyBox(command)
            ShellType.SYSTEM -> executeViaSystemShell(command)
        }

        val executionTime = System.currentTimeMillis() - startTime

        ShellResult(
            output = result.first,
            exitCode = result.second,
            shellType = shellType,
            executionTimeMs = executionTime
        )
    }

    /**
     * Execute via Termux bash
     */
    private fun executeViaTermux(command: String): Pair<String, Int> {
        val termuxPrefix = "/data/data/com.termux/files/usr"
        val termuxBash = "$termuxPrefix/bin/bash"
        val termuxHome = "/data/data/com.termux/files/home"

        val processBuilder = ProcessBuilder(termuxBash, "-c", command)

        val env = processBuilder.environment()
        env["PATH"] = "$termuxPrefix/bin:$termuxPrefix/bin/applets:${System.getenv("PATH")}"
        env["HOME"] = termuxHome
        env["PREFIX"] = termuxPrefix
        env["TMPDIR"] = "$termuxPrefix/tmp"
        env["SHELL"] = termuxBash

        processBuilder.directory(File(termuxHome))
        processBuilder.redirectErrorStream(true)

        return executeProcess(processBuilder)
    }

    /**
     * Execute via BusyBox
     */
    private fun executeViaBusyBox(command: String): Pair<String, Int> {
        val busyBoxPath = File(context.filesDir, "bin/busybox").absolutePath
        val appHome = context.filesDir.absolutePath

        val processBuilder = ProcessBuilder(busyBoxPath, "sh", "-c", command)

        val env = processBuilder.environment()
        env["PATH"] = "${context.filesDir.absolutePath}/bin:${System.getenv("PATH")}"
        env["HOME"] = appHome
        env["TMPDIR"] = context.cacheDir.absolutePath

        processBuilder.directory(File(appHome))
        processBuilder.redirectErrorStream(true)

        return executeProcess(processBuilder)
    }

    /**
     * Execute via system shell
     */
    private fun executeViaSystemShell(command: String): Pair<String, Int> {
        val appHome = context.filesDir.absolutePath
        val appBin = File(context.filesDir, "bin").absolutePath

        val processBuilder = ProcessBuilder("/system/bin/sh", "-c", command)

        val env = processBuilder.environment()
        env["PATH"] = "$appBin:${System.getenv("PATH")}"
        env["HOME"] = appHome
        env["TMPDIR"] = context.cacheDir.absolutePath

        processBuilder.directory(File(appHome))
        processBuilder.redirectErrorStream(true)

        return executeProcess(processBuilder)
    }

    /**
     * Execute a process and return output and exit code
     */
    private fun executeProcess(processBuilder: ProcessBuilder): Pair<String, Int> {
        val process = processBuilder.start()

        val output = StringBuilder()
        val reader = process.inputStream.bufferedReader()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        val exitCode = process.waitFor()

        return Pair(output.toString(), exitCode)
    }

    /**
     * Force a specific shell type
     */
    fun forceShellType(type: ShellType) {
        _shellType = type
    }

    /**
     * Reset to auto-detection
     */
    fun resetShellType() {
        _shellType = null
    }

    /**
     * Get shell information
     */
    fun getShellInfo(): String {
        return """
            Current Shell: ${shellType.getDisplayName()}
            Termux Available: ${hasTermuxAccess()}
            BusyBox Available: ${hasBusyBox()}
            System Shell: Always available
        """.trimIndent()
    }
}
