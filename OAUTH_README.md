# OAuth認証の実装について

## 📋 現在の状態

VibeTerminalにOpenAI "Sign in with ChatGPT" OAuth認証を実装しました。

### ✅ 実装済み機能

- OAuth 2.0 + PKCE認証フロー
- EncryptedSharedPreferencesによる安全なトークン保存
- トークンの自動更新
- Android Custom TabsによるOAuth認証
- Deep Linkコールバック（`vibeterminal://oauth/callback`）

### ⚠️ 重要な制限事項

#### 1. パイロット段階の機能
OpenAIの"Sign in with ChatGPT"は**2025年現在、パイロット段階**です：
- 公式APIドキュメントが未公開
- Codex CLI向けに限定公開
- 一般アプリでの利用には申請が必要: https://openai.com/form/sign-in-with-chatgpt/

#### 2. Client ID の制限
現在使用しているClient ID (`Iv1.b507a08c87ecfe98`) は：
- Codex CLI公式のClient ID
- `http://localhost:1455/auth/callback` 向けに登録されている
- Androidアプリの `vibeterminal://oauth/callback` は**登録されていない可能性が高い**

**結果**: OAuth認証が`redirect_uri_mismatch`エラーで失敗する可能性があります。

#### 3. 従量課金について
OAuth認証でログインしても：
- ✅ ログインが簡単（ブラウザで認証）
- ✅ 初回credits付与（Plus: $5、Pro: $50）
- ❌ **サブスクプランの無制限枠は使えません**
- ❌ **初回creditsを使い切ったら従量課金が適用されます**

Codex CLIの特別な枠は、**Codex専用**であり、一般的なChatGPT API利用には適用されません。

## 🔧 動作させるために必要なこと

### オプション1: OpenAIにアプリを登録
1. https://openai.com/form/sign-in-with-chatgpt/ から申請
2. VibeTerminalアプリ用のClient IDとSecretを取得
3. Redirect URI `vibeterminal://oauth/callback` を登録
4. 取得したClient IDをコードに反映

### オプション2: Device Code Flow実装（推奨）
Device Code Flowを実装すれば：
- Redirect URIが不要
- ブラウザで認証コードを入力
- Codex CLIの `--experimental_use-device-code` と同じ方式

### オプション3: APIキー方式を使用（現在動作）
- 設定画面で"APIキーを手動設定"を選択
- OpenAI Platform (platform.openai.com) でAPIキーを取得
- 従量課金で利用

## 📚 参考資料

- [OpenAI Codex CLI GitHub](https://github.com/openai/codex)
- [Codex CLI認証ドキュメント](https://github.com/openai/codex/blob/main/docs/authentication.md)
- [Sign in with ChatGPT申請フォーム](https://openai.com/form/sign-in-with-chatgpt/)

## 🎯 今後の改善

1. **Device Code Flowの実装**
   - ブラウザ不要の認証方式
   - Redirect URI不要

2. **Anthropic Claude OAuth対応**
   - Claude API向けのOAuth実装
   - claude code CLIと同等の機能

3. **トークン管理の改善**
   - 自動リフレッシュの実装
   - エラー時の再認証フロー

---

**注意**: 現在の実装は、OpenAIの公式ドキュメントが公開されるまでの**実験的な実装**です。実際の動作には追加の設定や申請が必要になる可能性があります。
