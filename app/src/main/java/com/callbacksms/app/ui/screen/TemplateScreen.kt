package com.callbacksms.app.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import com.callbacksms.app.data.model.MessageTemplate
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

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(title = { Text("메시지 형식", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "새 메시지 만들기")
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 전화 종류별 메시지 설정 ──
            item {
                Text(
                    "전화 종류별 메시지 설정",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "전화 종류마다 다른 메시지를 보낼 수 있어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                TypeAssignmentCard(
                    icon = Icons.Default.CallMade,
                    callTypeLabel = "발신",
                    title = "내가 전화를 건 후",
                    template = templateById(settings.outgoingTemplateId) ?: defaultTemplate(),
                    enabled = settings.triggerOutgoing,
                    onChangeTapped = { pickingTypeFor = "outgoing" }
                )
            }

            item {
                TypeAssignmentCard(
                    icon = Icons.Default.PhoneMissed,
                    callTypeLabel = "부재중",
                    title = "상대방이 못 받았을 때",
                    template = templateById(settings.missedTemplateId) ?: defaultTemplate(),
                    enabled = settings.triggerMissed,
                    onChangeTapped = { pickingTypeFor = "missed" }
                )
            }

            item {
                TypeAssignmentCard(
                    icon = Icons.Default.CallReceived,
                    callTypeLabel = "수신",
                    title = "상대방이 나에게 전화한 후",
                    template = templateById(settings.incomingTemplateId) ?: defaultTemplate(),
                    enabled = settings.triggerIncoming,
                    onChangeTapped = { pickingTypeFor = "incoming" }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    "내 메시지 목록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "💡 메시지에 {이름}, {시간}, {날짜}를 넣으면 자동으로 바뀌어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (templates.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Message, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("아직 메시지가 없어요",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("오른쪽 아래 + 버튼을 눌러 추가해보세요",
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

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // 타입별 메시지 선택 다이얼로그
    pickingTypeFor?.let { type ->
        val currentId = when (type) {
            "outgoing" -> settings.outgoingTemplateId
            "missed" -> settings.missedTemplateId
            else -> settings.incomingTemplateId
        }
        val title = when (type) {
            "outgoing" -> "내가 전화를 건 후 보낼 메시지"
            "missed" -> "부재중일 때 보낼 메시지"
            else -> "상대방이 전화한 후 보낼 메시지"
        }
        TemplatePickerDialog(
            title = title,
            templates = templates,
            currentId = currentId,
            onPick = { id ->
                when (type) {
                    "outgoing" -> viewModel.setOutgoingTemplateId(id)
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
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun TypeAssignmentCard(
    icon: ImageVector,
    callTypeLabel: String,
    title: String,
    template: MessageTemplate?,
    enabled: Boolean,
    onChangeTapped: () -> Unit
) {
    val containerColor = if (enabled) MaterialTheme.colorScheme.secondaryContainer
                         else MaterialTheme.colorScheme.surfaceVariant
    val onContainerColor = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (enabled) MaterialTheme.colorScheme.secondary
                      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp), tint = accentColor)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = onContainerColor)
                    Text(callTypeLabel, style = MaterialTheme.typography.labelSmall,
                        color = accentColor)
                }
                if (!enabled) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("꺼짐", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (template != null) {
                        Text(template.name, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = onContainerColor)
                        Text(
                            template.content.take(45) + if (template.content.length > 45) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.7f)
                        )
                    } else {
                        Text("메시지가 없어요 · 아래에서 추가해주세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainerColor.copy(alpha = 0.6f))
                    }
                }
                TextButton(onClick = onChangeTapped) { Text("변경하기") }
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
        title = { Text(title) },
        text = {
            if (templates.isEmpty()) {
                Text("메시지가 없어요. 먼저 메시지를 추가해주세요.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { t ->
                        val isSelected = t.id == currentId
                        Card(
                            onClick = { onPick(t.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.name, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(
                                        t.content.take(35) + if (t.content.length > 35) "…" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(template.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                if (template.isDefault) {
                    SuggestionChip(onClick = {},
                        label = { Text("기본", style = MaterialTheme.typography.labelSmall) })
                    Spacer(Modifier.width(4.dp))
                }
                if (template.imageUri != null) {
                    Icon(Icons.Default.Image, "이미지 첨부됨", Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(template.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("수정하기")
                }
                TextButton(onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("삭제하기")
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
        uri?.let {
            val path = viewModel.copyImageToInternal(it)
            selectedImagePath = path
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("메시지 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("보낼 내용") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("{이름} {시간} {날짜} 를 넣으면 자동으로 바뀌어요") }
                )

                if (selectedImagePath != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val bitmap = remember(selectedImagePath) {
                            selectedImagePath?.let {
                                try { BitmapFactory.decodeFile(it)?.asImageBitmap() } catch (e: Exception) { null }
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "첨부 이미지",
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Image, null, Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("이미지가 첨부돼요", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text("사진과 함께 메시지가 전송돼요",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { selectedImagePath = null }) {
                            Icon(Icons.Default.Close, "이미지 제거",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
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
                onClick = {
                    if (name.isNotBlank() && content.isNotBlank())
                        onConfirm(name.trim(), content.trim(), selectedImagePath)
                },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) { Text("저장하기") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
