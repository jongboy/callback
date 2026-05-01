package com.callbacksms.app.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbacksms.app.data.model.MessageTemplate
import com.callbacksms.app.service.CallMonitorService
import com.callbacksms.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(viewModel: MainViewModel, paddingValues: PaddingValues) {
    val state by viewModel.state.collectAsState()
    val settings = state.settings
    val templates = state.templates
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<MessageTemplate?>(null) }
    var deleteTarget by remember { mutableStateOf<MessageTemplate?>(null) }
    var pickingTypeFor by remember { mutableStateOf<String?>(null) }

    fun templateById(id: Long) = templates.find { it.id == id }
    fun defaultTemplate() = templates.find { it.isDefault } ?: templates.firstOrNull()

    // 타입별 설정 데이터
    data class TypeConfig(
        val key: String,
        val icon: ImageVector,
        val title: String,
        val subtitle: String,
        val enabled: Boolean,
        val templateId: Long
    )
    val typeConfigs = listOf(
        TypeConfig("outgoing", Icons.Default.CallMade, "내가 건 전화 후",
            "통화가 끝난 후 보낼 메시지", settings.triggerOutgoing, settings.outgoingTemplateId),
        TypeConfig("outgoingMissed", Icons.Default.CallEnd, "내가 건 전화, 상대방이 못 받음",
            "상대방이 안 받았을 때 보낼 메시지", settings.triggerOutgoingMissed, settings.outgoingMissedTemplateId),
        TypeConfig("missed", Icons.Default.PhoneMissed, "상대방이 걸었는데 내가 못 받음",
            "내가 못 받았을 때 보낼 메시지", settings.triggerMissed, settings.missedTemplateId),
        TypeConfig("incoming", Icons.Default.CallReceived, "상대방이 건 전화 후",
            "통화가 끝난 후 보낼 메시지", settings.triggerIncoming, settings.incomingTemplateId)
    )

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(title = { Text("메시지 형식", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("새 메시지") }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 전화 유형별 메시지 설정 ──
            item {
                Text("전화 유형별 메시지",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text("유형마다 다른 메시지를 보낼 수 있어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }

            items(typeConfigs, key = { it.key }) { cfg ->
                val assignedTemplate = templateById(cfg.templateId) ?: defaultTemplate()
                TypeCard(
                    icon = cfg.icon,
                    title = cfg.title,
                    subtitle = cfg.subtitle,
                    enabled = cfg.enabled,
                    template = assignedTemplate,
                    onChangeTapped = { pickingTypeFor = cfg.key }
                )
            }

            // ── 메시지 목록 ──
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("메시지 목록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text("💡 {이름} {시간} {날짜} 를 넣으면 자동으로 바뀌어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }

            if (templates.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Message, null, Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("아직 메시지가 없어요",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text("아래 '새 메시지' 버튼을 눌러 추가해보세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onEdit = { editingTemplate = template },
                    onDelete = { deleteTarget = template }
                )
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    // 타입별 메시지 선택
    pickingTypeFor?.let { typeKey ->
        val currentId = when (typeKey) {
            "outgoing" -> settings.outgoingTemplateId
            "outgoingMissed" -> settings.outgoingMissedTemplateId
            "missed" -> settings.missedTemplateId
            else -> settings.incomingTemplateId
        }
        val dialogTitle = when (typeKey) {
            "outgoing" -> "내가 건 전화 후 — 메시지 선택"
            "outgoingMissed" -> "상대방이 못 받았을 때 — 메시지 선택"
            "missed" -> "내가 못 받았을 때 — 메시지 선택"
            else -> "상대방이 건 전화 후 — 메시지 선택"
        }
        TemplatePickerDialog(
            title = dialogTitle,
            templates = templates,
            currentId = currentId,
            onPick = { id ->
                when (typeKey) {
                    "outgoing" -> viewModel.setOutgoingTemplateId(id)
                    "outgoingMissed" -> viewModel.setOutgoingMissedTemplateId(id)
                    "missed" -> viewModel.setMissedTemplateId(id)
                    else -> viewModel.setIncomingTemplateId(id)
                }
                pickingTypeFor = null
            },
            onDismiss = { pickingTypeFor = null }
        )
    }

    if (showAddDialog) {
        TemplateDialog(
            title = "새 메시지 만들기",
            initial = MessageTemplate(name = "", content = ""),
            onConfirm = { name, content, imageUri ->
                viewModel.addTemplate(name, content, imageUri)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
            viewModel = viewModel
        )
    }

    editingTemplate?.let { t ->
        TemplateDialog(
            title = "메시지 수정하기",
            initial = t,
            onConfirm = { name, content, imageUri ->
                viewModel.updateTemplate(t.copy(name = name, content = content, imageUri = imageUri))
                editingTemplate = null
            },
            onDismiss = { editingTemplate = null },
            viewModel = viewModel
        )
    }

    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("정말 삭제할까요?") },
            text = { Text("'${t.name}' 메시지를 삭제하면 복구할 수 없어요.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteTemplate(t); deleteTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제하기") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } }
        )
    }
}

@Composable
private fun TypeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    template: MessageTemplate?,
    onChangeTapped: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(22.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!enabled) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        ) {
                            Text("꺼짐", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(
                    if (template != null) template.name
                    else "메시지를 선택해주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (template != null && template.content.isNotBlank()) {
                    Text(
                        template.content.take(38) + if (template.content.length > 38) "…" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onChangeTapped,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("변경", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TemplatePickerDialog(
    title: String,
    templates: List<MessageTemplate>,
    currentId: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            if (templates.isEmpty()) {
                Text("메시지가 없어요. 먼저 메시지를 추가해주세요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { t ->
                        val isSelected = t.id == currentId
                        Card(
                            onClick = { onPick(t.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.name, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(t.content.take(35) + if (t.content.length > 35) "…" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}

@Composable
private fun TemplateCard(
    template: MessageTemplate,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(template.name, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                if (template.isDefault) {
                    Surface(shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("기본", Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                if (template.imageUri != null) {
                    Surface(shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, null, Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(3.dp))
                            Text("사진", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(template.content, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("수정")
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("삭제")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDialog(
    title: String,
    initial: MessageTemplate,
    onConfirm: (String, String, String?) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MainViewModel
) {
    var name by remember { mutableStateOf(initial.name) }
    var content by remember { mutableStateOf(initial.content) }
    var selectedImagePath by remember { mutableStateOf(initial.imageUri) }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedImagePath = viewModel.copyImageToInternal(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("메시지 이름") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("보낼 내용") },
                    minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("{이름} {시간} {날짜} 자동 치환 가능") }
                )
                if (selectedImagePath != null) {
                    Card(shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val bitmap = remember(selectedImagePath) {
                                selectedImagePath?.let {
                                    try { android.graphics.BitmapFactory.decodeFile(it)?.asImageBitmap() }
                                    catch (e: Exception) { null }
                                }
                            }
                            if (bitmap != null) {
                                Image(bitmap, null,
                                    Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Image, null, Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("사진 첨부됨",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                                Text("사진과 함께 전송돼요 (MMS)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { selectedImagePath = null }) {
                                Icon(Icons.Default.Close, null,
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("사진 첨부하기 (선택)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && content.isNotBlank()) onConfirm(name.trim(), content.trim(), selectedImagePath) },
                enabled = name.isNotBlank() && content.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("저장하기") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
