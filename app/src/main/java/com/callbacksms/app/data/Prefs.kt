package com.callbacksms.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val serviceEnabled: Boolean = false,
    val activeTemplateId: Long = -1L,
    val triggerOutgoing: Boolean = true,
    val triggerMissed: Boolean = false,
    val activeHoursEnabled: Boolean = false,
    val activeHoursStart: Int = 9,
    val activeHoursEnd: Int = 22,
    val minCallDuration: Int = 0
)

class Prefs(private val context: Context) {

    companion object {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val ACTIVE_TEMPLATE_ID = longPreferencesKey("active_template_id")
        val TRIGGER_OUTGOING = booleanPreferencesKey("trigger_outgoing")
        val TRIGGER_MISSED = booleanPreferencesKey("trigger_missed")
        val ACTIVE_HOURS_ENABLED = booleanPreferencesKey("active_hours_enabled")
        val ACTIVE_HOURS_START = intPreferencesKey("active_hours_start")
        val ACTIVE_HOURS_END = intPreferencesKey("active_hours_end")
        val MIN_CALL_DURATION = intPreferencesKey("min_call_duration")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            AppSettings(
                serviceEnabled = prefs[SERVICE_ENABLED] ?: false,
                activeTemplateId = prefs[ACTIVE_TEMPLATE_ID] ?: -1L,
                triggerOutgoing = prefs[TRIGGER_OUTGOING] ?: true,
                triggerMissed = prefs[TRIGGER_MISSED] ?: false,
                activeHoursEnabled = prefs[ACTIVE_HOURS_ENABLED] ?: false,
                activeHoursStart = prefs[ACTIVE_HOURS_START] ?: 9,
                activeHoursEnd = prefs[ACTIVE_HOURS_END] ?: 22,
                minCallDuration = prefs[MIN_CALL_DURATION] ?: 0
            )
        }

    suspend fun setServiceEnabled(v: Boolean) = context.dataStore.edit { it[SERVICE_ENABLED] = v }
    suspend fun setActiveTemplateId(v: Long) = context.dataStore.edit { it[ACTIVE_TEMPLATE_ID] = v }
    suspend fun setTriggerOutgoing(v: Boolean) = context.dataStore.edit { it[TRIGGER_OUTGOING] = v }
    suspend fun setTriggerMissed(v: Boolean) = context.dataStore.edit { it[TRIGGER_MISSED] = v }
    suspend fun setActiveHoursEnabled(v: Boolean) = context.dataStore.edit { it[ACTIVE_HOURS_ENABLED] = v }
    suspend fun setActiveHours(start: Int, end: Int) = context.dataStore.edit {
        it[ACTIVE_HOURS_START] = start; it[ACTIVE_HOURS_END] = end
    }
    suspend fun setMinCallDuration(v: Int) = context.dataStore.edit { it[MIN_CALL_DURATION] = v }
}
