package com.vibeterminal.data.chat

import com.vibeterminal.ui.chat.ChatMessage
import com.vibeterminal.ui.chat.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for Chat operations
 */
class ChatRepository(private val chatDao: ChatDao) {

    // Sessions
    fun getAllSessions(): Flow<List<ChatSession>> = chatDao.getAllSessions()

    suspend fun getSessionWithMessages(sessionId: String): ChatSession? {
        val session = chatDao.getSession(sessionId) ?: return null
        val msgs = chatDao.getMessagesForSessionSync(sessionId)
        return session.copy().apply {
            messages = msgs
        }
    }

    suspend fun insertSession(session: ChatSession) {
        chatDao.insertSession(session)
    }

    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
    }

    // Messages
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun insertMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
        // Update session's updatedAt timestamp
        chatDao.getSession(message.sessionId)?.let { session ->
            chatDao.updateSession(
                session.copy(updatedAt = System.currentTimeMillis())
            )
        }
    }

    suspend fun insertMessages(messages: List<ChatMessage>) {
        chatDao.insertMessages(messages)
        // Update session's updatedAt timestamp
        messages.firstOrNull()?.let { message ->
            chatDao.getSession(message.sessionId)?.let { session ->
                chatDao.updateSession(
                    session.copy(updatedAt = System.currentTimeMillis())
                )
            }
        }
    }

    suspend fun deleteMessage(message: ChatMessage) {
        chatDao.deleteMessage(message)
    }

    suspend fun clearMessagesForSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
    }
}
