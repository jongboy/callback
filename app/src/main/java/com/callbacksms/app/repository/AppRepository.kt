package com.callbacksms.app.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import com.callbacksms.app.data.AppDatabase
import com.callbacksms.app.data.Prefs
import com.callbacksms.app.data.model.MessageTemplate
import com.callbacksms.app.service.CallMonitorService

class AppRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val prefs = Prefs(context)

    val templates = db.templateDao().getAllTemplates()
    val smsLogs = db.smsLogDao().getAllLogs()
    val settings = prefs.settingsFlow

    suspend fun addTemplate(name: String, content: String) =
        db.templateDao().insert(MessageTemplate(name = name, content = content))

    suspend fun updateTemplate(t: MessageTemplate) = db.templateDao().update(t)
    suspend fun deleteTemplate(t: MessageTemplate) = db.templateDao().delete(t)
    suspend fun setDefaultTemplate(id: Long) = db.templateDao().setDefaultTemplate(id)
    suspend fun clearAllLogs() = db.smsLogDao().deleteAll()

    suspend fun setServiceEnabled(enabled: Boolean) {
        prefs.setServiceEnabled(enabled)
        if (enabled) startService() else stopService()
    }

    suspend fun setActiveTemplateId(id: Long) = prefs.setActiveTemplateId(id)
    suspend fun setTriggerOutgoing(v: Boolean) = prefs.setTriggerOutgoing(v)
    suspend fun setTriggerMissed(v: Boolean) = prefs.setTriggerMissed(v)
    suspend fun setActiveHoursEnabled(v: Boolean) = prefs.setActiveHoursEnabled(v)
    suspend fun setActiveHours(start: Int, end: Int) = prefs.setActiveHours(start, end)
    suspend fun setMinCallDuration(v: Int) = prefs.setMinCallDuration(v)

    private fun startService() {
        val intent = Intent(context, CallMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService() {
        context.stopService(Intent(context, CallMonitorService::class.java))
    }
}
