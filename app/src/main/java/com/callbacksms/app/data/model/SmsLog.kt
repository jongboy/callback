package com.callbacksms.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val message: String,
    val templateName: String,
    val callType: Int,
    val sentAt: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val errorMessage: String? = null
)
