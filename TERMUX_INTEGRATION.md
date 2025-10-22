# VibeTerminal と Termux の関係

## 現状

### VibeTerminal は Termux とは **別のアプリ** です

VibeTerminalは独立したターミナルアプリケーションとして動作します。

## なぜTermux bashを使えないのか？

### Android セキュリティモデル

Androidでは、各アプリは独自のユーザーID (UID) で実行され、他のアプリのプライベートディレクトリにはアクセスできません。

```
Termux:        UID 10488 (u0_a488)
VibeTerminal:  UID 10XXX (u0_aXXX)  ← 異なるUID

/data/data/com.termux/          ← Termuxのみアクセス可能
/data/data/com.vibeterminal/    ← VibeTerminalのみアクセス可能
```

### エラーの意味

```
Cannot run program "/data/data/com.termux/files/usr/bin/bash"
error=2, No such file or directory
```

これは**正常な動作**です。VibeTerminalからTermuxのディレクトリが見えないため、「ファイルが存在しない」と判定されます。

## 解決策

### オプション1: システムシェルを使用（現在の動作）

VibeTerminalは `/system/bin/sh` を使用して動作します。

**メリット:**
- シンプル
- 追加のアプリ不要
- 基本的なシェルコマンドは動作

**デメリット:**
- Termuxパッケージ (node, git, python等) は使えない
- 機能が制限される

### オプション2: Termux:API 経由で連携

Termuxと連携するには、Termux:APIを使用します。

**実装方法:**
```kotlin
// Termuxにコマンドを送信
val intent = Intent("com.termux.RUN_COMMAND")
intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/ls")
context.sendBroadcast(intent)
```

**メリット:**
- Termuxの全機能が使える
- パッケージ管理が可能

**デメリット:**
- Termux:APIアプリが必要
- セットアップが複雑
- リアルタイム出力の取得が難しい

### オプション3: 独自環境を構築

VibeTerminal内に独自のbash/パッケージマネージャーを組み込む。

**メリット:**
- 完全に独立
- カスタマイズ可能

**デメリット:**
- 実装が大規模
- メンテナンスコスト高

### オプション4: BusyBox を組み込む

静的リンクされたBusyBoxバイナリをVibeTerminalに含める。

**メリット:**
- ls, grep, find など基本コマンドが使える
- 軽量（1~2MB）
- 追加アプリ不要

**デメリット:**
- パッケージ管理はできない
- 高度なツールは含まれない

## 推奨アプローチ

**短期:** システムシェル + デバッグ表示の改善（現在の状態）

**中期:** BusyBoxの組み込み + 基本的なコマンドセット

**長期:** Termux:API連携 または 独自環境構築

## 現在のビルドで修正された内容

1. ✅ ターミナル出力が表示される
2. ✅ コマンド実行が動作する
3. ✅ デバッグメッセージが表示される
4. ✅ TTYエラーを解消
5. ✅ プロンプトの重複を修正

## 次のステップ

ユーザーの要望に応じて：

1. **シンプルに:** システムシェルのまま、UI/UX改善に集中
2. **高機能に:** BusyBox組み込み or Termux:API連携

どちらを希望しますか？
