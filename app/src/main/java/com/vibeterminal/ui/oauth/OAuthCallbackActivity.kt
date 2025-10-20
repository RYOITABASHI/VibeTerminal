package com.vibeterminal.ui.oauth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

        if (data != null && data.scheme == "vibeterminal" && data.host == "oauth") {
            lifecycleScope.launch {
                val config = OpenAIOAuthConfig.getConfig()
                val result = oauthManager.handleCallback(data, config)

                result.onSuccess { token ->
                    // 認証成功
                    // メインアクティビティに戻る
                    finishWithResult(true, "認証に成功しました")
                }.onFailure { error ->
                    // 認証失敗
                    finishWithResult(false, "認証に失敗しました: ${error.message}")
                }
            }
        } else {
            finishWithResult(false, "無効なコールバックURL")
        }
    }

    private fun finishWithResult(success: Boolean, message: String) {
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
