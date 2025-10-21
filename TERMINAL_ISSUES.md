# VibeTerminal - 現状の問題点と必要な修正

## 基本機能チェックリスト

### ❌ 動作しない機能

1. **セッション永続性**
   - 問題: 各コマンドが独立したプロセスで実行される
   - テスト: `cd /data && pwd` → `/data/local/tmp` が返る（cdが効かない）
   - 影響: ディレクトリ移動ができない

2. **環境変数の永続化**
   - 問題: export した変数が次のコマンドで消える
   - テスト: `export FOO=bar` → `echo $FOO` → 空
   - 影響: 環境設定ができない

3. **対話的プログラム**
   - 問題: PTY/TTYがない
   - テスト: `vim`, `top`, `nano` → 動作しない
   - 影響: エディタやツールが使えない

4. **リアルタイム出力**
   - 問題: コマンド完了後に一括表示
   - テスト: `ping -c 5 8.8.8.8` → 5秒後に一気に表示
   - 影響: 長時間実行プログラムの状態が見えない

5. **プロンプト**
   - 問題: 固定で `$ ` のみ
   - 期待: `user@host:/path$ ` 形式
   - 影響: 現在のディレクトリが分からない

6. **コマンド履歴**
   - 問題: 上矢印で前のコマンドを呼び出せない
   - 影響: 効率が悪い

7. **シグナルハンドリング**
   - 問題: Ctrl+C で実行中のプロセスを止められない
   - 影響: 暴走プロセスを止められない

8. **パイプとリダイレクト**
   - 状態: 未検証（動作する可能性あり）
   - テスト必要: `ls | grep test`, `echo hello > file.txt`

### ✅ 動作する機能

1. **単発コマンド実行**
   - `ls`, `pwd`, `echo` 等の単純なコマンドは動作

2. **出力表示**
   - 標準出力は表示される（完了後）

3. **エラー表示**
   - エラーメッセージは表示される

## 必要な修正

### 優先度: 高（最低限使えるようにするため）

1. **永続的シェルセッションの実装**
   ```kotlin
   // 継続的なシェルプロセスを保持
   private var shellProcess: Process? = null

   // 初期化時にシェルセッションを開始
   fun startShellSession() {
       shellProcess = ProcessBuilder("/system/bin/sh")
           .directory(File(appHome))
           .redirectErrorStream(true)
           .start()
   }

   // コマンドをシェルに送信
   fun executeInSession(command: String) {
       shellProcess?.outputStream?.write("$command\n".toByteArray())
       shellProcess?.outputStream?.flush()
   }
   ```

2. **リアルタイム出力のストリーミング**
   ```kotlin
   // バックグラウンドで出力を読み取る
   scope.launch {
       shellProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
           _terminalOutput.value += "$line\n"
       }
   }
   ```

3. **動的プロンプトの実装**
   ```kotlin
   // pwdとユーザー情報からプロンプトを生成
   suspend fun updatePrompt() {
       val currentDir = getCurrentDirectory()
       val user = System.getProperty("user.name") ?: "user"
       prompt = "$user:$currentDir$ "
   }
   ```

4. **cdコマンドの特別処理**
   ```kotlin
   // cdは特別に処理
   if (command.startsWith("cd ")) {
       val path = command.substring(3).trim()
       changeDirectory(path)
       return
   }
   ```

### 優先度: 中（使いやすくするため）

5. **コマンド履歴機能**
6. **タブ補完**
7. **Ctrl+Cハンドリング**

### 優先度: 低（将来的に）

8. **PTY/TTYエミュレーション** - vimやtop等の対話的プログラムのため
9. **ANSI/VT100エスケープシーケンス** - 色やカーソル制御
10. **複数タブ/セッション**

## 参考実装

AndroidでターミナルエミュレータToを作る場合の標準的なアプローチ：

1. **Termux** - JNI経由でネイティブPTYを使用
2. **ConnectBot** - Javaレイヤーでのシェルセッション管理
3. **Material Terminal** - Kotlin Coroutinesでのストリーミング

## 次のステップ

1. まず**永続的シェルセッション**を実装
2. **リアルタイム出力**を実装
3. **cdコマンド**の特別処理を実装
4. 基本機能が動作したら、PTY実装を検討
