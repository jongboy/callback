package com.callbacksms.app.data.dao

import androidx.room.*
import com.callbacksms.app.data.model.ExcludedNumber
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedNumberDao {
    @Query("SELECT * FROM excluded_numbers ORDER BY addedAt DESC")
    fun getAll(): Flow<List<ExcludedNumber>>

    @Query("SELECT COUNT(*) FROM excluded_numbers WHERE phoneNumber = :number")
    suspend fun isExcluded(number: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: ExcludedNumber)

    @Delete
    suspend fun delete(entry: ExcludedNumber)

    @Query("DELETE FROM excluded_numbers")
    suspend fun deleteAll()
}
