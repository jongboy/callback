package com.callbacksms.app.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callbacksms.app.data.AppSettings
import com.callbacksms.app.data.model.MessageTemplate
import com.callbacksms.app.data.model.SmsLog
import com.callbacksms.app.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val settings: AppSettings = AppSettings(),
    val templates: List<MessageTemplate> = emptyList(),
    val smsLogs: List<SmsLog> = emptyList(),
    val hasPermissions: Boolean = false,
    val isLoading: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppRepository(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.settings, repo.templates, repo.smsLogs) { s, t, l ->
                UiState(
                    settings = s,
                    templates = t,
                    smsLogs = l,
                    hasPermissions = checkPermissions(),
                    isLoading = false
                )
            }.collect { _state.value = it }
        }
    }

    fun checkPermissions(): Boolean {
        val ctx = getApplication<Application>()
        val required = buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.SEND_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        return required.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
    }

    fun onPermissionsResult(permissions: Map<String, Boolean>) {
        val granted = checkPermissions()
        _state.update { it.copy(hasPermissions = granted) }
        if (granted && _state.value.settings.serviceEnabled) {
            viewModelScope.launch { repo.setServiceEnabled(true) }
        }
    }

    fun setServiceEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.setServiceEnabled(enabled)
    }

    fun setActiveTemplate(id: Long) = viewModelScope.launch { repo.setActiveTemplateId(id) }
    fun addTemplate(name: String, content: String) = viewModelScope.launch { repo.addTemplate(name, content) }
    fun updateTemplate(t: MessageTemplate) = viewModelScope.launch { repo.updateTemplate(t) }
    fun deleteTemplate(t: MessageTemplate) = viewModelScope.launch { repo.deleteTemplate(t) }
    fun setTriggerOutgoing(v: Boolean) = viewModelScope.launch { repo.setTriggerOutgoing(v) }
    fun setTriggerMissed(v: Boolean) = viewModelScope.launch { repo.setTriggerMissed(v) }
    fun setActiveHoursEnabled(v: Boolean) = viewModelScope.launch { repo.setActiveHoursEnabled(v) }
    fun setActiveHours(start: Int, end: Int) = viewModelScope.launch { repo.setActiveHours(start, end) }
    fun setMinCallDuration(v: Int) = viewModelScope.launch { repo.setMinCallDuration(v) }
    fun clearAllLogs() = viewModelScope.launch { repo.clearAllLogs() }
}
