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

    // Available AI CLI tools (installed via npm)
    private val availableTools = listOf(
        CLITool(
            id = "claude-cli",
            name = "Claude Code",
            description = "Anthropic Claude Code - AI coding assistant",
            executable = "claude",
            downloadUrl = "npm:@anthropic-ai/claude-code",
            version = "latest",
            sizeBytes = 50_000_000 // ~50MB
        ),
        CLITool(
            id = "openai-cli",
            name = "OpenAI CLI",
            description = "OpenAI GPT CLI - ChatGPT from terminal",
            executable = "openai",
            downloadUrl = "npm:openai-cli",
            version = "latest",
            sizeBytes = 30_000_000 // ~30MB
        ),
        CLITool(
            id = "gemini-cli",
            name = "Google Gemini",
            description = "Google Gemini AI CLI",
            executable = "gemini",
            downloadUrl = "npm:@google/generative-ai-cli",
            version = "latest",
            sizeBytes = 20_000_000 // ~20MB
        ),
        CLITool(
            id = "codex-cli",
            name = "GitHub Copilot CLI",
            description = "GitHub Copilot command line tool",
            executable = "github-copilot-cli",
            downloadUrl = "npm:@githubnext/github-copilot-cli",
            version = "latest",
            sizeBytes = 25_000_000 // ~25MB
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
     * Install a CLI tool via npm
     */
    suspend fun install(
        toolId: String,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tool = availableTools.find { it.id == toolId }
                ?: return@withContext Result.failure(Exception("ツールが見つかりません: $toolId"))

            // Check if download URL is npm package
            if (tool.downloadUrl.startsWith("npm:")) {
                return@withContext installViaNpm(tool, onProgress)
            }

            // Fallback to direct download (legacy)
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
                return@withContext Result.failure(Exception("ダウンロード失敗: ${e.message}"))
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
     * Install via npm
     */
    private suspend fun installViaNpm(
        tool: CLITool,
        onProgress: (Int, String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Extract npm package name from downloadUrl (format: "npm:package-name")
            val npmPackage = tool.downloadUrl.removePrefix("npm:")

            onProgress(10, "準備中: Node.js確認")

            // Find Node.js executable
            val nodeExecutable = findNodeExecutable()
                ?: return@withContext Result.failure(Exception("Node.js が見つかりません。先にNode.jsをインストールしてください。"))

            val npmExecutable = findNpmExecutable()
                ?: return@withContext Result.failure(Exception("npm が見つかりません。"))

            onProgress(20, "インストール中: $npmPackage")

            // Run npm install -g
            val process = ProcessBuilder(
                npmExecutable.absolutePath,
                "install",
                "-g",
                npmPackage
            ).apply {
                environment().apply {
                    put("NODE", nodeExecutable.absolutePath)
                    put("HOME", context.filesDir.absolutePath)
                    val nodeBin = "${context.filesDir.absolutePath}/nodejs/bin"
                    put("PATH", "$nodeBin:${get("PATH")}")
                }
                directory(context.filesDir)
                redirectErrorStream(true)
            }.start()

            // Read output with progress updates
            val output = StringBuilder()
            val reader = process.inputStream.bufferedReader()
            var line: String?
            var progressCount = 20

            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
                progressCount = minOf(90, progressCount + 5)
                onProgress(progressCount, "インストール中: ${line?.take(40) ?: ""}")
            }

            process.waitFor()

            if (process.exitValue() != 0) {
                return@withContext Result.failure(
                    Exception("npm install failed:\n${output.toString()}")
                )
            }

            onProgress(95, "検証中: インストール")

            // Verify the tool is accessible
            val toolCheck = ProcessBuilder(tool.executable, "--version")
                .apply {
                    environment().apply {
                        val nodeBin = "${context.filesDir.absolutePath}/nodejs/bin"
                        put("PATH", "$nodeBin:${get("PATH")}")
                    }
                }
                .redirectErrorStream(true)
                .start()

            toolCheck.waitFor()

            if (toolCheck.exitValue() != 0) {
                onProgress(100, "警告: インストール済みだが動作確認失敗")
            } else {
                onProgress(100, "完了: ${tool.name} インストール成功")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("npm install error: ${e.message}"))
        }
    }

    /**
     * Find Node.js executable
     */
    private fun findNodeExecutable(): File? {
        val candidates = listOf(
            File(context.filesDir, "nodejs/bin/node"),
            File("/data/data/com.termux/files/usr/bin/node"),
            File("/system/bin/node")
        )
        return candidates.firstOrNull { it.exists() && it.canExecute() }
    }

    /**
     * Find npm executable
     */
    private fun findNpmExecutable(): File? {
        val candidates = listOf(
            File(context.filesDir, "nodejs/bin/npm"),
            File("/data/data/com.termux/files/usr/bin/npm"),
            File("/system/bin/npm")
        )
        return candidates.firstOrNull { it.exists() && it.canExecute() }
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
