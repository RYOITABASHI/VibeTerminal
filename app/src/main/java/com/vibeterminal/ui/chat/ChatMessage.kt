package com.vibeterminal.ui.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Chat message role
 */
enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Chat message
 */
@Entity(tableName = "chat_messages")
@TypeConverters(Converters::class)
data class ChatMessage(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_error")
    val isError: Boolean = false,

    val attachments: List<ChatAttachment> = emptyList()
)

/**
 * Chat attachment (files, images, terminal output)
 */
data class ChatAttachment(
    val type: AttachmentType,
    val content: String,
    val name: String? = null,
    val mimeType: String? = null,
    val uri: String? = null,
    val base64Data: String? = null // For images - base64 encoded data
)

enum class AttachmentType {
    FILE,
    IMAGE,
    TERMINAL_OUTPUT,
    CODE_SNIPPET
}

/**
 * Chat session
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),

    val title: String = "新しいチャット",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    // For in-memory use only (not stored in DB)
    @androidx.room.Ignore
    var messages: List<ChatMessage> = emptyList()
}

/**
 * Quick action suggestions for chat
 */
data class QuickAction(
    val id: String,
    val label: String,
    val prompt: String,
    val icon: ImageVector,
    val description: String
)

object QuickActions {
    val ALL = listOf(
        QuickAction(
            id = "explain_error",
            label = "エラーを説明",
            prompt = "このエラーメッセージを初心者にも分かるように説明して、解決方法も教えて",
            icon = Icons.Default.BugReport,
            description = "エラーの意味と解決方法を教えます"
        ),
        QuickAction(
            id = "explain_code",
            label = "コードを説明",
            prompt = "このコードが何をしているか、初心者にも分かるように説明して",
            icon = Icons.Default.Code,
            description = "コードの動作を分かりやすく説明します"
        ),
        QuickAction(
            id = "improve_code",
            label = "コード改善",
            prompt = "このコードをもっと良くする方法を提案して",
            icon = Icons.Default.AutoFixHigh,
            description = "より良い書き方を提案します"
        ),
        QuickAction(
            id = "write_test",
            label = "テストを書く",
            prompt = "このコードのテストを書いて",
            icon = Icons.Default.Science,
            description = "自動テストコードを生成します"
        ),
        QuickAction(
            id = "add_comments",
            label = "コメント追加",
            prompt = "このコードに分かりやすいコメントを追加して",
            icon = Icons.Default.Comment,
            description = "コメントを追加してコードを読みやすくします"
        ),
        QuickAction(
            id = "refactor",
            label = "リファクタリング",
            prompt = "このコードをリファクタリングして、読みやすくして",
            icon = Icons.Default.AutoAwesome,
            description = "コードの構造を整理します"
        )
    )
}

/**
 * Type converters for Room Database
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromChatRole(value: ChatRole): String {
        return value.name
    }

    @TypeConverter
    fun toChatRole(value: String): ChatRole {
        return ChatRole.valueOf(value)
    }

    @TypeConverter
    fun fromAttachmentsList(value: List<ChatAttachment>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAttachmentsList(value: String): List<ChatAttachment> {
        val listType = object : TypeToken<List<ChatAttachment>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String {
        return value.name
    }

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType {
        return AttachmentType.valueOf(value)
    }
}
