package com.vibeterminal.core.termux

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * Termux integration helper
 */
class TermuxIntegration(private val context: Context) {

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val TERMUX_API_PACKAGE = "com.termux.api"
        const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
        const val TERMUX_HOME = "/data/data/com.termux/files/home"
    }

    /**
     * Check if Termux is installed
     */
    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Check if Termux:API is installed
     */
    fun isTermuxAPIInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_API_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get Termux version
     */
    fun getTermuxVersion(): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if Termux bash is accessible
     */
    fun isTermuxBashAccessible(): Boolean {
        val bashPath = "$TERMUX_PREFIX/bin/bash"
        return try {
            File(bashPath).exists() && File(bashPath).canExecute()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if Termux home directory is accessible
     */
    fun isTermuxHomeAccessible(): Boolean {
        return try {
            File(TERMUX_HOME).exists() && File(TERMUX_HOME).canRead()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get accessibility status
     */
    fun getAccessibilityStatus(): TermuxAccessibilityStatus {
        return when {
            !isTermuxInstalled() -> TermuxAccessibilityStatus.NOT_INSTALLED
            !isTermuxBashAccessible() && !isTermuxHomeAccessible() -> TermuxAccessibilityStatus.NO_ACCESS
            isTermuxBashAccessible() -> TermuxAccessibilityStatus.FULL_ACCESS
            isTermuxHomeAccessible() -> TermuxAccessibilityStatus.PARTIAL_ACCESS
            else -> TermuxAccessibilityStatus.NO_ACCESS
        }
    }

    /**
     * Open Termux app
     */
    fun openTermux(): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Open Termux at Play Store
     */
    fun openTermuxPlayStore(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("market://details?id=$TERMUX_PACKAGE")
                setPackage("com.android.vending")
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to web browser
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$TERMUX_PACKAGE")
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Execute command via Termux:API
     */
    fun executeViaTermuxAPI(command: String): Boolean {
        if (!isTermuxAPIInstalled()) return false

        return try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                putExtra("com.termux.RUN_COMMAND_PATH", "$TERMUX_PREFIX/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", TERMUX_HOME)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            }
            context.sendBroadcast(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get list of installed Termux packages
     */
    fun getInstalledPackages(): List<String> {
        val listFile = File(TERMUX_PREFIX, "var/lib/dpkg/info")
        if (!listFile.exists() || !listFile.canRead()) {
            return emptyList()
        }

        return try {
            listFile.listFiles()
                ?.filter { it.name.endsWith(".list") }
                ?.map { it.name.removeSuffix(".list") }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if a specific package is installed in Termux
     */
    fun isPackageInstalled(packageName: String): Boolean {
        val packageFile = File(TERMUX_PREFIX, "var/lib/dpkg/info/$packageName.list")
        return packageFile.exists()
    }

    /**
     * Get Termux environment variables
     */
    fun getTermuxEnvironment(): Map<String, String> {
        return mapOf(
            "PREFIX" to TERMUX_PREFIX,
            "HOME" to TERMUX_HOME,
            "PATH" to "$TERMUX_PREFIX/bin:$TERMUX_PREFIX/bin/applets:${System.getenv("PATH")}",
            "TMPDIR" to "$TERMUX_PREFIX/tmp",
            "SHELL" to "$TERMUX_PREFIX/bin/bash",
            "LANG" to "en_US.UTF-8",
            "TERM" to "xterm-256color"
        )
    }

    /**
     * Get detailed Termux status
     */
    fun getStatus(): TermuxStatus {
        return TermuxStatus(
            installed = isTermuxInstalled(),
            version = getTermuxVersion(),
            apiInstalled = isTermuxAPIInstalled(),
            bashAccessible = isTermuxBashAccessible(),
            homeAccessible = isTermuxHomeAccessible(),
            accessibilityStatus = getAccessibilityStatus(),
            installedPackages = if (isTermuxHomeAccessible()) getInstalledPackages().size else 0
        )
    }
}

/**
 * Termux accessibility status
 */
enum class TermuxAccessibilityStatus {
    NOT_INSTALLED,      // Termux not installed
    NO_ACCESS,          // Termux installed but no access to files
    PARTIAL_ACCESS,     // Can read home but not execute bash
    FULL_ACCESS;        // Full access to bash and home

    fun getDescription(): String = when(this) {
        NOT_INSTALLED -> "Termuxがインストールされていません"
        NO_ACCESS -> "Termuxにアクセスできません（Android権限制限）"
        PARTIAL_ACCESS -> "Termuxに部分的にアクセス可能"
        FULL_ACCESS -> "Termuxに完全アクセス可能"
    }

    fun canExecute(): Boolean = this == FULL_ACCESS
}

/**
 * Termux status data class
 */
data class TermuxStatus(
    val installed: Boolean,
    val version: String?,
    val apiInstalled: Boolean,
    val bashAccessible: Boolean,
    val homeAccessible: Boolean,
    val accessibilityStatus: TermuxAccessibilityStatus,
    val installedPackages: Int
) {
    fun toDisplayString(): String {
        return buildString {
            appendLine("Termux ステータス:")
            appendLine("  インストール: ${if (installed) "はい (v$version)" else "いいえ"}")
            appendLine("  Termux:API: ${if (apiInstalled) "はい" else "いいえ"}")
            appendLine("  アクセス状態: ${accessibilityStatus.getDescription()}")
            if (installed) {
                appendLine("  bash実行可: ${if (bashAccessible) "はい" else "いいえ"}")
                appendLine("  ホーム読取可: ${if (homeAccessible) "はい" else "いいえ"}")
                if (installedPackages > 0) {
                    appendLine("  インストール済パッケージ: $installedPackages")
                }
            }
        }
    }
}
