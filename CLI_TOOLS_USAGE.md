# VibeTerminal で CLI ツールを使う

## 対応しているCLIツール

VibeTerminalから以下のTermuxインストール済みCLIツールを実行できます：

- `claude` - Claude Code CLI (Anthropic)
- `codex` - OpenAI Codex CLI
- `gemini` - Google Gemini CLI
- `node` - Node.js
- `npm` / `npx` - Node Package Manager
- `python` / `python3` - Python
- `pip` - Python Package Manager
- `git` - Git version control

## 使い方

### Step 1: Termuxにインストール

まず、Termuxアプリで必要なCLIツールをインストールしてください：

```bash
# Termuxアプリで実行
pkg update
pkg install nodejs
npm install -g @anthropic-ai/claude-code
npm install -g codex-cli
```

### Step 2: VibeTerminalで実行

VibeTerminalのターミナルで、通常通りコマンドを入力：

```bash
$ claude --version
📱 Executing in Termux: claude --version
⚠️  Output will appear in Termux app
💡 Tip: Check Termux notifications for results
```

## 動作の仕組み

1. **コマンド検出**
   VibeTerminalが `claude`、`codex`などのコマンドを検出

2. **Termuxに転送**
   Termux RUN_COMMAND APIを使ってTermuxアプリに送信

3. **Termuxで実行**
   Termuxの環境で実行（全てのパッケージと設定が利用可能）

4. **結果確認**
   Termuxアプリの通知で出力を確認

## 現在の制限事項

### 出力表示

現在、出力はTermuxアプリに表示されます。VibeTerminalでは以下が表示されます：

```
📱 Executing in Termux: claude --help
⚠️  Output will appear in Termux app
💡 Tip: Check Termux notifications for results
```

**将来的な改善:**
- VibeTerminalで直接出力を表示
- リアルタイム出力のストリーミング
- 対話的なプログラムのサポート

### 対応コマンド

自動検出されるコマンド：
- claude, codex, gemini
- node, npm, npx
- python, python3, pip
- git

その他のコマンドはシステムシェルで実行されます。

## トラブルシューティング

### 「Termux not available」と表示される

**原因:** Termuxアプリがインストールされていない

**解決策:**
1. Google PlayまたはF-DroidからTermuxをインストール
2. Termuxを一度起動してセットアップを完了
3. VibeTerminalを再起動

### コマンドが実行されない

**確認事項:**
1. Termuxアプリでコマンドが動作するか確認
   ```bash
   # Termuxアプリで
   claude --version
   ```

2. Termuxのバックグラウンド実行を許可
   - 設定 → アプリ → Termux → バッテリー → 制限なし

3. Termux:API permissions を確認

### 出力が見つからない

**確認方法:**
1. Termuxアプリを開く
2. 通知パネルを確認
3. Termuxのセッション履歴を確認

## サンプル使用例

### Claude Code CLI

```bash
$ claude --help
$ claude "Explain this code: print('hello')"
```

### Node.js スクリプト

```bash
$ node --version
$ node script.js
```

### npm パッケージ管理

```bash
$ npm list -g
$ npm update -g @anthropic-ai/claude-code
```

### Git 操作

```bash
$ git status
$ git log --oneline
```

## 今後の予定

### フェーズ1 (完了)
- ✅ Termuxコマンド検出
- ✅ Termux RUN_COMMAND統合
- ✅ 基本的なCLIツール対応

### フェーズ2 (予定)
- ⏳ VibeTerminalでの出力表示
- ⏳ リアルタイム出力ストリーミング
- ⏳ エラーハンドリング改善

### フェーズ3 (予定)
- ⏳ 対話的プログラムのサポート
- ⏳ CLIツール自動インストール
- ⏳ パッケージマネージャーUI

## フィードバック

機能の改善提案やバグ報告は、GitHubのIssuesでお願いします。
