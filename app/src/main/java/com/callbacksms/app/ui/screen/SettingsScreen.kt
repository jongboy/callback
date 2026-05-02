package com.callbacksms.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callbacksms.app.auth.DeviceAuth
import com.callbacksms.app.data.model.ExcludedNumber
import com.callbacksms.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, paddingValues: PaddingValues) {
    val state by viewModel.state.collectAsState()
    val settings = state.settings
    val excludedNumbers = state.excludedNumbers
    var showHourPicker by remember { mutableStateOf<String?>(null) }
    var minDurationInput by remember { mutableStateOf(settings.minCallDuration.toString()) }
    var showAddExclusionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(settings.minCallDuration) {
        minDurationInput = settings.minCallDuration.toString()
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = { TopAppBar(title = { Text("설정", fontWeight = FontWeight.Bold) }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── 언제 문자를 보낼까요?
            SectionHeader("언제 문자를 보낼까요?", Icons.Default.Tune)

            SettingSwitch(
                title = "010 번호에만 보내기",
                subtitle = "010으로 시작하는 번호에만 문자를 보내요",
                icon = Icons.Default.PhoneAndroid,
                checked = settings.onlySendTo010,
                onChecked = { viewModel.setOnlySendTo010(it) }
            )
            SettingSwitch(
                title = "내가 건 전화 후 (통화 완료)",
                subtitle = "통화가 끝난 후 문자를 보내요",
                icon = Icons.Default.CallMade,
                checked = settings.triggerOutgoing,
                onChecked = { viewModel.setTriggerOutgoing(it) }
            )
            SettingSwitch(
                title = "내가 건 전화, 상대방이 못 받음",
                subtitle = "상대방이 전화를 안 받았을 때 보내요",
                icon = Icons.Default.CallEnd,
                checked = settings.triggerOutgoingMissed,
                onChecked = { viewModel.setTriggerOutgoingMissed(it) }
            )
            SettingSwitch(
                title = "상대방이 걸었는데 내가 못 받음",
                subtitle = "내가 전화를 못 받았을 때 보내요",
                icon = Icons.Default.PhoneMissed,
                checked = settings.triggerMissed,
                onChecked = { viewModel.setTriggerMissed(it) }
            )
            SettingSwitch(
                title = "상대방이 건 전화 후 (통화 완료)",
                subtitle = "상대방이 건 통화가 끝난 후 문자를 보내요",
                icon = Icons.Default.CallReceived,
                checked = settings.triggerIncoming,
                onChecked = { viewModel.setTriggerIncoming(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 통화 시간 조건
            SectionHeader("통화 시간 조건", Icons.Default.Timer)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("이 시간보다 짧은 통화에는 문자를 보내지 않아요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = minDurationInput,
                            onValueChange = { v ->
                                minDurationInput = v
                                v.toIntOrNull()?.let { viewModel.setMinCallDuration(it.coerceAtLeast(0)) }
                            },
                            label = { Text("초") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when {
                                settings.minCallDuration == 0 -> "통화 길이 상관없이 문자를 보내요"
                                else -> "${settings.minCallDuration}초 이상 통화했을 때만 보내요"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 작동 시간 설정
            SectionHeader("작동 시간 설정", Icons.Default.Schedule)

            SettingSwitch(
                title = "정해진 시간에만 작동하기",
                subtitle = "이 시간대에만 자동으로 문자를 보내요",
                icon = Icons.Default.AccessTime,
                checked = settings.activeHoursEnabled,
                onChecked = { viewModel.setActiveHoursEnabled(it) }
            )

            if (settings.activeHoursEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("시작 시간", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                TextButton(onClick = { showHourPicker = "start" }) {
                                    Text("%02d:00".format(settings.activeHoursStart),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Icon(Icons.Default.ArrowForward, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("종료 시간", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                TextButton(onClick = { showHourPicker = "end" }) {
                                    Text("%02d:00".format(settings.activeHoursEnd),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Text(
                            text = buildString {
                                val s = settings.activeHoursStart
                                val e = settings.activeHoursEnd
                                append("오전/오후 ${if (s < 12) "${s}시" else "${s - 12}시"}")
                                append(" ~ ")
                                append("오전/오후 ${if (e < 12) "${e}시" else "${e - 12}시"}에만 전송")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 문자 제외 번호
            SectionHeader("문자 보내지 않을 번호", Icons.Default.Block)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "등록된 번호에서 전화가 오거나 걸어도 문자를 보내지 않아요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (excludedNumbers.isEmpty()) {
                        Text(
                            "제외된 번호 없음",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        excludedNumbers.forEach { entry ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Block, null, Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        entry.contactName ?: entry.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (entry.contactName != null) {
                                        Text(entry.phoneNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { viewModel.removeExcludedNumber(entry) }) {
                                    Icon(Icons.Default.Close, "삭제",
                                        Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { showAddExclusionDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("번호 추가")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 라이선스
            if (DeviceAuth.isConfigured()) {
                val context = LocalContext.current
                val licenseKey = remember { DeviceAuth.getStoredLicense(context) }

                SectionHeader("라이선스", Icons.Default.VpnKey)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow("코드", licenseKey ?: "미입력")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // ── 앱 정보
            SectionHeader("앱 정보", Icons.Default.Info)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoRow("버전", "1.0.0")
                    InfoRow("기능", "발신·부재중·수신 통화 후 자동 문자 전송")
                    InfoRow("저장 기록", "최근 300건 보관")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    showHourPicker?.let { mode ->
        val currentHour = if (mode == "start") settings.activeHoursStart else settings.activeHoursEnd
        HourPickerDialog(
            title = if (mode == "start") "시작 시간" else "종료 시간",
            currentHour = currentHour,
            onConfirm = { hour ->
                if (mode == "start") viewModel.setActiveHours(hour, settings.activeHoursEnd)
                else viewModel.setActiveHours(settings.activeHoursStart, hour)
                showHourPicker = null
            },
            onDismiss = { showHourPicker = null }
        )
    }

    if (showAddExclusionDialog) {
        AddExclusionDialog(
            onConfirm = { number, name ->
                viewModel.addExcludedNumber(number, name.ifBlank { null })
                showAddExclusionDialog = false
            },
            onDismiss = { showAddExclusionDialog = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HourPickerDialog(
    title: String,
    currentHour: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableIntStateOf(currentHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("${selected}시 (%02d:00)".format(selected),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = selected.toFloat(),
                    onValueChange = { selected = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0시", style = MaterialTheme.typography.labelSmall)
                    Text("12시", style = MaterialTheme.typography.labelSmall)
                    Text("23시", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
private fun AddExclusionDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("제외할 번호 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("전화번호") },
                    placeholder = { Text("010-0000-0000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (number.isNotBlank()) onConfirm(number.trim(), name.trim()) },
                enabled = number.isNotBlank()
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
