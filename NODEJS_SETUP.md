# VibeTerminal で Node.js と CLI ツールを使う

## 自動セットアップ

VibeTerminalを初めて起動すると、Node.jsが自動的にセットアップされます：

```
📦 Setting up Node.js environment...
✅ Node.js installed successfully
💡 You can now install CLI tools with: npm install -g <package>

✅ Node.js v20.10.0 ready
```

## CLI ツールのインストール

### Claude Code CLI

```bash
$ npm install -g @anthropic-ai/claude-code
$ claude --version
```

### Codex CLI

```bash
$ npm install -g codex-cli
$ codex --help
```

### Gemini CLI (Google)

```bash
$ npm install -g @google/generative-ai-cli
$ gemini --version
```

## 使い方

インストールしたCLIツールは、そのまま実行できます：

```bash
# Claude Code
$ claude "Explain this code: console.log('hello')"

# Node.js スクリプト
$ node script.js

# npm コマンド
$ npm list -g
$ npm update -g @anthropic-ai/claude-code
```

## 技術詳細

### Node.js の場所

- **Binary**: `/data/user/0/com.vibeterminal/files/nodejs/bin/node`
- **npm**: `/data/user/0/com.vibeterminal/files/nodejs/bin/npm`
- **Global modules**: `/data/user/0/com.vibeterminal/files/nodejs/lib/node_modules`

### 環境変数

VibeTerminalのシェルセッションには以下が自動設定されます：

```bash
NODE_HOME=/data/user/0/com.vibeterminal/files/nodejs
PATH=$NODE_HOME/bin:$PATH
NODE_PATH=$NODE_HOME/lib/node_modules
```

### セッション永続性

VibeTerminal内のNode.js環境は**完全に独立**しています：

- ✅ Termuxとは別のNode.jsインストール
- ✅ VibeTerminal専用のpackage環境
- ✅ セッション永続化（cdやexportが効く）
- ✅ VibeTerminal内で完結

## Termux との違い

| 項目 | VibeTerminal | Termux連携（旧実装） |
|------|--------------|---------------------|
| Node.js | 独自インストール | Termux依存 |
| セッション | 永続化 | 一時的 |
| 出力 | VibeTerminal内 | Termuxアプリ |
| パッケージ | 独立管理 | Termux共有 |

## ビルド時の注意

### Node.jsバイナリの準備

GitHubからクローンした場合、Node.jsバイナリは含まれていません（65MBのため.gitignore）。

ビルド前に手動でコピー：

```bash
# Termuxで実行
cp /data/data/com.termux/files/usr/bin/node \
   ~/VibeTerminal/app/src/main/assets/node/bin/
```

または公式ビルドをダウンロード：

```bash
cd ~/VibeTerminal/app/src/main/assets/
mkdir -p node/bin
cd node/bin
wget https://unofficial-builds.nodejs.org/download/release/v20.10.0/node-v20.10.0-linux-arm64.tar.gz
tar -xzf node-v20.10.0-linux-arm64.tar.gz --strip-components=2 node-v20.10.0-linux-arm64/bin/node
```

## トラブルシューティング

### Node.jsセットアップが失敗する

**確認事項:**
1. assets/node/bin/node が存在するか
2. ストレージ容量が十分か（100MB以上推奨）

**解決策:**
```bash
# VibeTerminalで
$ ls /data/user/0/com.vibeterminal/files/nodejs/bin/node
```

存在しない場合は、アプリを再インストール。

### npm install が遅い

**原因:** 初回はnpm自体のダウンロードも行われます。

**解決策:** しばらく待つ（1-2分）

### パッケージが見つからない

**確認:**
```bash
$ npm list -g
$ echo $PATH
```

**修正:**
```bash
$ export PATH=/data/user/0/com.vibeterminal/files/nodejs/bin:$PATH
```

## 推奨パッケージ

### AI/LLM CLI Tools

```bash
npm install -g @anthropic-ai/claude-code  # Claude
npm install -g codex-cli                  # OpenAI Codex
npm install -g @google/generative-ai-cli  # Gemini
```

### Development Tools

```bash
npm install -g typescript   # TypeScript
npm install -g prettier     # Code formatter
npm install -g eslint       # Linter
```

### Utilities

```bash
npm install -g http-server  # Simple HTTP server
npm install -g json-server  # Mock REST API
npm install -g nodemon      # Auto-restart
```

## サンプル使用例

### Claude Code で開発

```bash
$ cd ~/MyProject
$ claude "Create a React component for user profile"
$ node profile.js
```

### TypeScript プロジェクト

```bash
$ npm install -g typescript
$ tsc --init
$ tsc app.ts
$ node app.js
```

### ローカルサーバー

```bash
$ npm install -g http-server
$ http-server .
Starting up http-server on port 8080
```

## 今後の予定

- [ ] GUI経由でのパッケージ管理
- [ ] よく使うCLIツールのプリインストール
- [ ] Node.jsバージョン管理（nvm相当）
- [ ] パッケージ更新通知

## フィードバック

問題や改善提案は GitHubのIssues へ！
