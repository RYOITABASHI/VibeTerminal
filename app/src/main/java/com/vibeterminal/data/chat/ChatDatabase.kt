package com.vibeterminal.data.chat

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vibeterminal.ui.chat.ChatMessage
import com.vibeterminal.ui.chat.ChatSession
import com.vibeterminal.ui.chat.Converters

/**
 * Room Database for Chat
 */
@Database(
    entities = [ChatSession::class, ChatMessage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
