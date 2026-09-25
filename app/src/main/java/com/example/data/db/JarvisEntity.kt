package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jarvis_history")
data class JarvisHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commandText: String,
    val responseText: String,
    val actionType: String, // CALL, WHATSAPP, PAYMENT, DEVICE, CHAT, APP, NOTE
    val actionTarget: String, // e.g. Phone number, UPI ID, or action name
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "jarvis_notes")
data class JarvisNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

