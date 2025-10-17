# VibeTerminal - Termux Development Setup

このガイドでは、TermuxでVibeTerminalを開発する環境をセットアップします。

---

## 📋 前提条件

- Android 7.0以上
- Termux最新版（F-DroidまたはGitHub Releasesから）
- ストレージ権限許可済み

---

## 🚀 クイックスタート

### 1. Termuxで基本パッケージをインストール

```bash
# パッケージリスト更新
pkg update && pkg upgrade -y

# 必要なツールをインストール
pkg install -y \
  git \
  openjdk-17 \
  gradle \
  kotlin \
  wget \
  curl \
  openssh \
  termux-api
```

### 2. Android SDK のインストール

```bash
# Android SDKをダウンロード（コマンドラインツール）
cd ~
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip

# 解凍
pkg install unzip
unzip commandlinetools-linux-9477386_latest.zip -d android-sdk
rm commandlinetools-linux-9477386_latest.zip

# SDK Manager セットアップ
mkdir -p ~/android-sdk/cmdline-tools/latest
mv ~/android-sdk/cmdline-tools/* ~/android-sdk/cmdline-tools/latest/ 2>/dev/null || true

# 環境変数設定
echo 'export ANDROID_HOME="$HOME/android-sdk"' >> ~/.bashrc
echo 'export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"' >> ~/.bashrc
source ~/.bashrc

# SDK コンポーネントのインストール
sdkmanager --update
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 3. VibeTerminalをクローン

```bash
# UserLAndからTermuxにクローン
cd ~

# Option A: UserLAndからSSH経由でクローン
# (UserLAnd側でsshd起動済みの場合)
git clone ssh://userland@localhost:2222/home/userland/VibeTerminal

# Option B: ローカルパスから直接クローン（ストレージ共有済みの場合）
git clone /storage/emulated/0/VibeTerminal

# Option C: GitHubにpush済みの場合
git clone https://github.com/YOUR_USERNAME/VibeTerminal.git
```

### 4. プロジェクトのビルド

```bash
cd ~/VibeTerminal

# Gradleラッパーに実行権限付与
chmod +x gradlew

# ビルド実行
./gradlew assembleDebug
```

---

## 🔧 開発フロー

### ビルド＆インストール

```bash
# デバッグAPKをビルド
./gradlew assembleDebug

# APKの場所
ls -lh app/build/outputs/apk/debug/app-debug.apk

# インストール（USB デバッグまたは直接）
adb install app/build/outputs/apk/debug/app-debug.apk

# または、直接インストール（Termux内から）
termux-open app/build/outputs/apk/debug/app-debug.apk
```

### コードの編集

```bash
# Nano で編集
nano app/src/main/java/com/vibeterminal/ui/terminal/TerminalViewModel.kt

# または Micro（より高機能）
pkg install micro
micro app/TranslationEngine.kt

# または Neovim
pkg install neovim
nvim app/TranslationEngine.kt
```

### テストの実行

```bash
# ユニットテスト
./gradlew test

# テストレポート確認
termux-open app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 🔄 UserLAndとの同期

### SSHで双方向同期

```bash
# UserLAnd → Termux
# (UserLAnd側で実行)
rsync -avz ~/VibeTerminal/ userland@localhost:2222:~/VibeTerminal/

# Termux → UserLAnd
# (Termux側で実行)
rsync -avz ~/VibeTerminal/ ssh://userland@localhost:2222/home/userland/VibeTerminal/
```

### Gitで管理（推奨）

```bash
# Termux側で変更をコミット
cd ~/VibeTerminal
git add .
git commit -m "Implement feature X"
git push origin main

# UserLAnd側でpull
cd ~/VibeTerminal
git pull origin main
```

---

## 📱 デバッグ

### ADB接続（有線または無線）

```bash
# ADB over USB
# 1. 開発者オプションを有効化
# 2. USBデバッグを有効化
# 3. PCまたは別デバイスから接続

# ADB over WiFi（同一ネットワーク内）
# デバイス側で有効化後
adb connect 192.168.x.x:5555
```

### Logcat監視

```bash
# アプリのログを監視
adb logcat | grep VibeTerminal

# または、Termux内でpidcatを使う
pip install pidcat
pidcat com.vibeterminal
```

---

## 🎨 翻訳パターンの追加

```bash
# translations/ ディレクトリに新しいJSONを追加
cd ~/VibeTerminal/translations

# 例: Python用パターン
cat > python.json <<'EOF'
{
  "patterns": [
    {
      "regex": "^ModuleNotFoundError: No module named '(.+)'$",
      "translation": "モジュール '$1' が見つかりません",
      "emoji": "📦",
      "category": "error",
      "suggestion": "pip install $1 を実行してください"
    }
  ]
}
EOF

# TranslationEngine.kt を更新して読み込み
micro ../app/TranslationEngine.kt
# loadPatterns() に "python.json" を追加
```

---

## 🐛 トラブルシューティング

### Gradleビルドエラー

```bash
# Gradle キャッシュをクリア
./gradlew clean

# Gradle デーモン再起動
./gradlew --stop
./gradlew assembleDebug
```

### Java/Kotlinバージョン問題

```bash
# Javaバージョン確認
java -version

# Kotlin バージョン確認
kotlinc -version

# 必要に応じて再インストール
pkg reinstall openjdk-17 kotlin
```

### Android SDK not found

```bash
# ANDROID_HOME が正しく設定されているか確認
echo $ANDROID_HOME

# SDK Manager が動作するか確認
sdkmanager --list

# 再設定
export ANDROID_HOME="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
```

### ストレージ権限エラー

```bash
# Termuxのストレージアクセス許可
termux-setup-storage

# 権限確認
ls ~/storage/shared
```

---

## 📚 参考リンク

- [Termux Wiki](https://wiki.termux.com/)
- [Android Gradle Plugin](https://developer.android.com/build)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Android Studio Command Line Tools](https://developer.android.com/studio/command-line)

---

## 🎉 完了

セットアップが完了したら：

```bash
# プロジェクトディレクトリに移動
cd ~/VibeTerminal

# ビルド
./gradlew assembleDebug

# APKをインストール
termux-open app/build/outputs/apk/debug/app-debug.apk
```

**Happy Vibe Coding! 🚀**
