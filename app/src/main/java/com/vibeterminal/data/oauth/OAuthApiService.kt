package com.vibeterminal.data.oauth

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Device Code Request (Device Flow用)
 */
data class DeviceCodeRequest(
    @SerializedName("client_id")
    val clientId: String,

    @SerializedName("scope")
    val scope: String? = null
)

/**
 * Device Code Response
 */
data class DeviceCodeResponse(
    @SerializedName("device_code")
    val deviceCode: String,

    @SerializedName("user_code")
    val userCode: String,

    @SerializedName("verification_uri")
    val verificationUri: String,

    @SerializedName("verification_uri_complete")
    val verificationUriComplete: String? = null,

    @SerializedName("expires_in")
    val expiresIn: Long,

    @SerializedName("interval")
    val interval: Long = 5
)

/**
 * OAuth Token Request (認可コード交換)
 */
data class OAuthTokenRequest(
    @SerializedName("grant_type")
    val grantType: String,

    @SerializedName("code")
    val code: String? = null,

    @SerializedName("redirect_uri")
    val redirectUri: String? = null,

    @SerializedName("client_id")
    val clientId: String,

    @SerializedName("code_verifier")
    val codeVerifier: String? = null,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("device_code")
    val deviceCode: String? = null
)

/**
 * OAuth Token Response
 */
data class OAuthTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("expires_in")
    val expiresIn: Long,

    @SerializedName("scope")
    val scope: String? = null
)

/**
 * OAuth Error Response
 */
data class OAuthErrorResponse(
    @SerializedName("error")
    val error: String,

    @SerializedName("error_description")
    val errorDescription: String? = null
)

/**
 * OpenAI OAuth API Service
 */
interface OpenAIOAuthApiService {

    @POST("device/code")
    suspend fun getDeviceCode(@Body request: DeviceCodeRequest): DeviceCodeResponse

    @POST("oauth/token")
    suspend fun getToken(@Body request: OAuthTokenRequest): OAuthTokenResponse

    companion object {
        private const val BASE_URL = "https://auth.openai.com/"

        fun create(): OpenAIOAuthApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(OpenAIOAuthApiService::class.java)
        }
    }
}

/**
 * Anthropic OAuth API Service (将来実装用)
 */
interface AnthropicOAuthApiService {

    @POST("device/code")
    suspend fun getDeviceCode(@Body request: DeviceCodeRequest): DeviceCodeResponse

    @POST("oauth/token")
    suspend fun getToken(@Body request: OAuthTokenRequest): OAuthTokenResponse

    companion object {
        private const val BASE_URL = "https://auth.anthropic.com/"

        fun create(): AnthropicOAuthApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(AnthropicOAuthApiService::class.java)
        }
    }
}
