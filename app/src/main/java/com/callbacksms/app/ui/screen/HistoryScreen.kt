package com.callbacksms.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callbacksms.app.data.model.SmsLog
import com.callbacksms.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.provider.CallLog
import com.callbacksms.app.service.CallMonitorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel, paddingValues: PaddingValues) {
    val state by viewModel.state.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("전송 기록", fontWeight = FontWeight.Bold) },
                actions = {
                    if (state.smsLogs.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, "기록 삭제")
                        }
                    }
                }
            )
        }
    ) { inner ->
        if (state.smsLogs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("전송 기록이 없습니다", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("문자가 전송되면 여기에 기록됩니다", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // Group logs by date
            val grouped = state.smsLogs.groupBy { log ->
                SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN).format(Date(log.sentAt))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (date, logs) ->
                    item(key = "header_$date") {
                        Text(date, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(logs, key = { it.id }) { log ->
                        LogCard(log)
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("기록 전체 삭제") },
            text = { Text("모든 전송 기록을 삭제하시겠어요? 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAllLogs(); showClearDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("취소") }
            }
        )
    }
}

private fun callTypeIcon(callType: Int) = when (callType) {
    CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
    CallMonitorService.OUTGOING_MISSED_TYPE -> Icons.Default.CallEnd
    CallLog.Calls.MISSED_TYPE -> Icons.Default.PhoneMissed
    CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
    else -> Icons.Default.Phone
}

private fun callTypeLabel(callType: Int) = when (callType) {
    CallLog.Calls.OUTGOING_TYPE -> "내가 건 전화"
    CallMonitorService.OUTGOING_MISSED_TYPE -> "내가 건 전화 (상대 부재중)"
    CallLog.Calls.MISSED_TYPE -> "내가 못 받은 전화"
    CallLog.Calls.INCOMING_TYPE -> "상대방이 건 전화"
    else -> "전화"
}

@Composable
private fun LogCard(log: SmsLog) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.KOREAN).format(Date(log.sentAt))
    val iconBgColor = when (log.callType) {
        CallLog.Calls.OUTGOING_TYPE -> MaterialTheme.colorScheme.primaryContainer
        CallMonitorService.OUTGOING_MISSED_TYPE -> MaterialTheme.colorScheme.errorContainer
        CallLog.Calls.MISSED_TYPE -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val iconTint = when (log.callType) {
        CallLog.Calls.OUTGOING_TYPE -> MaterialTheme.colorScheme.primary
        CallMonitorService.OUTGOING_MISSED_TYPE -> MaterialTheme.colorScheme.error
        CallLog.Calls.MISSED_TYPE -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (log.success) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBgColor,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = callTypeIcon(log.callType),
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = iconTint
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.contactName ?: log.phoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(timeStr, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(callTypeLabel(log.callType),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (log.contactName != null) {
                    Text(log.phoneNumber, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(log.message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                // 전송 결과
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (log.success) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (log.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = if (log.success) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (log.success) {
                                if (!log.errorMessage.isNullOrEmpty()) "전송됨 (${log.errorMessage})"
                                else "전송됨 • ${log.templateName}"
                            } else "전송 실패: ${log.errorMessage ?: "알 수 없는 오류"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (log.success) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
