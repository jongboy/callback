package com.callbacksms.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.callbacksms.app.data.dao.ExcludedNumberDao
import com.callbacksms.app.data.dao.SmsLogDao
import com.callbacksms.app.data.dao.TemplateDao
import com.callbacksms.app.data.model.ExcludedNumber
import com.callbacksms.app.data.model.MessageTemplate
import com.callbacksms.app.data.model.SmsLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MessageTemplate::class, SmsLog::class, ExcludedNumber::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun excludedNumberDao(): ExcludedNumberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE templates ADD COLUMN imageUri TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS excluded_numbers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "phoneNumber TEXT NOT NULL, " +
                    "contactName TEXT, " +
                    "addedAt INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "DELETE FROM templates WHERE name IN ('간단 메시지', '이름 포함', '부재중 답장')"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callback_sms_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    if (instance.templateDao().getCount() == 0) {
                        insertDefaultTemplates(instance.templateDao())
                    }
                }
                instance
            }
        }

        private suspend fun insertDefaultTemplates(dao: TemplateDao) {
            dao.insert(MessageTemplate(
                name = "기본 메시지",
                content = "안녕하세요! 방금 전화를 드렸는데 연결이 되지 않았네요. 편하실 때 다시 연락 부탁드립니다 😊",
                isDefault = true
            ))
        }
    }
}
