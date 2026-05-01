package com.callbacksms.app.data.dao

import androidx.room.*
import com.callbacksms.app.data.model.MessageTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY isDefault DESC, createdAt ASC")
    fun getAllTemplates(): Flow<List<MessageTemplate>>

    @Query("SELECT * FROM templates WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultTemplate(): MessageTemplate?

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: Long): MessageTemplate?

    @Insert
    suspend fun insert(template: MessageTemplate): Long

    @Update
    suspend fun update(template: MessageTemplate)

    @Delete
    suspend fun delete(template: MessageTemplate)

    @Query("UPDATE templates SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE templates SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)

    @Transaction
    suspend fun setDefaultTemplate(id: Long) {
        clearDefaultFlag()
        setDefault(id)
    }

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun getCount(): Int
}
