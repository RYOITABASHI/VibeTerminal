package com.vibeterminal.data.chat

import androidx.room.*
import com.vibeterminal.ui.chat.ChatMessage
import com.vibeterminal.ui.chat.ChatSession
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Chat
 */
@Dao
interface ChatDao {
    // Sessions
    @Query("SELECT * FROM chat_sessions ORDER BY updated_at DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Update
    suspend fun updateSession(session: ChatSession)

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    // Messages
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionSync(sessionId: String): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    @Delete
    suspend fun deleteMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE session_id = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}
