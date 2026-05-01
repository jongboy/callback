package com.callbacksms.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class MessageTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)
