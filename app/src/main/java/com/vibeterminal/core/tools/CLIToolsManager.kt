package com.vibeterminal.core.tools

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI CLI tool information
 */
data class CLITool(
    val id: String,
    val name: String,
    val description: String,
    val executable: String,
    val downloadUrl: String,
    val version: String,
    val sizeBytes: Long
)

/**
 * CLI Tools download and installation manager
 */
class CLIToolsManager(private val context: Context) {

    private val binDir = File(context.filesDir, "bin")

    // Available AI CLI tools (GitHub releases or direct downloads)
    val availableTools = listOf(
        CLITool(
            id = "claude-cli",
            name = "Claude CLI",
            description = "Anthropic Claude Code CLI tool",
            executable = "claude-cli",
            downloadUrl = "https://github.com/anthropics/claude-code/releases/download/v2.0.24/claude-cli-android-arm64",
            version = "2.0.24",
            sizeBytes = 15_000_000 // ~15MB
        ),
        CLITool(
            id = "openai-cli",
            name = "OpenAI CLI",
            description = "OpenAI ChatGPT/Codex CLI",
            executable = "openai",
            downloadUrl = "https://github.com/openai/openai-cli/releases/latest/download/openai-android-arm64",
            version = "latest",
            sizeBytes = 10_000_000 // ~10MB
        )
    )

    /**
     * Check if a tool is installed
     */
    fun isInstalled(toolId: String): Boolean {
        val tool = availableTools.find { it.id == toolId } ?: return false
        val executable = File(binDir, tool.executable)
        return executable.exists() && executable.canExecute()
    }

    /**
     * Get installed tool version
     */
    suspend fun getVersion(toolId: String): String? = withContext(Dispatchers.IO) {
        if (!isInstalled(toolId)) return@withContext null

        val tool = availableTools.find { it.id == toolId } ?: return@withContext null
        val executable = File(binDir, tool.executable)

        try {
            val process = ProcessBuilder(
                executable.absolutePath,
                "--version"
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // Extract version from output
            output.trim().lines().firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Download and install a CLI tool
     */
    suspend fun install(
        toolId: String,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tool = availableTools.find { it.id == toolId }
                ?: return@withContext Result.failure(Exception("ツールが見つかりません: $toolId"))

            // Create bin directory if not exists
            if (!binDir.exists()) {
                binDir.mkdirs()
            }

            onProgress(0, "準備中: ${tool.name}")

            val executable = File(binDir, tool.executable)

            // Check if already installed
            if (executable.exists()) {
                onProgress(10, "削除中: 既存のバージョン")
                executable.delete()
            }

            onProgress(20, "ダウンロード中: ${tool.name}")

            // Download the tool
            try {
                downloadFile(tool.downloadUrl, executable) { progress ->
                    onProgress(20 + (progress * 0.7).toInt(), "ダウンロード中: $progress%")
                }
            } catch (e: Exception) {
                // If direct download fails, try alternative installation methods
                onProgress(30, "代替インストール: npm経由")
                return@withContext installViaNpm(tool, onProgress)
            }

            onProgress(90, "インストール中: 実行権限を設定")

            // Set executable permission
            executable.setExecutable(true, false)

            onProgress(95, "検証中: インストール")

            // Verify installation
            if (!executable.exists() || !executable.canExecute()) {
                return@withContext Result.failure(Exception("${tool.name}のインストールに失敗しました"))
            }

            onProgress(100, "完了: ${tool.name} インストール成功")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Install via npm (fallback method)
     */
    private suspend fun installViaNpm(
        tool: CLITool,
        onProgress: (Int, String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val npmPackage = when (tool.id) {
                "claude-cli" -> "@anthropic-ai/claude-code"
                "openai-cli" -> "@openai/cli"
                else -> return@withContext Result.failure(Exception("npm package not available"))
            }

            onProgress(40, "インストール中: npm install $npmPackage")

            // Create package directory
            val npmDir = File(context.filesDir, "npm")
            if (!npmDir.exists()) {
                npmDir.mkdirs()
            }

            // Create wrapper script that uses Termux npm if available
            val wrapperScript = File(binDir, tool.executable)
            wrapperScript.writeText("""
                #!/system/bin/sh
                if [ -f /data/data/com.termux/files/usr/bin/node ]; then
                    /data/data/com.termux/files/usr/bin/node /data/data/com.termux/files/usr/lib/node_modules/$npmPackage/cli.js "$@"
                else
                    echo "Error: Node.js not found. Please install Termux and run 'pkg install nodejs'"
                    exit 1
                fi
            """.trimIndent())

            wrapperScript.setExecutable(true, false)

            onProgress(100, "完了: ${tool.name} ラッパースクリプト作成")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uninstall a CLI tool
     */
    fun uninstall(toolId: String): Boolean {
        val tool = availableTools.find { it.id == toolId } ?: return false
        val executable = File(binDir, tool.executable)

        return try {
            executable.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Download file from URL
     */
    private fun downloadFile(
        url: String,
        destination: File,
        onProgress: (Int) -> Unit = {}
    ) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 60000
        connection.readTimeout = 60000
        connection.setRequestProperty("User-Agent", "VibeTerminal/${getAppVersion()}")

        try {
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }

            val fileSize = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(destination)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (fileSize > 0) {
                    val progress = ((totalBytesRead * 100) / fileSize).toInt()
                    onProgress(progress)
                }
            }

            output.flush()
            output.close()
            input.close()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get all installed tools
     */
    fun getInstalledTools(): List<CLITool> {
        return availableTools.filter { isInstalled(it.id) }
    }

    /**
     * Get all available (not installed) tools
     */
    fun getAvailableTools(): List<CLITool> {
        return availableTools.filter { !isInstalled(it.id) }
    }

    /**
     * Check for tool updates
     */
    suspend fun checkForUpdates(toolId: String): String? = withContext(Dispatchers.IO) {
        // TODO: Implement version checking against remote
        null
    }
}
