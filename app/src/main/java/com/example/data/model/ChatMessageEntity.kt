package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shipmentId: Int,
    val bidId: Int? = null,
    val senderRole: String, // "DRIVER", "CONSIGNEE", "SYSTEM"
    val senderName: String,
    val receiverName: String,
    val message: String,
    val messageType: String = "TEXT", // "TEXT", "VOICE_NOTE", "SYSTEM", "CALL_LOG"
    val voiceNoteDurationSec: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
