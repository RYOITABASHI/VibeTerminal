# VibeTerminal - モバイルVibe Coding環境

Z Fold6に最適化された、日本語翻訳機能付きTerminalアプリ

## 🚀 特徴

- **リアルタイム翻訳**: コマンド出力を即座に日本語化
- **日本語IME対応**: 変換中のテキストをリアルタイム表示
- **Z Fold6最適化**: 展開時は左右分割UI、折りたたみ時はコンパクト表示
- **オフライン優先**: ローカルパターンマッチング（50+パターン）
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
│   │   │   │   └── TranslationEngine.kt
│   │   │   └── ui/
│   │   │       ├── VibeTerminalApp.kt
│   │   │       ├── terminal/
│   │   │       │   ├── TerminalScreen.kt
│   │   │       │   ├── TerminalViewModel.kt
│   │   │       │   └── TranslationOverlay.kt
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
│   └── DEVELOPMENT_LOG.md
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🎯 開発状況

### ✅ 完成済み (v0.1.0-alpha)

- [x] プロジェクト構造
- [x] 翻訳エンジンコア
- [x] 日本語IMEブリッジ
- [x] ターミナルUI実装
- [x] 翻訳オーバーレイ
- [x] 設定画面
- [x] Z Fold6アダプティブレイアウト
- [x] Material3テーマ
- [x] Gradle設定完了
- [x] 50+翻訳パターン

### 🔄 今後の予定

- [ ] LLM API統合（Claude/GPT）
- [ ] 実機テスト＆バグ修正
- [ ] アプリアイコン作成
- [ ] Google Playリリース

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
