package com.example.bluechat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = "",
    val chatId: String = "default" // To separate conversations if needed
)
