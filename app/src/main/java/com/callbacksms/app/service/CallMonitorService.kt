package com.callbacksms.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callbacksms.app.App
import com.callbacksms.app.MainActivity
import com.callbacksms.app.R
import com.callbacksms.app.data.AppDatabase
import com.callbacksms.app.data.AppSettings
import com.callbacksms.app.data.Prefs
import com.callbacksms.app.data.model.SmsLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class CallMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var prefs: Prefs
    private lateinit var database: AppDatabase

    private var previousState = TelephonyManager.CALL_STATE_IDLE
    private var isOutgoingCall = false
    private var callStartTime = 0L

    @RequiresApi(Build.VERSION_CODES.S)
    private val telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) = handleStateChange(state)
    }

    @Suppress("DEPRECATION")
    private val legacyListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) = handleStateChange(state)
    }

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        prefs = Prefs(applicationContext)
        database = (application as App).database
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_SERVICE, buildServiceNotification())
        registerListener()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterListener()
        scope.cancel()
        super.onDestroy()
    }

    private fun registerListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback)
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun unregisterListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback)
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
        }
    }

    private fun handleStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isOutgoingCall = false
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (previousState == TelephonyManager.CALL_STATE_IDLE) {
                    isOutgoingCall = true
                    callStartTime = System.currentTimeMillis()
                } else if (previousState == TelephonyManager.CALL_STATE_RINGING) {
                    isOutgoingCall = false
                    callStartTime = System.currentTimeMillis()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                val wasInCall = previousState == TelephonyManager.CALL_STATE_OFFHOOK
                val wasMissed = previousState == TelephonyManager.CALL_STATE_RINGING
                val duration = if (callStartTime > 0)
                    ((System.currentTimeMillis() - callStartTime) / 1000).toInt() else 0
                callStartTime = 0L

                if (wasInCall && isOutgoingCall) {
                    scope.launch {
                        delay(800)
                        handleOutgoingCallEnded(duration)
                    }
                } else if (wasMissed) {
                    scope.launch {
                        delay(800)
                        handleMissedCall()
                    }
                }
            }
        }
        previousState = state
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleOutgoingCallEnded(duration: Int) {
        val settings = prefs.settingsFlow.first()
        if (!settings.triggerOutgoing) return
        if (!isWithinActiveHours(settings)) return
        if (duration < settings.minCallDuration) return

        val (number, name) = getLastCallEntry(CallLog.Calls.OUTGOING_TYPE) ?: return
        sendSms(number, name, CallLog.Calls.OUTGOING_TYPE, settings)
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleMissedCall() {
        val settings = prefs.settingsFlow.first()
        if (!settings.triggerMissed) return
        if (!isWithinActiveHours(settings)) return

        val (number, name) = getLastCallEntry(CallLog.Calls.MISSED_TYPE) ?: return
        sendSms(number, name, CallLog.Calls.MISSED_TYPE, settings)
    }

    @SuppressLint("MissingPermission")
    private fun getLastCallEntry(type: Int): Pair<String, String?>? {
        return try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE),
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(type.toString()),
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val number = cursor.getString(0) ?: return null
                    Pair(number, getContactName(number))
                } else null
            }
        } catch (e: Exception) { null }
    }

    private fun getContactName(phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) { null }
    }

    private suspend fun sendSms(
        phoneNumber: String,
        contactName: String?,
        callType: Int,
        settings: AppSettings
    ) {
        val template = if (settings.activeTemplateId > 0) {
            database.templateDao().getById(settings.activeTemplateId)
        } else {
            database.templateDao().getDefaultTemplate()
        } ?: return

        val now = Date()
        val message = template.content
            .replace("{이름}", contactName ?: phoneNumber)
            .replace("{name}", contactName ?: phoneNumber)
            .replace("{시간}", SimpleDateFormat("HH:mm", Locale.KOREAN).format(now))
            .replace("{날짜}", SimpleDateFormat("M월 d일", Locale.KOREAN).format(now))

        var success = true
        var errorMsg: String? = null

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
        } catch (e: Exception) {
            success = false
            errorMsg = e.message
        }

        database.smsLogDao().insert(
            SmsLog(
                phoneNumber = phoneNumber,
                contactName = contactName,
                message = message,
                templateName = template.name,
                callType = callType,
                success = success,
                errorMessage = errorMsg
            )
        )
        database.smsLogDao().trimOldLogs()
        showSmsNotification(contactName ?: phoneNumber, message, success)
    }

    private fun showSmsNotification(recipient: String, message: String, success: Boolean) {
        val notif = NotificationCompat.Builder(this, App.CHANNEL_SMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (success) "✓ 문자 전송 완료 → $recipient" else "✗ 문자 전송 실패")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_SMS, notif)
        } catch (_: SecurityException) {}
    }

    private fun isWithinActiveHours(settings: AppSettings): Boolean {
        if (!settings.activeHoursEnabled) return true
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (settings.activeHoursStart <= settings.activeHoursEnd) {
            hour in settings.activeHoursStart..settings.activeHoursEnd
        } else {
            hour >= settings.activeHoursStart || hour <= settings.activeHoursEnd
        }
    }

    private fun buildServiceNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, App.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("콜백 SMS 실행 중")
            .setContentText("전화 종료 시 자동으로 문자를 전송합니다")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIF_SERVICE = 1001
        private const val NOTIF_SMS = 1002
    }
}
