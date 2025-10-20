package com.vibeterminal.data.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
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
        private const val TAG = "OAuthManager"

        // PKCE用の状態を一時保存
        private var pendingCodeVerifier: String? = null
        private var pendingState: String? = null

        // API Service instances
        private val openAIApiService by lazy { OpenAIOAuthApiService.create() }
        private val anthropicApiService by lazy { AnthropicOAuthApiService.create() }
    }

    /**
     * OAuth認証フローを開始
     */
    fun startOAuthFlow(config: OAuthConfig) {
        Log.d(TAG, "Starting OAuth flow for provider: ${config.provider}")
        Log.d(TAG, "Client ID: ${config.clientId}")
        Log.d(TAG, "Redirect URI: ${config.redirectUri}")

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

        Log.d(TAG, "Authorization URL: $authUri")

        // Custom Tabsでブラウザを開く
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        try {
            customTabsIntent.launchUrl(context, authUri)
            Log.d(TAG, "Custom Tab launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Custom Tab", e)
            throw Exception("ブラウザを起動できませんでした: ${e.message}")
        }
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
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Exchanging authorization code for token")

                val request = OAuthTokenRequest(
                    grantType = "authorization_code",
                    code = code,
                    redirectUri = config.redirectUri,
                    clientId = config.clientId,
                    codeVerifier = pendingCodeVerifier
                )

                val response = when (config.provider) {
                    OAuthProvider.OPENAI -> openAIApiService.getToken(request)
                    OAuthProvider.ANTHROPIC -> anthropicApiService.getToken(request)
                }

                Log.d(TAG, "Token exchange successful")

                OAuthToken(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    tokenType = response.tokenType,
                    expiresIn = response.expiresIn,
                    scope = response.scope ?: config.scope
                )
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error during token exchange: ${e.code()}", e)
                throw Exception("認証に失敗しました: HTTP ${e.code()} - ${e.message()}")
            } catch (e: Exception) {
                Log.e(TAG, "Error during token exchange", e)
                throw Exception("認証中にエラーが発生しました: ${e.message}")
            }
        }
    }

    /**
     * トークンをリフレッシュ
     */
    suspend fun refreshToken(config: OAuthConfig): Result<OAuthToken> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Refreshing OAuth token")

                val currentToken = repository.getToken(config.provider)
                    ?: return@withContext Result.failure(Exception("トークンが見つかりません"))

                val refreshToken = currentToken.refreshToken
                    ?: return@withContext Result.failure(Exception("リフレッシュトークンが利用できません"))

                val request = OAuthTokenRequest(
                    grantType = "refresh_token",
                    refreshToken = refreshToken,
                    redirectUri = config.redirectUri,
                    clientId = config.clientId
                )

                val response = when (config.provider) {
                    OAuthProvider.OPENAI -> openAIApiService.getToken(request)
                    OAuthProvider.ANTHROPIC -> anthropicApiService.getToken(request)
                }

                Log.d(TAG, "Token refresh successful")

                val newToken = OAuthToken(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken ?: refreshToken, // Keep old refresh token if not provided
                    tokenType = response.tokenType,
                    expiresIn = response.expiresIn,
                    scope = response.scope ?: config.scope
                )

                repository.saveToken(config.provider, newToken)
                Result.success(newToken)
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error during token refresh: ${e.code()}", e)
                Result.failure(Exception("トークン更新に失敗しました: HTTP ${e.code()} - ${e.message()}"))
            } catch (e: Exception) {
                Log.e(TAG, "Error during token refresh", e)
                Result.failure(Exception("トークン更新中にエラーが発生しました: ${e.message}"))
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
