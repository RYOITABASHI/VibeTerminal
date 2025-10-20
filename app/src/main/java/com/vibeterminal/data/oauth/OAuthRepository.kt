package com.vibeterminal.data.oauth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * OAuth認証トークンを安全に保存・管理するリポジトリ
 */
class OAuthRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    // EncryptedSharedPreferencesのインスタンス
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "oauth_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // 現在のOAuthトークンのFlow
    private val _oauthToken = MutableStateFlow<OAuthToken?>(null)
    val oauthToken: Flow<OAuthToken?> = _oauthToken.asStateFlow()

    // 認証状態のFlow
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: Flow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        // 起動時にトークンを読み込む
        loadToken()
    }

    /**
     * トークンを保存
     */
    suspend fun saveToken(provider: OAuthProvider, token: OAuthToken) {
        withContext(Dispatchers.IO) {
            try {
                val tokenJson = json.encodeToString(token)
                encryptedPrefs.edit()
                    .putString(getTokenKey(provider), tokenJson)
                    .apply()

                _oauthToken.value = token
                _isAuthenticated.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * トークンを読み込む
     */
    private fun loadToken(provider: OAuthProvider = OAuthProvider.OPENAI) {
        try {
            val tokenJson = encryptedPrefs.getString(getTokenKey(provider), null)
            if (tokenJson != null) {
                val token = json.decodeFromString<OAuthToken>(tokenJson)
                _oauthToken.value = token
                _isAuthenticated.value = !token.isExpired()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * トークンを取得
     */
    suspend fun getToken(provider: OAuthProvider = OAuthProvider.OPENAI): OAuthToken? {
        return withContext(Dispatchers.IO) {
            try {
                val tokenJson = encryptedPrefs.getString(getTokenKey(provider), null)
                if (tokenJson != null) {
                    json.decodeFromString<OAuthToken>(tokenJson)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * トークンを削除（ログアウト）
     */
    suspend fun clearToken(provider: OAuthProvider = OAuthProvider.OPENAI) {
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit()
                .remove(getTokenKey(provider))
                .apply()

            _oauthToken.value = null
            _isAuthenticated.value = false
        }
    }

    /**
     * トークンの有効性をチェック
     */
    suspend fun isTokenValid(provider: OAuthProvider = OAuthProvider.OPENAI): Boolean {
        val token = getToken(provider)
        return token != null && !token.isExpired()
    }

    /**
     * トークンの更新が必要かチェック
     */
    suspend fun shouldRefreshToken(provider: OAuthProvider = OAuthProvider.OPENAI): Boolean {
        val token = getToken(provider)
        return token != null && token.needsRefresh()
    }

    /**
     * プロバイダー別のキーを生成
     */
    private fun getTokenKey(provider: OAuthProvider): String {
        return "oauth_token_${provider.name.lowercase()}"
    }

    /**
     * すべてのトークンをクリア
     */
    suspend fun clearAllTokens() {
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().clear().apply()
            _oauthToken.value = null
            _isAuthenticated.value = false
        }
    }
}
