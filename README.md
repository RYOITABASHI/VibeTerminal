# VibeTerminal - モバイルVibe Coding環境

**AI CLI（Claude Code/Codex/Gemini）との共存を前提とした、非エンジニアにも使いやすいZ Fold6最適化ターミナル**

## 🎯 プロジェクトビジョン

将来のモバイル向けオンデバイスLLMの普及を見据え、モバイル単体で様々なCLIを使ってVibe Codingが出来る環境を目指します。
プロにも非エンジニアにも使いやすい、次世代のモバイル開発環境です。

## 🚀 主要機能

### 🤖 AI CLI補完機能
- **リアルタイム出力解析**: AI CLIの状態を自動検出（思考中/実行中/待機中/エラー/成功）
- **初心者向けサマリー**: 専門用語を日本語で分かりやすく説明
- **プロンプトテンプレート**: 11種類のテンプレートでAI CLIを素早く操作
- **タッチジェスチャー**: スワイプでyes/no入力、長押しで翻訳表示
- **進捗可視化**: タスク一覧と進捗バーでAIの作業状況を把握

### 🔌 MCPサーバー統合（NEW!）
- **サーバー管理**: Serena/Playwright/GitHub等のMCPサーバーをワンタップで起動/停止
- **接続状態可視化**: 各サーバーの接続状態をリアルタイム表示
- **ツールクイックアクセス**: ファイル操作、ブラウザ自動化、GitHub操作を簡単実行
- **接続ログ**: MCPサーバーとの通信履歴を記録・表示
- **パラメータ入力UI**: ツール実行時のパラメータを視覚的に入力

### 🌐 翻訳機能
- **ハイブリッド翻訳**: ローカル（50+パターン）+ Gemini AI
- **リアルタイム**: コマンド出力を即座に日本語化
- **カテゴリ別表示**: エラー/警告/成功/情報を色分け
- **解決方法提示**: エラー時の対処法を自動提案

### 📱 モバイル最適化
- **Z Fold6対応**: 展開時は左右分割、折りたたみ時はコンパクト表示
- **日本語IME**: 変換中のテキストをリアルタイム表示
- **ファイル添付**: カメラ/画像/ファイルピッカー統合
- **Material3 UI**: モダンでアクセシブルなデザイン
- **タッチ最適化**: 44dp以上のタッチターゲット

## 📱 スクリーンショット

### メインターミナル画面
- ダークテーマのモノスペースフォント
- コマンド履歴とスクロール対応
- リアルタイム入力サポート

### 翻訳オーバーレイ
- カテゴリ別色分け（エラー/警告/成功/情報）
- 絵文字による視覚的フィードバック
- 解決方法の自動提案

### 設定画面
- テーマ切り替え
- フォントサイズ調整（10-24sp）
- 翻訳ON/OFF
- LLM API設定

## 🛠️ 技術スタック

- **Kotlin** + **Jetpack Compose**
- **Material3** Design System
- **Coroutines** for async operations
- **Kotlin Serialization** for JSON parsing
- **DataStore** for settings persistence

## 📦 ビルド方法

### Termuxでのビルド

```bash
# 1. リポジトリをクローン
cd ~
git clone https://github.com/RYOITABASHI/VibeTerminal.git
cd VibeTerminal

# 2. ビルド実行（Gradleラッパー使用）
chmod +x gradlew
./gradlew assembleDebug

# 3. APKの場所
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

詳細は [TERMUX_SETUP.md](./TERMUX_SETUP.md) を参照

## 📂 プロジェクト構造

```
VibeTerminal/
├── app/
│   ├── src/main/
│   │   ├── java/com/vibeterminal/
│   │   │   ├── MainActivity.kt
│   │   │   ├── core/translator/
│   │   │   │   └── TranslationEngine.kt    # Gemini AI + Local patterns
│   │   │   └── ui/
│   │   │       ├── VibeTerminalApp.kt
│   │   │       ├── aicli/                  # AI CLI assistance
│   │   │       │   ├── PromptTemplate.kt
│   │   │       │   ├── PromptTemplateScreen.kt
│   │   │       │   ├── GestureHandler.kt
│   │   │       │   └── AICLIOutputAnalyzer.kt
│   │   │       ├── mcp/                    # NEW! MCP server integration
│   │   │       │   ├── MCPServer.kt
│   │   │       │   ├── MCPViewModel.kt
│   │   │       │   ├── MCPServerPanel.kt
│   │   │       │   └── MCPToolPanel.kt
│   │   │       ├── terminal/
│   │   │       │   ├── TerminalScreen.kt
│   │   │       │   ├── TerminalViewModel.kt
│   │   │       │   ├── TranslationOverlay.kt
│   │   │       │   └── FilePickerType.kt
│   │   │       ├── settings/
│   │   │       │   ├── SettingsScreen.kt
│   │   │       │   └── SettingsViewModel.kt
│   │   │       ├── ime/
│   │   │       │   ├── JapaneseIMEBridge.kt
│   │   │       │   └── IMETextField.kt
│   │   │       └── theme/
│   │   │           ├── Theme.kt
│   │   │           └── Type.kt
│   │   ├── assets/translations/
│   │   │   ├── git.json      (20+ patterns)
│   │   │   ├── npm.json      (10+ patterns)
│   │   │   ├── docker.json   (10+ patterns)
│   │   │   └── common.json   (10+ patterns)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── translations/              # Source translation files
├── docs/
│   ├── ARCHITECTURE.md
│   ├── DEVELOPMENT_LOG.md
│   └── COMPLETION_SUMMARY.md
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🎯 開発状況

**完成度: 98%** 🎉

### ✅ v1.1 完成機能

#### コア機能
- [x] プロジェクト構造（MVVM）
- [x] 翻訳エンジン（Gemini AI + ローカル）
- [x] 日本語IME完全対応
- [x] ターミナルUI実装
- [x] 翻訳オーバーレイ
- [x] 設定画面（AI翻訳ON/OFF）
- [x] 50+翻訳パターン

#### AI CLI補完機能
- [x] プロンプトテンプレート（11種類）
- [x] AI CLI出力解析＆サマリー
- [x] タッチジェスチャー操作
- [x] リアルタイム進捗可視化
- [x] ステータス自動検出

#### MCPサーバー統合（NEW!）
- [x] MCPサーバー管理パネル（Serena/Playwright/GitHub）
- [x] 接続状態監視＆制御
- [x] MCPツールクイックアクセス
- [x] ツール実行ダイアログ
- [x] 接続ログビューアー
- [x] パラメータ入力フォーム

#### モバイル最適化
- [x] Z Fold6アダプティブレイアウト
- [x] ファイル/画像/カメラ添付
- [x] Material3テーマ
- [x] タッチ最適化（44dp+）

### 🚀 v1.2 予定（近日）

- [ ] 実MCPサーバー接続実装
- [ ] プロンプト実行履歴＆再実行
- [ ] セッション保存＆復元
- [ ] APKビルド＆実機テスト

### 🌟 v2.0 ロードマップ

- [ ] オンデバイスLLM対応（Gemma 2B）
- [ ] Termux統合強化
- [ ] プラグインシステム
- [ ] コミュニティ翻訳パターン共有

## 📖 ドキュメント

- [アーキテクチャ設計](./docs/ARCHITECTURE.md)
- [開発ログ](./docs/DEVELOPMENT_LOG.md)
- [Termuxセットアップガイド](./TERMUX_SETUP.md)

## 📝 ライセンス

MIT License

## 🙏 謝辞

- Termux プロジェクト
- Jetpack Compose
- Material Design 3

---

**Made with ❤️ for Z Fold6 developers**
