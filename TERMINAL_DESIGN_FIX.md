# VibeTerminal - 正しい設計への修正方針

## 設計の原点

**VibeTerminal = Termuxベースの UI/UX拡張**
- TermuxのShellを使う（独自Shell実装しない）
- AI補完、翻訳、ジェスチャーなどの付加価値
- 非エンジニア向けの使いやすいインターフェース

## 現状の問題

### 問題1: Termuxへのアクセス方法が間違っている

**現在のアプローチ（間違い）：**
```kotlin
// ファイルの存在確認でTermux検出
File("/data/data/com.termux/files/usr/bin/bash").exists()
// → Android 10+ でfalse（サンドボックス）
```

**正しいアプローチ：**
```kotlin
// Termux bashを直接プロセスとして起動
ProcessBuilder("/data/data/com.termux/files/usr/bin/bash", "-l")
    .start()
// → バイナリ実行は可能（ファイル読み取りとは別）
```

### 問題2: 単発コマンド実行になっている

**現在（間違い）：**
```kotlin
// 毎回新しいプロセス
fun executeCommand(cmd: String) {
    ProcessBuilder("sh", "-c", cmd).start()
}
```

**正しい実装：**
```kotlin
// 1つの永続的プロセス
private var shellProcess: Process? = null

fun startSession() {
    shellProcess = ProcessBuilder(
        "/data/data/com.termux/files/usr/bin/bash",
        "-l"  // ログインシェルとして起動
    ).start()

    // バックグラウンドで出力を読み取る
    scope.launch {
        shellProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
            _output.value += "$line\n"
        }
    }
}

fun executeCommand(cmd: String) {
    // プロセスに直接コマンドを送信
    shellProcess?.outputStream?.write("$cmd\n".toByteArray())
    shellProcess?.outputStream?.flush()
}
```

## 修正プラン

### Step 1: 永続的Termux Shellセッションの実装

**新しいクラス: `TermuxShellSession.kt`**

```kotlin
package com.vibeterminal.core.shell

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class TermuxShellSession(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var shellProcess: Process? = null
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    /**
     * Start a persistent Termux bash session
     */
    fun start(): Boolean {
        return try {
            val termuxBash = "/data/data/com.termux/files/usr/bin/bash"
            val termuxHome = "/data/data/com.termux/files/home"
            val termuxPrefix = "/data/data/com.termux/files/usr"

            val processBuilder = ProcessBuilder(termuxBash, "-l")

            // Set Termux environment
            processBuilder.environment().apply {
                put("HOME", termuxHome)
                put("PREFIX", termuxPrefix)
                put("PATH", "$termuxPrefix/bin:$termuxPrefix/bin/applets:${get("PATH")}")
                put("TMPDIR", "$termuxPrefix/tmp")
                put("SHELL", termuxBash)
                put("TERM", "xterm-256color")
                put("LANG", "en_US.UTF-8")
            }

            // Try to set working directory to Termux home
            // If not accessible, use app's files dir
            val workDir = if (File(termuxHome).canRead()) {
                File(termuxHome)
            } else {
                context.filesDir
            }
            processBuilder.directory(workDir)

            processBuilder.redirectErrorStream(true)

            // Start the process
            shellProcess = processBuilder.start()
            _isRunning.value = true

            // Start reading output in background
            startOutputReader()

            true
        } catch (e: Exception) {
            _output.value += "❌ Failed to start Termux shell: ${e.message}\n"
            false
        }
    }

    /**
     * Read shell output continuously
     */
    private fun startOutputReader() {
        scope.launch(Dispatchers.IO) {
            try {
                shellProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                    _output.value += "$line\n"
                }
            } catch (e: Exception) {
                _output.value += "❌ Output reader error: ${e.message}\n"
            } finally {
                _isRunning.value = false
            }
        }
    }

    /**
     * Execute a command in the session
     */
    fun executeCommand(command: String) {
        try {
            shellProcess?.outputStream?.apply {
                write("$command\n".toByteArray())
                flush()
            }
        } catch (e: Exception) {
            _output.value += "❌ Command execution error: ${e.message}\n"
        }
    }

    /**
     * Stop the shell session
     */
    fun stop() {
        try {
            shellProcess?.outputStream?.write("exit\n".toByteArray())
            shellProcess?.outputStream?.flush()
            shellProcess?.waitFor(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
            shellProcess?.destroy()
        } catch (e: Exception) {
            // Force kill
            shellProcess?.destroyForcibly()
        } finally {
            shellProcess = null
            _isRunning.value = false
        }
    }
}
```

### Step 2: TerminalViewModelの修正

```kotlin
class TerminalViewModel : ViewModel() {
    private var termuxSession: TermuxShellSession? = null

    fun initialize(context: Context, ...) {
        // Start Termux shell session
        termuxSession = TermuxShellSession(context, viewModelScope).apply {
            if (start()) {
                // Session started successfully
                viewModelScope.launch {
                    output.collect { newOutput ->
                        _terminalOutput.value = newOutput
                    }
                }
            } else {
                // Fallback to system shell or show error
                _terminalOutput.value += "⚠️  Termux not available. Install Termux app.\n"
            }
        }
    }

    fun executeCommand(command: String) {
        termuxSession?.executeCommand(command)
    }

    override fun onCleared() {
        super.onCleared()
        termuxSession?.stop()
    }
}
```

## 利点

この修正により：

✅ **セッション永続性**
- cdコマンドが動作
- 環境変数が保持される
- シェルスクリプトが正常に動作

✅ **リアルタイム出力**
- コマンド実行中の出力が即座に表示される
- 長時間プログラム（ping, npm install）の進捗が見える

✅ **Termuxの完全な機能**
- Termuxでインストールしたすべてのパッケージが使える
- claude, codex, geminiなどのCLIツールがそのまま動作
- python, node, gitなども利用可能

✅ **本来の設計に準拠**
- VibeTerminalはUI/UX拡張レイヤーとして機能
- Termuxの強力なシェル環境を活用
- 翻訳・AI補完などの付加価値を提供

## テスト計画

修正後、以下をテスト：

1. **基本動作**
   ```bash
   pwd  # /data/data/com.termux/files/home
   ls
   echo $HOME  # 環境変数が保持されているか
   ```

2. **セッション永続性**
   ```bash
   cd /data
   pwd  # /data が返る（cdが効く）
   export FOO=bar
   echo $FOO  # bar が返る（環境変数が保持）
   ```

3. **リアルタイム出力**
   ```bash
   ping -c 5 8.8.8.8  # リアルタイムで出力される
   ```

4. **CLIツール**
   ```bash
   claude --version  # Termuxのclaude-cliが動作
   node --version    # Termuxのnodeが動作
   ```

## 次のステップ

1. `TermuxShellSession.kt`を実装
2. `TerminalViewModel`を修正
3. ビルド＆テスト
4. 動作確認後、他の機能（プロンプト、履歴など）を追加
