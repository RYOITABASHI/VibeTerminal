package com.vibeterminal.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * ViewModel for AI Chat
 */
class ChatViewModel : ViewModel() {

    private val _currentSession = MutableStateFlow(ChatSession())
    val currentSession: StateFlow<ChatSession> = _currentSession.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var openAiApiKey: String? = null
    private var selectedModel: String = "gpt-4o" // Vision-capable model

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun setOpenAiApiKey(apiKey: String?) {
        openAiApiKey = apiKey
    }

    fun setModel(model: String) {
        selectedModel = model
    }

    /**
     * Send a message to the AI
     */
    fun sendMessage(content: String, attachments: List<ChatAttachment> = emptyList()) {
        if (content.isBlank() && attachments.isEmpty()) return

        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(
                role = ChatRole.USER,
                content = content,
                attachments = attachments
            )

            _currentSession.value = _currentSession.value.copy(
                messages = _currentSession.value.messages + userMessage,
                updatedAt = System.currentTimeMillis()
            )

            _isLoading.value = true

            try {
                // Call OpenAI API
                val response = callOpenAI(
                    messages = _currentSession.value.messages,
                    apiKey = openAiApiKey
                )

                // Add assistant message
                val assistantMessage = ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = response
                )

                _currentSession.value = _currentSession.value.copy(
                    messages = _currentSession.value.messages + assistantMessage,
                    updatedAt = System.currentTimeMillis()
                )

            } catch (e: Exception) {
                // Add error message
                val errorMessage = ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "エラーが発生しました: ${e.message}",
                    isError = true
                )

                _currentSession.value = _currentSession.value.copy(
                    messages = _currentSession.value.messages + errorMessage
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Call OpenAI Chat Completion API
     */
    private suspend fun callOpenAI(
        messages: List<ChatMessage>,
        apiKey: String?
    ): String {
        if (apiKey.isNullOrBlank()) {
            throw Exception("OpenAI API keyが設定されていません。設定画面で設定してください。")
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("https://api.openai.com/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true

                // Build request body
                val requestMessages = messages.map { msg ->
                    mapOf(
                        "role" to when (msg.role) {
                            ChatRole.USER -> "user"
                            ChatRole.ASSISTANT -> "assistant"
                            ChatRole.SYSTEM -> "system"
                        },
                        "content" to buildMessageContentForAPI(msg)
                    )
                }

                val requestBody = mapOf(
                    "model" to selectedModel,
                    "messages" to requestMessages,
                    "temperature" to 0.7,
                    "max_tokens" to 2000
                )

                val requestJson = json.encodeToString(
                    kotlinx.serialization.serializer(),
                    requestBody
                )

                connection.outputStream.use { it.write(requestJson.toByteArray()) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().readText()
                    val response = json.decodeFromString<OpenAIResponse>(responseText)
                    response.choices.firstOrNull()?.message?.content
                        ?: throw Exception("No response from OpenAI")
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.readText()
                        ?: "Unknown error"
                    throw Exception("OpenAI API error ($responseCode): $errorText")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Build message content for OpenAI API (supports Vision API)
     */
    private fun buildMessageContentForAPI(message: ChatMessage): Any {
        val hasImages = message.attachments.any { it.type == AttachmentType.IMAGE && it.base64Data != null }

        return if (hasImages) {
            // Vision API format: array of content parts
            val contentParts = mutableListOf<Map<String, Any>>()

            // Add text content
            if (message.content.isNotBlank()) {
                contentParts.add(mapOf(
                    "type" to "text",
                    "text" to message.content
                ))
            }

            // Add attachments
            message.attachments.forEach { attachment ->
                when (attachment.type) {
                    AttachmentType.IMAGE -> {
                        if (attachment.base64Data != null) {
                            val mimeType = attachment.mimeType ?: "image/jpeg"
                            contentParts.add(mapOf(
                                "type" to "image_url",
                                "image_url" to mapOf(
                                    "url" to "data:$mimeType;base64,${attachment.base64Data}"
                                )
                            ))
                        }
                    }
                    AttachmentType.TERMINAL_OUTPUT -> {
                        contentParts.add(mapOf(
                            "type" to "text",
                            "text" to "\n\nターミナル出力:\n```\n${attachment.content}\n```"
                        ))
                    }
                    AttachmentType.CODE_SNIPPET -> {
                        contentParts.add(mapOf(
                            "type" to "text",
                            "text" to "\n\nコード:\n```\n${attachment.content}\n```"
                        ))
                    }
                    AttachmentType.FILE -> {
                        contentParts.add(mapOf(
                            "type" to "text",
                            "text" to "\n\nファイル ${attachment.name}:\n```\n${attachment.content}\n```"
                        ))
                    }
                }
            }

            contentParts
        } else {
            // Text-only format: simple string
            buildMessageContentAsString(message)
        }
    }

    private fun buildMessageContentAsString(message: ChatMessage): String {
        val content = StringBuilder(message.content)

        // Add attachments as context
        message.attachments.forEach { attachment ->
            when (attachment.type) {
                AttachmentType.TERMINAL_OUTPUT -> {
                    content.append("\n\nターミナル出力:\n```\n${attachment.content}\n```")
                }
                AttachmentType.CODE_SNIPPET -> {
                    content.append("\n\nコード:\n```\n${attachment.content}\n```")
                }
                AttachmentType.FILE -> {
                    content.append("\n\nファイル ${attachment.name}:\n```\n${attachment.content}\n```")
                }
                AttachmentType.IMAGE -> {
                    content.append("\n\n[画像: ${attachment.name}]")
                }
            }
        }

        return content.toString()
    }

    /**
     * Clear current session
     */
    fun clearSession() {
        _currentSession.value = ChatSession()
    }

    /**
     * Create new session
     */
    fun newSession() {
        // Save current session if it has messages
        if (_currentSession.value.messages.isNotEmpty()) {
            _sessions.value = _sessions.value + _currentSession.value
        }

        _currentSession.value = ChatSession()
    }

    /**
     * Load session
     */
    fun loadSession(session: ChatSession) {
        _currentSession.value = session
    }

    /**
     * Process image URI and convert to Base64
     */
    suspend fun processImageUri(context: Context, uri: Uri): ChatAttachment? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    throw Exception("画像の読み込みに失敗しました")
                }

                // Resize if too large (max 2048px)
                val resizedBitmap = if (bitmap.width > 2048 || bitmap.height > 2048) {
                    val scale = 2048f / maxOf(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else {
                    bitmap
                }

                // Convert to Base64
                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64Data = Base64.getEncoder().encodeToString(outputStream.toByteArray())

                // Get MIME type
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

                // Get filename
                val fileName = uri.lastPathSegment ?: "image.jpg"

                ChatAttachment(
                    type = AttachmentType.IMAGE,
                    content = "画像: $fileName",
                    name = fileName,
                    mimeType = mimeType,
                    uri = uri.toString(),
                    base64Data = base64Data
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

// OpenAI API response models
@Serializable
private data class OpenAIResponse(
    val choices: List<Choice>
)

@Serializable
private data class Choice(
    val message: Message
)

@Serializable
private data class Message(
    val content: String
)
