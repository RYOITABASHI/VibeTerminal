package com.vibeterminal.core.nodejs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Install and manage Node.js runtime in VibeTerminal
 */
class NodeJsInstaller(private val context: Context) {

    private val nodeHome = File(context.filesDir, "nodejs")
    private val nodeBin = File(nodeHome, "bin")
    val nodeExecutable = File(nodeBin, "node")

    companion object {
        // Node.js download URL for Android ARM64
        private const val NODE_VERSION = "v20.18.0"
        private const val NODE_DOWNLOAD_URL = "https://github.com/RYOITABASHI/VibeTerminal/releases/download/node-binaries/node-$NODE_VERSION-android-arm64"
        private const val NODE_EXPECTED_SIZE = 65_000_000L // ~65MB

        private const val SETUP_SCRIPT = """
#!/system/bin/sh
# VibeTerminal Node.js Setup Script

NODE_HOME="${'$'}1"
NODE_BIN="${'$'}NODE_HOME/bin"
NODE="${'$'}NODE_BIN/node"

echo "Setting up Node.js in VibeTerminal..."

# Create necessary directories
mkdir -p "${'$'}NODE_BIN"
mkdir -p "${'$'}NODE_HOME/lib"

# Download npm (minimal installation)
echo "Installing npm..."
cd "${'$'}NODE_HOME"

# Create minimal npm wrapper
cat > "${'$'}NODE_BIN/npm" << 'NPM_EOF'
#!/system/bin/sh
NODE_HOME="${'$'}(dirname ${'$'}(dirname ${'$'}0))"
NODE="${'$'}NODE_HOME/bin/node"
NPM_CLI="${'$'}NODE_HOME/lib/node_modules/npm/bin/npm-cli.js"

if [ ! -f "${'$'}NPM_CLI" ]; then
    echo "npm not installed. Installing..."
    mkdir -p "${'$'}NODE_HOME/lib/node_modules"
    cd "${'$'}NODE_HOME/lib/node_modules"

    # Use node to download and extract npm
    ${'$'}NODE -e "
    const https = require('https');
    const fs = require('fs');
    const tar = require('tar');

    https.get('https://registry.npmjs.org/npm/-/npm-10.2.5.tgz', (res) => {
        res.pipe(tar.x({ cwd: process.cwd() }));
    });
    "
fi

exec "${'$'}NODE" "${'$'}NPM_CLI" "${'$'}@"
NPM_EOF

chmod +x "${'$'}NODE_BIN/npm"

# Create npx wrapper
cat > "${'$'}NODE_BIN/npx" << 'NPX_EOF'
#!/system/bin/sh
NODE_HOME="${'$'}(dirname ${'$'}(dirname ${'$'}0))"
exec "${'$'}NODE_HOME/bin/npm" exec -- "${'$'}@"
NPX_EOF

chmod +x "${'$'}NODE_BIN/npx"

echo "Node.js setup complete!"
echo "Node: ${'$'}NODE"
echo "npm: ${'$'}NODE_BIN/npm"
"""
    }

    /**
     * Check if Node.js is installed
     */
    fun isInstalled(): Boolean {
        // Check VibeTerminal's own Node.js
        if (nodeExecutable.exists() && nodeExecutable.canExecute()) {
            return true
        }

        // Check Termux Node.js - File.exists() may fail on Android 10+ due to sandboxing
        // So we try to execute it instead
        val termuxNode = File("/data/data/com.termux/files/usr/bin/node")
        try {
            val process = ProcessBuilder(termuxNode.absolutePath, "--version")
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            if (process.exitValue() == 0) {
                return true
            }
        } catch (e: Exception) {
            // Try file check as fallback
            if (termuxNode.exists() && termuxNode.canExecute()) {
                return true
            }
        }

        // Check system Node.js using 'which' command
        try {
            val process = ProcessBuilder("sh", "-c", "command -v node")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (process.exitValue() == 0 && output.isNotEmpty()) {
                return true
            }
        } catch (e: Exception) {
            // Ignore
        }

        return false
    }

    /**
     * Install Node.js by checking system-wide availability first
     */
    suspend fun install(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create directories
            nodeBin.mkdirs()

            // First, try to detect Node.js via PATH (works across app boundaries)
            try {
                // Set up environment with Termux paths
                val termuxPrefix = "/data/data/com.termux/files/usr"
                val termuxBin = "$termuxPrefix/bin"
                val appBin = "${context.filesDir.absolutePath}/bin"
                val currentPath = System.getenv("PATH") ?: ""

                val testProcess = ProcessBuilder("node", "--version")
                    .apply {
                        environment().apply {
                            put("PATH", "$nodeBin:$termuxBin:$appBin:$currentPath")
                            put("HOME", context.filesDir.absolutePath)
                        }
                    }
                    .redirectErrorStream(true)
                    .start()
                val versionOutput = testProcess.inputStream.bufferedReader().readText().trim()
                testProcess.waitFor()

                if (testProcess.exitValue() == 0 && versionOutput.isNotEmpty()) {
                    // Node.js is available in PATH (probably from Termux)
                    return@withContext Result.success("Node.js $versionOutput detected in system PATH")
                }
            } catch (e: Exception) {
                // ProcessBuilder failed, but check if Node.js file exists
                // This can happen due to Android app sandboxing
                if (isInstalled()) {
                    // Node.js is actually installed, return success
                    val version = getNodeVersion()
                    return@withContext Result.success("Node.js $version detected (Termux)")
                }

                // Node.js not found anywhere
                return@withContext Result.failure(
                    Exception("Node.js not found. Please install Termux and run: pkg install nodejs")
                )
            }

            // Download Node.js binary (disabled for now due to no hosting)
            return@withContext Result.failure(
                Exception("Node.js auto-download not available. Please install Termux and run: pkg install nodejs")
            )
        } catch (e: Exception) {
            Result.failure(Exception("Installation error: ${e.message}"))
        }
    }

    /**
     * Download and install Node.js from GitHub releases
     */
    private suspend fun downloadAndInstall(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Download Node.js binary
            val connection = java.net.URL(NODE_DOWNLOAD_URL).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "VibeTerminal")

            try {
                connection.connect()

                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(
                        Exception("Download failed: HTTP ${connection.responseCode}")
                    )
                }

                val fileSize = connection.contentLength.toLong()
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(nodeExecutable)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                // Make executable
                if (!nodeExecutable.setExecutable(true, false)) {
                    return@withContext Result.failure(Exception("Failed to set executable permission"))
                }

                // Verify the binary works
                val testProcess = ProcessBuilder(nodeExecutable.absolutePath, "--version")
                    .redirectErrorStream(true)
                    .start()
                val testOutput = testProcess.inputStream.bufferedReader().readText()
                testProcess.waitFor()

                if (testProcess.exitValue() != 0) {
                    return@withContext Result.failure(Exception("Node.js verification failed: $testOutput"))
                }

                // Run setup script to install npm
                val setupScript = File(context.cacheDir, "node_setup.sh")
                setupScript.writeText(SETUP_SCRIPT)
                setupScript.setExecutable(true)

                val process = ProcessBuilder(
                    "/system/bin/sh",
                    setupScript.absolutePath,
                    nodeHome.absolutePath
                ).redirectErrorStream(true).start()

                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

                if (process.exitValue() == 0) {
                    Result.success("Node.js $NODE_VERSION installed successfully")
                } else {
                    Result.failure(Exception("npm setup failed: $output"))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(Exception("Download error: ${e.message}"))
        }
    }

    /**
     * Install a CLI tool using npm
     */
    suspend fun installCLI(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isInstalled()) {
                return@withContext Result.failure(Exception("Node.js not installed"))
            }

            // Set up environment with Termux paths
            val termuxPrefix = "/data/data/com.termux/files/usr"
            val termuxBin = "$termuxPrefix/bin"
            val appBin = "${context.filesDir.absolutePath}/bin"
            val currentPath = System.getenv("PATH") ?: ""

            val process = ProcessBuilder(
                "npm",
                "install",
                "-g",
                packageName
            ).apply {
                environment().apply {
                    put("HOME", context.filesDir.absolutePath)
                    put("PATH", "$nodeBin:$termuxBin:$appBin:$currentPath")
                    put("NPM_CONFIG_PREFIX", termuxPrefix)
                    put("NODE_PATH", "$termuxPrefix/lib/node_modules")
                }
                directory(context.filesDir)
                redirectErrorStream(true)
            }.start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (process.exitValue() == 0) {
                Result.success("Installed $packageName\n$output")
            } else {
                Result.failure(Exception("npm install failed: $output"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get Node.js version
     */
    suspend fun getNodeVersion(): String? = withContext(Dispatchers.IO) {
        try {
            if (!isInstalled()) return@withContext null

            // Try Termux Node.js directly first
            val termuxNode = File("/data/data/com.termux/files/usr/bin/node")
            try {
                val process = ProcessBuilder(termuxNode.absolutePath, "--version")
                    .redirectErrorStream(true)
                    .start()
                val version = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                if (process.exitValue() == 0 && version.isNotEmpty()) {
                    return@withContext version
                }
            } catch (e: Exception) {
                // Continue to next check
            }

            // Try VibeTerminal's own Node.js
            if (nodeExecutable.exists()) {
                try {
                    val process = ProcessBuilder(nodeExecutable.absolutePath, "--version")
                        .redirectErrorStream(true)
                        .start()
                    val version = process.inputStream.bufferedReader().readText().trim()
                    process.waitFor()
                    if (process.exitValue() == 0 && version.isNotEmpty()) {
                        return@withContext version
                    }
                } catch (e: Exception) {
                    // Continue to next check
                }
            }

            // Fallback: Try using PATH-based detection
            val termuxPrefix = "/data/data/com.termux/files/usr"
            val termuxBin = "$termuxPrefix/bin"
            val appBin = "${context.filesDir.absolutePath}/bin"
            val currentPath = System.getenv("PATH") ?: ""

            val process = ProcessBuilder("sh", "-c", "command -v node && node --version")
                .apply {
                    environment().apply {
                        put("PATH", "$nodeBin:$termuxBin:$appBin:$currentPath")
                        put("HOME", context.filesDir.absolutePath)
                    }
                }
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (process.exitValue() == 0) {
                // Extract version from output (last line should be version)
                val version = output.lines().lastOrNull { it.startsWith("v") }
                if (version != null) return@withContext version
            }

            null
        } catch (e: Exception) {
            null
        }
    }
}
