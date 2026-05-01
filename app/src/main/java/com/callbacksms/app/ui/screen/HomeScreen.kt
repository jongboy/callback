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
    val activeTemplate = if (settings.activeTemplateId > 0)
        state.templates.find { it.id == settings.activeTemplateId }
    else
        state.templates.find { it.isDefault } ?: state.templates.firstOrNull()

    // Pulse animation (active state only)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Title
        Text(
            "콜백 SMS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "전화 후 자동 문자 전송",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(44.dp))

        // ── 메인 원형 버튼 ──
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            // 활성화 시 맥박 효과
            if (isEnabled) {
                Box(
                    Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                )
            }

            val circleColor = when {
                !hasPerms -> Color(0xFFE65100)       // 주황 - 권한 필요
                !isEnabled -> MaterialTheme.colorScheme.surfaceVariant  // 회색 - 준비됨
                else -> MaterialTheme.colorScheme.primary               // 파랑/초록 - 실행 중
            }
            val contentColor = when {
                !hasPerms -> Color.White
                !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onPrimary
            }
            val circleIcon = when {
                !hasPerms -> Icons.Default.Lock
                !isEnabled -> Icons.Default.TouchApp
                else -> Icons.Default.CheckCircle
            }
            val circleLabel = when {
                !hasPerms -> "권한\n설정 필요"
                !isEnabled -> "탭하여\n시작"
                else -> "실행 중"
            }

            Button(
                onClick = {
                    when {
                        !hasPerms -> onRequestPermissions()
                        !isEnabled -> viewModel.setServiceEnabled(true)
                        else -> viewModel.setServiceEnabled(false)
                    }
                },
                modifier = Modifier.size(190.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = circleColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = circleIcon,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = contentColor
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = circleLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 원 아래 안내 문구
        Text(
            text = when {
                !hasPerms -> "앱 사용을 위해 권한이 필요해요\n버튼을 탭하면 설정할 수 있어요"
                !isEnabled -> "모든 준비가 완료됐어요!\n버튼을 한번 더 탭하면 시작됩니다"
                else -> "실행 중 · 전화가 끊기면 자동으로 문자를 보냅니다"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(32.dp))

        // ── 상태별 하단 콘텐츠 ──
        when {
            !hasPerms -> PermissionGuideSection()
            !isEnabled -> ReadySection(activeTemplate?.name, activeTemplate?.content)
            else -> ActiveSection(
                todayCount = todayCount,
                totalCount = totalCount,
                activeTemplateName = activeTemplate?.name,
                activeTemplateContent = activeTemplate?.content,
                triggerOutgoing = settings.triggerOutgoing,
                triggerMissed = settings.triggerMissed,
                onToggleOutgoing = { viewModel.setTriggerOutgoing(!settings.triggerOutgoing) },
                onToggleMissed = { viewModel.setTriggerMissed(!settings.triggerMissed) }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── 1단계: 권한 안내 ──
@Composable
private fun PermissionGuideSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "필요한 권한",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        PermissionItem(
            icon = Icons.Default.Phone,
            title = "전화 상태 읽기",
            desc = "전화가 끊기는 순간을 감지해요"
        )
        PermissionItem(
            icon = Icons.Default.History,
            title = "통화 기록 읽기",
            desc = "누구에게 전화했는지 확인해요"
        )
        PermissionItem(
            icon = Icons.Default.Sms,
            title = "문자 보내기",
            desc = "상대방에게 자동으로 문자를 보내요"
        )
        PermissionItem(
            icon = Icons.Default.Notifications,
            title = "알림",
            desc = "문자 전송 결과를 알려드려요"
        )

        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "모든 권한은 자동 문자 전송에만 사용되며\n외부로 전송되지 않아요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(icon: ImageVector, title: String, desc: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── 2단계: 시작 준비 완료 ──
@Composable
private fun ReadySection(templateName: String?, templateContent: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Message, null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("전송할 메시지 미리보기",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    templateName ?: "기본 메시지",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (templateContent != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        templateContent.take(60) + if (templateContent.length > 60) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Text(
            "💡  '메시지 형식' 탭에서 보낼 메시지를 바꿀 수 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ── 3단계: 실행 중 ──
@Composable
private fun ActiveSection(
    todayCount: Int,
    totalCount: Int,
    activeTemplateName: String?,
    activeTemplateContent: String?,
    triggerOutgoing: Boolean,
    triggerMissed: Boolean,
    onToggleOutgoing: () -> Unit,
    onToggleMissed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 전송 통계
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "오늘 전송",
                value = "$todayCount 건",
                icon = Icons.Default.Send,
                color = MaterialTheme.colorScheme.primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "누적 전송",
                value = "$totalCount 건",
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // 현재 메시지
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Message, null,
                    tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("전송 중인 메시지",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        activeTemplateName ?: "기본 메시지",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (activeTemplateContent != null) {
                        Text(
                            activeTemplateContent.take(50) + if (activeTemplateContent.length > 50) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // 작동 모드
        Text("작동 모드",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = triggerOutgoing,
                onClick = onToggleOutgoing,
                label = { Text("발신 후 문자") },
                leadingIcon = { Icon(Icons.Default.CallMade, null, Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = triggerMissed,
                onClick = onToggleMissed,
                label = { Text("부재중 답장") },
                leadingIcon = { Icon(Icons.Default.PhoneMissed, null, Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
