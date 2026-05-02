package com.callbacksms.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excluded_numbers")
data class ExcludedNumber(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val addedAt: Long = System.currentTimeMillis()
)
