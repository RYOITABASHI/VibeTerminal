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
 * BusyBox download and installation manager
 */
class BusyBoxManager(private val context: Context) {

    private val binDir = File(context.filesDir, "bin")
    private val busyBoxFile = File(binDir, "busybox")

    // BusyBox download URLs for different architectures
    private val busyBoxUrls = mapOf(
        "arm64-v8a" to "https://busybox.net/downloads/binaries/1.35.0-x86_64-linux-musl/busybox_ASH",
        "armeabi-v7a" to "https://busybox.net/downloads/binaries/1.35.0-armv7l-linux-musleabihf/busybox_ASH",
        "x86_64" to "https://busybox.net/downloads/binaries/1.35.0-x86_64-linux-musl/busybox_ASH",
        "x86" to "https://busybox.net/downloads/binaries/1.35.0-i686-linux-musl/busybox_ASH"
    )

    /**
     * Check if BusyBox is installed
     */
    fun isInstalled(): Boolean {
        return busyBoxFile.exists() && busyBoxFile.canExecute()
    }

    /**
     * Get BusyBox version
     */
    suspend fun getVersion(): String? = withContext(Dispatchers.IO) {
        if (!isInstalled()) return@withContext null

        try {
            val process = ProcessBuilder(busyBoxFile.absolutePath, "--help")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // Extract version from first line
            output.lines().firstOrNull()?.let { firstLine ->
                Regex("BusyBox v([0-9.]+)").find(firstLine)?.groupValues?.get(1)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Download and install BusyBox
     */
    suspend fun install(
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Create bin directory if not exists
            if (!binDir.exists()) {
                binDir.mkdirs()
            }

            onProgress(0, "検出中: デバイスアーキテクチャ")

            // Detect device architecture
            val arch = detectArchitecture()
            val downloadUrl = busyBoxUrls[arch]
                ?: return@withContext Result.failure(
                    Exception("サポートされていないアーキテクチャ: $arch")
                )

            onProgress(20, "ダウンロード中: BusyBox")

            // Download BusyBox
            downloadFile(downloadUrl, busyBoxFile) { progress ->
                onProgress(20 + (progress * 0.6).toInt(), "ダウンロード中: $progress%")
            }

            onProgress(80, "インストール中: 実行権限を設定")

            // Set executable permission
            busyBoxFile.setExecutable(true, false)

            onProgress(90, "検証中: インストール")

            // Verify installation
            if (!isInstalled()) {
                return@withContext Result.failure(Exception("BusyBoxのインストールに失敗しました"))
            }

            // Create symlinks for common commands
            onProgress(95, "作成中: コマンドシンボリックリンク")
            createSymlinks()

            onProgress(100, "完了: BusyBox インストール成功")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uninstall BusyBox
     */
    fun uninstall(): Boolean {
        return try {
            // Remove symlinks
            binDir.listFiles()?.forEach { file ->
                if (file.name != "busybox" && file.exists()) {
                    file.delete()
                }
            }

            // Remove BusyBox binary
            busyBoxFile.delete()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detect device architecture
     */
    private fun detectArchitecture(): String {
        val supportedAbis = Build.SUPPORTED_ABIS
        return when {
            supportedAbis.contains("arm64-v8a") -> "arm64-v8a"
            supportedAbis.contains("armeabi-v7a") -> "armeabi-v7a"
            supportedAbis.contains("x86_64") -> "x86_64"
            supportedAbis.contains("x86") -> "x86"
            else -> supportedAbis[0]
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
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        try {
            connection.connect()

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
     * Create symlinks for common BusyBox commands
     */
    private fun createSymlinks() {
        // Get list of available BusyBox applets
        val applets = try {
            val process = ProcessBuilder(busyBoxFile.absolutePath, "--list")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            output.lines().filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }

        // Priority commands to create symlinks for
        val priorityCommands = setOf(
            "sh", "ls", "cat", "grep", "find", "sed", "awk",
            "wget", "curl", "tar", "gzip", "unzip",
            "ps", "top", "kill", "chmod", "chown",
            "cp", "mv", "rm", "mkdir", "touch",
            "echo", "printf", "test", "expr"
        )

        // Create symlinks for priority commands
        priorityCommands.forEach { command ->
            if (applets.contains(command)) {
                val symlink = File(binDir, command)
                if (!symlink.exists()) {
                    try {
                        // Create symbolic link (Android 5.0+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            android.system.Os.symlink(busyBoxFile.absolutePath, symlink.absolutePath)
                        } else {
                            // Fallback: Create wrapper script
                            symlink.writeText("#!/system/bin/sh\n${busyBoxFile.absolutePath} $command \"\$@\"\n")
                            symlink.setExecutable(true, false)
                        }
                    } catch (e: Exception) {
                        // Ignore symlink creation errors
                    }
                }
            }
        }
    }

    /**
     * Get list of available BusyBox commands
     */
    suspend fun getAvailableCommands(): List<String> = withContext(Dispatchers.IO) {
        if (!isInstalled()) return@withContext emptyList()

        try {
            val process = ProcessBuilder(busyBoxFile.absolutePath, "--list")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            output.lines().filter { it.isNotBlank() }.sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
