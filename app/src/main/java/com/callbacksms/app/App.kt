package com.callbacksms.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.callbacksms.app.data.AppDatabase

class App : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_SERVICE, "콜백 모니터링", NotificationManager.IMPORTANCE_LOW).apply {
                description = "전화 통화 상태를 모니터링합니다"
            },
            NotificationChannel(CHANNEL_SMS, "문자 전송 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "자동 문자 전송 결과를 알립니다"
            }
        ))
    }

    companion object {
        const val CHANNEL_SERVICE = "callback_service"
        const val CHANNEL_SMS = "sms_notification"
    }
}
