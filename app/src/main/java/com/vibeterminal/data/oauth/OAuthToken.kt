package com.vibeterminal.data.oauth

import kotlinx.serialization.Serializable

/**
 * OAuth認証トークン
 */
@Serializable
data class OAuthToken(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 0,
    val scope: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * トークンの有効期限が切れているかチェック
     */
    fun isExpired(): Boolean {
        if (expiresIn == 0L) return false
        val expirationTime = createdAt + (expiresIn * 1000)
        return System.currentTimeMillis() >= expirationTime
    }

    /**
     * 有効期限まで28日以内かチェック（更新が必要）
     */
    fun needsRefresh(): Boolean {
        if (expiresIn == 0L) return false
        val refreshThreshold = 28 * 24 * 60 * 60 * 1000L // 28日
        val expirationTime = createdAt + (expiresIn * 1000)
        return (expirationTime - System.currentTimeMillis()) <= refreshThreshold
    }
}

/**
 * OAuth認証プロバイダー
 */
enum class OAuthProvider {
    OPENAI,
    ANTHROPIC
}

/**
 * OAuth認証設定
 */
data class OAuthConfig(
    val provider: OAuthProvider,
    val clientId: String,
    val redirectUri: String,
    val scope: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String
)

/**
 * OpenAI OAuth設定
 *
 * 注意: OpenAIの"Sign in with ChatGPT"は現在パイロット段階で、
 * 公式APIドキュメントがありません。Codex CLIの実装を参考にしています。
 *
 * Codex CLI公式Client ID: Iv1.b507a08c87ecfe98
 * 参照: https://github.com/openai/codex
 */
object OpenAIOAuthConfig {
    // Codex CLI公式Client ID（GitHub OAuth App形式）
    const val CLIENT_ID = "Iv1.b507a08c87ecfe98"

    // Androidアプリ用のDeep Link（Codex CLIはlocalhost:1455を使用）
    const val REDIRECT_URI = "vibeterminal://oauth/callback"

    // OpenAI OAuth scope（推定）
    const val SCOPE = "openid profile email"

    // 認証エンドポイント（auth.openai.comまたはGitHub経由の可能性）
    const val AUTHORIZATION_ENDPOINT = "https://auth.openai.com/authorize"

    // トークンエンドポイント
    const val TOKEN_ENDPOINT = "https://auth.openai.com/oauth/token"

    fun getConfig() = OAuthConfig(
        provider = OAuthProvider.OPENAI,
        clientId = CLIENT_ID,
        redirectUri = REDIRECT_URI,
        scope = SCOPE,
        authorizationEndpoint = AUTHORIZATION_ENDPOINT,
        tokenEndpoint = TOKEN_ENDPOINT
    )
}

/**
 * Anthropic OAuth設定（将来的に実装）
 */
object AnthropicOAuthConfig {
    const val CLIENT_ID = "vibeterminal-android"
    const val REDIRECT_URI = "vibeterminal://oauth/callback"
    const val SCOPE = "openid profile email"
    const val AUTHORIZATION_ENDPOINT = "https://auth.anthropic.com/authorize"
    const val TOKEN_ENDPOINT = "https://auth.anthropic.com/oauth/token"

    fun getConfig() = OAuthConfig(
        provider = OAuthProvider.ANTHROPIC,
        clientId = CLIENT_ID,
        redirectUri = REDIRECT_URI,
        scope = SCOPE,
        authorizationEndpoint = AUTHORIZATION_ENDPOINT,
        tokenEndpoint = TOKEN_ENDPOINT
    )
}
