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
        return nodeExecutable.exists() && nodeExecutable.canExecute()
    }

    /**
     * Install Node.js by downloading from GitHub
     */
    suspend fun install(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create directories
            nodeBin.mkdirs()

            // First, try to use Termux Node.js if available
            val termuxNode = File("/data/data/com.termux/files/usr/bin/node")
            if (termuxNode.exists() && termuxNode.canExecute()) {
                // Create symlink to Termux Node.js
                try {
                    Runtime.getRuntime().exec(arrayOf(
                        "ln", "-sf",
                        termuxNode.absolutePath,
                        nodeExecutable.absolutePath
                    )).waitFor()

                    val termuxNpm = File("/data/data/com.termux/files/usr/bin/npm")
                    if (termuxNpm.exists()) {
                        Runtime.getRuntime().exec(arrayOf(
                            "ln", "-sf",
                            termuxNpm.absolutePath,
                            File(nodeBin, "npm").absolutePath
                        )).waitFor()
                    }

                    return@withContext Result.success("Using Termux Node.js")
                } catch (e: Exception) {
                    // Symlink failed, continue with download
                }
            }

            // Download Node.js binary
            return@withContext downloadAndInstall()
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

            val npmBin = File(nodeBin, "npm")

            val process = ProcessBuilder(
                npmBin.absolutePath,
                "install",
                "-g",
                packageName
            ).apply {
                environment().apply {
                    put("NODE", nodeExecutable.absolutePath)
                    put("HOME", context.filesDir.absolutePath)
                    put("PATH", "${nodeBin.absolutePath}:${get("PATH")}")
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

            val process = ProcessBuilder(
                nodeExecutable.absolutePath,
                "--version"
            ).start()

            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            null
        }
    }
}
