package com.vibeterminal.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

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
    private var selectedModel: String = "gpt-4o-mini"

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
                        "content" to buildMessageContent(msg)
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

    private fun buildMessageContent(message: ChatMessage): String {
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
