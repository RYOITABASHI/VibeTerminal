# VibeTerminal に Node.js と CLI ツールを組み込む計画

## 目標

VibeTerminal内で以下のCLIツールを使えるようにする：
- `claude` (Claude Code CLI by Anthropic)
- `codex` (OpenAI Codex CLI)
- `gemini` (Google Gemini CLI)
- その他のnpmパッケージ

## 実装方法

### アプローチ: Node.js バイナリを組み込む

VibeTerminalアプリ内にNode.jsランタイムを含め、すべてアプリのプライベートディレクトリで動作させる。

## 実装ステップ

### Step 1: Node.js バイナリの取得

Android ARM64用のNode.jsバイナリを取得：

```bash
# 公式のNode.js Android ビルドまたは
# Termuxから既存のNode.jsバイナリをコピー
```

**オプション A: Termux から取得（推奨）**
```bash
# Termuxで確認
which node
# /data/data/com.termux/files/usr/bin/node

# バイナリをコピー
cp /data/data/com.termux/files/usr/bin/node ~/VibeTerminal/app/src/main/assets/
cp -r /data/data/com.termux/files/usr/lib/node_modules ~/VibeTerminal/app/src/main/assets/
```

**オプション B: 公式ビルドをダウンロード**
```bash
# Node.js公式のAndroidビルド
wget https://unofficial-builds.nodejs.org/download/release/v20.10.0/node-v20.10.0-linux-arm64.tar.gz
```

### Step 2: アプリに組み込む

#### 2.1 ディレクトリ構造

```
app/src/main/assets/
├── node              # Node.jsバイナリ
├── npm               # npmスクリプト
└── lib/
    └── node_modules/ # Node.js標準ライブラリ
```

#### 2.2 初回起動時にセットアップ

```kotlin
class NodeJsManager(private val context: Context) {
    private val nodeHome = File(context.filesDir, "nodejs")
    private val nodeBin = File(nodeHome, "bin/node")
    private val npmBin = File(nodeHome, "bin/npm")

    fun setup() {
        if (!nodeHome.exists()) {
            // assetsからNode.jsを展開
            extractNodeFromAssets()
            makeExecutable()
            installCLITools()
        }
    }

    private fun extractNodeFromAssets() {
        // assets/ -> context.filesDir/nodejs/
        context.assets.open("node").use { input ->
            nodeBin.parentFile?.mkdirs()
            nodeBin.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun makeExecutable() {
        nodeBin.setExecutable(true)
        npmBin.setExecutable(true)
    }

    private fun installCLITools() {
        // npm install -g @anthropic-ai/claude-code
        // npm install -g codex-cli
        // npm install -g @google/generative-ai
    }
}
```

### Step 3: PATH環境変数を設定

```kotlin
class TermuxShellSession {
    fun start(): Boolean {
        val nodeHome = File(context.filesDir, "nodejs")
        val nodeBin = File(nodeHome, "bin")

        processBuilder.environment().apply {
            put("HOME", context.filesDir.absolutePath)
            put("PATH", "${nodeBin.absolutePath}:${get("PATH")}")
            put("NODE_PATH", "${nodeHome.absolutePath}/lib/node_modules")
        }
    }
}
```

### Step 4: CLIツール自動インストール

設定画面からワンクリックでインストール：

```kotlin
fun installCLITool(tool: String) {
    when (tool) {
        "claude" -> {
            executeCommand("npm install -g @anthropic-ai/claude-code")
        }
        "codex" -> {
            executeCommand("npm install -g codex-cli")
        }
        "gemini" -> {
            executeCommand("npm install -g @google/generative-ai-cli")
        }
    }
}
```

## 実装の課題と解決策

### 課題1: バイナリサイズ

Node.jsバイナリは約40-50MBと大きい。

**解決策:**
- APKを分割（Dynamic Feature Module）
- 初回起動時にダウンロード
- または圧縮して含める

### 課題2: アーキテクチャ対応

ARM64, ARMv7, x86_64など複数のアーキテクチャに対応が必要。

**解決策:**
- ABIごとにバイナリを用意
- Gradleで自動選択

```gradle
android {
    splits {
        abi {
            enable true
            reset()
            include 'arm64-v8a', 'armeabi-v7a', 'x86_64'
        }
    }
}
```

### 課題3: npm パッケージの保存先

グローバルインストール先をアプリ内に変更。

**解決策:**
```bash
npm config set prefix ~/.npm-global
export PATH=$PATH:~/.npm-global/bin
```

## 簡易実装（最速）

**Termuxが既にインストールされている場合:**

Termux:Tasker プラグインを使ってTermuxのNode.jsを呼び出す。

```kotlin
fun executeInTermux(command: String) {
    val intent = Intent("com.termux.RUN_COMMAND")
    intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/$command")
    intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
    context.sendBroadcast(intent)
}
```

## 推奨実装順序

### フェーズ1: Termux連携（最速）
1. Termux:Tasker プラグインを使う
2. TermuxのNode.js/CLIツールを呼び出し
3. 出力を取得して表示

### フェーズ2: 独自Node.js（中期）
1. Node.jsバイナリを組み込み
2. npmパッケージマネージャー対応
3. CLIツール自動インストール

### フェーズ3: 完全統合（長期）
1. パッケージマネージャーUI
2. CLIツールの自動アップデート
3. カスタムパッケージリポジトリ

## 次のステップ

どのフェーズから始めますか？

1. **最速:** Termux連携（数時間で実装可能）
2. **中期:** Node.js組み込み（1-2日で実装可能）
3. **カスタム:** 独自実装（1週間以上）
