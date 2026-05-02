package com.callbacksms.app.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.callbacksms.app.data.AppDatabase
import com.callbacksms.app.data.Prefs
import com.callbacksms.app.data.model.ExcludedNumber
import com.callbacksms.app.data.model.MessageTemplate
import com.callbacksms.app.service.CallMonitorService
import java.io.File

class AppRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val prefs = Prefs(context)

    val templates = db.templateDao().getAllTemplates()
    val smsLogs = db.smsLogDao().getAllLogs()
    val settings = prefs.settingsFlow
    val excludedNumbers = db.excludedNumberDao().getAll()

    suspend fun addTemplate(name: String, content: String, imageUri: String? = null) =
        db.templateDao().insert(MessageTemplate(name = name, content = content, imageUri = imageUri))

    suspend fun updateTemplate(t: MessageTemplate) = db.templateDao().update(t)
    suspend fun deleteTemplate(t: MessageTemplate) {
        t.imageUri?.let { File(it).delete() }
        db.templateDao().delete(t)
    }
    suspend fun setDefaultTemplate(id: Long) = db.templateDao().setDefaultTemplate(id)
    suspend fun clearAllLogs() = db.smsLogDao().deleteAll()

    suspend fun addExcludedNumber(number: String, name: String?) =
        db.excludedNumberDao().insert(ExcludedNumber(phoneNumber = number, contactName = name))
    suspend fun removeExcludedNumber(entry: ExcludedNumber) = db.excludedNumberDao().delete(entry)
    suspend fun isNumberExcluded(number: String): Boolean =
        db.excludedNumberDao().isExcluded(number) > 0

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
    suspend fun setOnlySendTo010(v: Boolean) = prefs.setOnlySendTo010(v)
    suspend fun setTriggerOutgoingMissed(v: Boolean) = prefs.setTriggerOutgoingMissed(v)
    suspend fun setTriggerIncoming(v: Boolean) = prefs.setTriggerIncoming(v)
    suspend fun setOutgoingTemplateId(id: Long) = prefs.setOutgoingTemplateId(id)
    suspend fun setOutgoingMissedTemplateId(id: Long) = prefs.setOutgoingMissedTemplateId(id)
    suspend fun setMissedTemplateId(id: Long) = prefs.setMissedTemplateId(id)
    suspend fun setIncomingTemplateId(id: Long) = prefs.setIncomingTemplateId(id)

    fun copyImageToInternal(uri: Uri): String? {
        return try {
            val ext = when (context.contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val file = File(context.filesDir, "template_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            file.absolutePath
        } catch (e: Exception) { null }
    }

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
