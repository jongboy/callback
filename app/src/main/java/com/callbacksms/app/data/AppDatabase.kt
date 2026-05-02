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
    version = 3,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callback_sms_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
            dao.insert(MessageTemplate(
                name = "간단 메시지",
                content = "방금 전화했어요. 나중에 연락 주세요!",
                isDefault = false
            ))
            dao.insert(MessageTemplate(
                name = "이름 포함",
                content = "{이름}님, 안녕하세요. 잠시 전 전화드렸으나 연결이 안 되어 문자 남깁니다. 편하신 시간에 회신 부탁드립니다.",
                isDefault = false
            ))
            dao.insert(MessageTemplate(
                name = "부재중 답장",
                content = "전화 주셨군요! {시간}에 확인했습니다. 곧 다시 연락드릴게요.",
                isDefault = false
            ))
        }
    }
}
