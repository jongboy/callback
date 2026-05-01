package com.callbacksms.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbacksms.app.viewmodel.MainViewModel
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    paddingValues: PaddingValues
) {
    val state by viewModel.state.collectAsState()
    val settings = state.settings
    val isEnabled = settings.serviceEnabled
    val hasPerms = state.hasPermissions

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayCount  = state.smsLogs.count { it.sentAt >= todayStart && it.success }
    val totalCount  = state.smsLogs.count { it.success }
    val activeTemplate = if (settings.activeTemplateId > 0)
        state.templates.find { it.id == settings.activeTemplateId }
    else
        state.templates.find { it.isDefault } ?: state.templates.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text("콜백 SMS", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("전화 종료 후 자동 문자 전송", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))

        // Power button
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    if (isEnabled)
                        Brush.radialGradient(listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        ))
                    else
                        Brush.radialGradient(listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        ))
                )
        ) {
            IconButton(
                onClick = {
                    if (!hasPerms) onRequestPermissions()
                    else viewModel.setServiceEnabled(!isEnabled)
                },
                modifier = Modifier.size(160.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.PhoneInTalk else Icons.Default.PhoneDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isEnabled) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isEnabled) "ON" else "OFF",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                !hasPerms  -> "권한 허용 필요 — 버튼을 눌러 설정하세요"
                isEnabled  -> "실행 중 • 전화 종료 시 자동 문자 전송"
                else       -> "비활성화됨 • 버튼을 눌러 시작하세요"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // Stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), "오늘 전송", "$todayCount", "건", Icons.Default.Send)
            StatCard(Modifier.weight(1f), "총 전송", "$totalCount", "건", Icons.Default.CheckCircle)
        }

        Spacer(Modifier.height(16.dp))

        // Active template
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Message, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("현재 템플릿", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(activeTemplate?.name ?: "템플릿 없음",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    activeTemplate?.let {
                        Text(
                            it.content.take(50) + if (it.content.length > 50) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Trigger mode chips
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.triggerOutgoing,
                onClick = { viewModel.setTriggerOutgoing(!settings.triggerOutgoing) },
                label = { Text("발신 후 문자") },
                leadingIcon = { Icon(Icons.Default.CallMade, null, Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = settings.triggerMissed,
                onClick = { viewModel.setTriggerMissed(!settings.triggerMissed) },
                label = { Text("부재중 답장") },
                leadingIcon = { Icon(Icons.Default.PhoneMissed, null, Modifier.size(16.dp)) }
            )
        }

        // Permission warning
        if (!hasPerms) {
            Spacer(Modifier.height(16.dp))
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("권한이 필요합니다", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("전화 상태, 통화 기록, 문자 발송 권한을 허용해주세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String, unit: String, icon: ImageVector) {
    Card(modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(unit, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp))
            }
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
