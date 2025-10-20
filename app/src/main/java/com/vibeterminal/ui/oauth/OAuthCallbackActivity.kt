package com.vibeterminal.ui.oauth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vibeterminal.data.oauth.OAuthManager
import com.vibeterminal.data.oauth.OAuthRepository
import com.vibeterminal.data.oauth.OpenAIOAuthConfig
import kotlinx.coroutines.launch

/**
 * OAuth認証のコールバックを受け取るActivity
 * Deep Link: vibeterminal://oauth/callback
 */
class OAuthCallbackActivity : ComponentActivity() {

    companion object {
        private const val TAG = "OAuthCallbackActivity"
    }

    private lateinit var oauthManager: OAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = OAuthRepository(this)
        oauthManager = OAuthManager(this, repository)

        // Deep Linkからコールバックデータを取得
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data

        Log.d(TAG, "Received OAuth callback: $data")

        if (data != null && data.scheme == "vibeterminal" && data.host == "oauth") {
            // エラーがある場合
            val error = data.getQueryParameter("error")
            if (error != null) {
                val errorDescription = data.getQueryParameter("error_description") ?: "不明なエラー"
                Log.e(TAG, "OAuth error: $error - $errorDescription")
                finishWithResult(false, "認証エラー: $errorDescription")
                return
            }

            // 正常なコールバック処理
            lifecycleScope.launch {
                try {
                    val config = OpenAIOAuthConfig.getConfig()
                    val result = oauthManager.handleCallback(data, config)

                    result.onSuccess { token ->
                        // 認証成功
                        Log.d(TAG, "OAuth authentication successful")
                        finishWithResult(true, "ChatGPTアカウントで認証しました")
                    }.onFailure { error ->
                        // 認証失敗
                        Log.e(TAG, "OAuth authentication failed", error)
                        finishWithResult(false, "認証に失敗: ${error.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during OAuth callback", e)
                    finishWithResult(false, "予期しないエラー: ${e.message}")
                }
            }
        } else {
            Log.e(TAG, "Invalid callback URL: $data")
            finishWithResult(false, "無効なコールバックURL")
        }
    }

    private fun finishWithResult(success: Boolean, message: String) {
        // ユーザーにToastで結果を表示
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
        }

        // 結果をブロードキャスト
        val intent = Intent("com.vibeterminal.OAUTH_RESULT").apply {
            putExtra("success", success)
            putExtra("message", message)
        }
        sendBroadcast(intent)

        // Activityを終了
        finish()
    }
}
