package com.callbacksms.app.ui.screen

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val todayCount = state.smsLogs.count { it.sentAt >= todayStart && it.success }
    val totalCount = state.smsLogs.count { it.success }

    val activeTemplate = run {
        val id = settings.outgoingTemplateId
        if (id > 0) state.templates.find { it.id == id }
        else state.templates.find { it.isDefault } ?: state.templates.firstOrNull()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.04f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))

        // ── 상단 제목 + 상태 뱃지 ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text("콜백 SMS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("전화 후 자동 문자 전송",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isEnabled && hasPerms) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF00C896).copy(alpha = 0.15f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(6.dp).clip(CircleShape)
                                .background(Color(0xFF00C896))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("작동 중", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = Color(0xFF00A87A))
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        // ── 메인 원형 버튼 ──
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
            if (isEnabled && hasPerms) {
                Box(
                    Modifier.size(200.dp).scale(pulseScale).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                )
            }
            val circleColor = when {
                !hasPerms -> Color(0xFFFF6B35)
                !isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.primary
            }
            val contentColor = when {
                !hasPerms -> Color.White
                !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onPrimary
            }
            Button(
                onClick = {
                    when {
                        !hasPerms -> onRequestPermissions()
                        !isEnabled -> viewModel.setServiceEnabled(true)
                        else -> viewModel.setServiceEnabled(false)
                    }
                },
                modifier = Modifier.size(185.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = circleColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when {
                            !hasPerms -> Icons.Default.Lock
                            !isEnabled -> Icons.Default.PowerSettingsNew
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = contentColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            !hasPerms -> "권한 설정"
                            !isEnabled -> "시작하기"
                            else -> "작동 중"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = when {
                !hasPerms -> "권한 설정이 필요해요"
                !isEnabled -> "버튼을 누르면 시작돼요"
                else -> "전화가 끊기면 바로 문자를 보내드려요"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ── 상태별 콘텐츠 ──
        when {
            !hasPerms -> PermissionSection(onRequestPermissions)
            !isEnabled -> ReadySection(activeTemplate?.name, activeTemplate?.content)
            else -> ActiveSection(
                todayCount = todayCount,
                totalCount = totalCount,
                activeTemplateName = activeTemplate?.name,
                activeTemplateContent = activeTemplate?.content,
                triggerOutgoing = settings.triggerOutgoing,
                triggerOutgoingMissed = settings.triggerOutgoingMissed,
                triggerMissed = settings.triggerMissed,
                triggerIncoming = settings.triggerIncoming,
                onToggleOutgoing = { viewModel.setTriggerOutgoing(!settings.triggerOutgoing) },
                onToggleOutgoingMissed = { viewModel.setTriggerOutgoingMissed(!settings.triggerOutgoingMissed) },
                onToggleMissed = { viewModel.setTriggerMissed(!settings.triggerMissed) },
                onToggleIncoming = { viewModel.setTriggerIncoming(!settings.triggerIncoming) }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── 1단계: 권한 필요 ──
@Composable
private fun PermissionSection(onRequestPermissions: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("이런 권한이 필요해요",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)

        PermItem(Icons.Default.Phone, "전화 상태 확인", "전화가 끊기는 순간을 감지해요")
        PermItem(Icons.Default.History, "통화 기록 확인", "누구에게 전화했는지 알 수 있어요")
        PermItem(Icons.Default.Sms, "문자 전송", "전화 후 자동으로 문자를 보내요")
        PermItem(Icons.Default.Notifications, "알림", "전송 결과를 알림으로 알려드려요")

        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("권한 정보는 자동 문자 전송에만 사용되며\n외부로 절대 보내지 않아요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("권한 설정하러 가기", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PermItem(icon: ImageVector, title: String, desc: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── 2단계: 준비 완료 ──
@Composable
private fun ReadySection(templateName: String?, templateContent: String?) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Message, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("이런 메시지가 전송돼요",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Text(templateName ?: "기본 메시지",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold)
                if (templateContent != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        templateContent.take(70) + if (templateContent.length > 70) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text("💡 '메시지' 탭에서 내용을 바꿀 수 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp))
    }
}

// ── 3단계: 작동 중 ──
@Composable
private fun ActiveSection(
    todayCount: Int,
    totalCount: Int,
    activeTemplateName: String?,
    activeTemplateContent: String?,
    triggerOutgoing: Boolean,
    triggerOutgoingMissed: Boolean,
    triggerMissed: Boolean,
    triggerIncoming: Boolean,
    onToggleOutgoing: () -> Unit,
    onToggleOutgoingMissed: () -> Unit,
    onToggleMissed: () -> Unit,
    onToggleIncoming: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 전송 통계
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), "오늘 보낸 문자", todayCount, Icons.Default.Send,
                MaterialTheme.colorScheme.primary)
            StatCard(Modifier.weight(1f), "지금까지 총", totalCount, Icons.Default.CheckCircle,
                MaterialTheme.colorScheme.tertiary)
        }

        // 현재 메시지
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Message, null, Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("지금 전송하는 메시지",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(activeTemplateName ?: "기본 메시지",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    if (activeTemplateContent != null) {
                        Text(
                            activeTemplateContent.take(45) + if (activeTemplateContent.length > 45) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 작동 조건 토글
        Text("어떤 경우에 문자를 보낼까요?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                TriggerRow(Icons.Default.CallMade, "내가 건 전화 후",
                    "통화가 끝난 후 문자를 보내요", triggerOutgoing, onToggleOutgoing)
                HorizontalDivider(Modifier.padding(start = 72.dp))
                TriggerRow(Icons.Default.CallEnd, "내가 걸었는데 상대방이 못 받음",
                    "상대방이 전화를 안 받았을 때 보내요", triggerOutgoingMissed, onToggleOutgoingMissed)
                HorizontalDivider(Modifier.padding(start = 72.dp))
                TriggerRow(Icons.Default.PhoneMissed, "상대방이 걸었는데 내가 못 받음",
                    "내가 전화를 못 받았을 때 보내요", triggerMissed, onToggleMissed)
                HorizontalDivider(Modifier.padding(start = 72.dp))
                TriggerRow(Icons.Default.CallReceived, "상대방이 건 전화 후",
                    "통화가 끝난 후 문자를 보내요", triggerIncoming, onToggleIncoming)
            }
        }
    }
}

@Composable
private fun TriggerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(
                    if (checked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp),
                tint = if (checked) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = { onChecked() })
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    count: Int,
    icon: ImageVector,
    tintColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = tintColor)
            Spacer(Modifier.height(10.dp))
            Text("$count 건",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
