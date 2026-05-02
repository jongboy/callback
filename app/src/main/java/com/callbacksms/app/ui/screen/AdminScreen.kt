package com.callbacksms.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbacksms.app.auth.DeviceAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onLogout: () -> Unit, onUseApp: () -> Unit) {
    var licenses by remember { mutableStateOf<List<DeviceAuth.LicenseInfo>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    fun reload() {
        isLoading = true
        DeviceAuth.getAllLicenses { result ->
            licenses = result
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    // Firebase URL 미설정 안내
    if (!DeviceAuth.isConfigured()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Block, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text("Firebase URL 미설정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "DeviceAuth.kt의 FIREBASE_DB_URL을 채워주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUseApp) { Text("앱 사용하기") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onLogout) { Text("로그아웃") }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("라이선스 관리") },
                actions = {
                    IconButton(onClick = { reload() }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, "새로고침")
                    }
                    IconButton(onClick = onUseApp) {
                        Icon(Icons.Default.PhoneAndroid, "앱 사용하기")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, "로그아웃")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "코드 추가")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                licenses == null -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("불러오기 실패", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { reload() }) { Text("다시 시도") }
                }
                licenses!!.isEmpty() -> Text(
                    "등록된 코드가 없습니다\n+ 버튼으로 추가하세요",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(licenses!!, key = { it.code }) { license ->
                        LicenseCard(
                            license = license,
                            onToggleActive = { active ->
                                DeviceAuth.setLicenseActive(license.code, active) { if (it) reload() }
                            },
                            onDelete = { deleteTarget = license.code }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLicenseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { code, maxDevices, note ->
                showAddDialog = false
                DeviceAuth.createLicense(code, maxDevices, note) { if (it) reload() }
            }
        )
    }

    deleteTarget?.let { code ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("삭제 확인") },
            text = { Text("'$code' 코드를 삭제하면 해당 기기들은 즉시 사용 불가 처리됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    DeviceAuth.deleteLicense(code) { if (it) reload() }
                    deleteTarget = null
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun LicenseCard(
    license: DeviceAuth.LicenseInfo,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        license.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (license.note.isNotBlank()) {
                        Text(
                            license.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = license.active, onCheckedChange = onToggleActive)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "기기 ${license.deviceCount} / ${license.maxDevices}대",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (license.deviceCount >= license.maxDevices)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddLicenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, maxDevices: Int, note: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var maxDevicesText by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("코드 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().trim() },
                    label = { Text("코드") },
                    placeholder = { Text("예: ABC1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                )
                OutlinedTextField(
                    value = maxDevicesText,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) maxDevicesText = it },
                    label = { Text("최대 기기 수") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("메모 (선택)") },
                    placeholder = { Text("예: 홍길동") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val devices = maxDevicesText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    onConfirm(code, devices, note.trim())
                },
                enabled = code.isNotBlank()
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
