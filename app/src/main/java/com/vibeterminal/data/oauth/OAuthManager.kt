package com.vibeterminal.data.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * OAuth認証フローを管理するマネージャー
 */
class OAuthManager(
    private val context: Context,
    private val repository: OAuthRepository
) {

    companion object {
        // PKCE用の状態を一時保存
        private var pendingCodeVerifier: String? = null
        private var pendingState: String? = null
    }

    /**
     * OAuth認証フローを開始
     */
    fun startOAuthFlow(config: OAuthConfig) {
        // PKCE用のcode_verifierとcode_challengeを生成
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val state = generateRandomString(32)

        // 一時保存
        pendingCodeVerifier = codeVerifier
        pendingState = state

        // 認証URLを構築
        val authUri = Uri.parse(config.authorizationEndpoint).buildUpon()
            .appendQueryParameter("client_id", config.clientId)
            .appendQueryParameter("redirect_uri", config.redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", config.scope)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

        // Custom Tabsでブラウザを開く
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(context, authUri)
    }

    /**
     * コールバックURLを処理
     */
    suspend fun handleCallback(uri: Uri, config: OAuthConfig): Result<OAuthToken> {
        return withContext(Dispatchers.IO) {
            try {
                // stateを検証
                val state = uri.getQueryParameter("state")
                if (state != pendingState) {
                    return@withContext Result.failure(Exception("Invalid state parameter"))
                }

                // 認可コードを取得
                val code = uri.getQueryParameter("code")
                    ?: return@withContext Result.failure(Exception("Authorization code not found"))

                // トークンを取得
                val token = exchangeCodeForToken(code, config)

                // トークンを保存
                repository.saveToken(config.provider, token)

                // 一時データをクリア
                pendingCodeVerifier = null
                pendingState = null

                Result.success(token)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 認可コードをトークンと交換
     */
    private suspend fun exchangeCodeForToken(code: String, config: OAuthConfig): OAuthToken {
        // TODO: 実際のHTTP通信を実装
        // ここでは仮のトークンを返す
        // 本番実装ではRetrofitなどでPOSTリクエストを送信

        /*
        val requestBody = mapOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to config.redirectUri,
            "client_id" to config.clientId,
            "code_verifier" to pendingCodeVerifier
        )

        // POST request to config.tokenEndpoint
        */

        // 仮実装：テスト用のトークン
        return OAuthToken(
            accessToken = "test_access_token_$code",
            refreshToken = "test_refresh_token",
            tokenType = "Bearer",
            expiresIn = 3600,
            scope = config.scope
        )
    }

    /**
     * トークンをリフレッシュ
     */
    suspend fun refreshToken(config: OAuthConfig): Result<OAuthToken> {
        return withContext(Dispatchers.IO) {
            try {
                val currentToken = repository.getToken(config.provider)
                    ?: return@withContext Result.failure(Exception("No refresh token available"))

                val refreshToken = currentToken.refreshToken
                    ?: return@withContext Result.failure(Exception("No refresh token available"))

                // TODO: 実際のHTTP通信を実装
                /*
                val requestBody = mapOf(
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken,
                    "client_id" to config.clientId
                )

                // POST request to config.tokenEndpoint
                */

                // 仮実装：新しいトークン
                val newToken = OAuthToken(
                    accessToken = "refreshed_access_token",
                    refreshToken = refreshToken,
                    tokenType = "Bearer",
                    expiresIn = 3600,
                    scope = config.scope
                )

                repository.saveToken(config.provider, newToken)
                Result.success(newToken)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * ログアウト
     */
    suspend fun logout(provider: OAuthProvider) {
        repository.clearToken(provider)
    }

    /**
     * PKCE用のcode_verifierを生成
     */
    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * PKCE用のcode_challengeを生成
     */
    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * ランダム文字列を生成
     */
    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}
