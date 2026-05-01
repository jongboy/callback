package com.callbacksms.app.data.dao

import androidx.room.*
import com.callbacksms.app.data.model.SmsLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY sentAt DESC")
    fun getAllLogs(): Flow<List<SmsLog>>

    @Insert
    suspend fun insert(log: SmsLog)

    @Query("DELETE FROM sms_logs WHERE id NOT IN (SELECT id FROM sms_logs ORDER BY sentAt DESC LIMIT 300)")
    suspend fun trimOldLogs()

    @Query("DELETE FROM sms_logs")
    suspend fun deleteAll()
}
