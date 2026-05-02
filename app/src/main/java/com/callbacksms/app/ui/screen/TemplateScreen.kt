package com.callbacksms.app.ui.screen

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
import com.callbacksms.app.data.AppSettings
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
            item {
                Text("메시지 목록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text("각 메시지 카드에서 어떤 경우에 보낼지 직접 설정해요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text("💡 {이름} {시간} {날짜} 를 넣으면 자동으로 바뀌어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (templates.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
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
                                fontWeight = FontWeight.SemiBold)
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
                    settings = settings,
                    onToggleOutgoing = {
                        if (settings.outgoingTemplateId == template.id) viewModel.setOutgoingTemplateId(-1L)
                        else viewModel.setOutgoingTemplateId(template.id)
                    },
                    onToggleOutgoingMissed = {
                        if (settings.outgoingMissedTemplateId == template.id) viewModel.setOutgoingMissedTemplateId(-1L)
                        else viewModel.setOutgoingMissedTemplateId(template.id)
                    },
                    onToggleMissed = {
                        if (settings.missedTemplateId == template.id) viewModel.setMissedTemplateId(-1L)
                        else viewModel.setMissedTemplateId(template.id)
                    },
                    onToggleIncoming = {
                        if (settings.incomingTemplateId == template.id) viewModel.setIncomingTemplateId(-1L)
                        else viewModel.setIncomingTemplateId(template.id)
                    },
                    onEdit = { editingTemplate = template },
                    onDelete = { deleteTarget = template }
                )
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
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
private fun TemplateCard(
    template: MessageTemplate,
    settings: AppSettings,
    onToggleOutgoing: () -> Unit,
    onToggleOutgoingMissed: () -> Unit,
    onToggleMissed: () -> Unit,
    onToggleIncoming: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOutgoing = settings.outgoingTemplateId == template.id
    val isOutgoingMissed = settings.outgoingMissedTemplateId == template.id
    val isMissed = settings.missedTemplateId == template.id
    val isIncoming = settings.incomingTemplateId == template.id
    val hasAnyType = isOutgoing || isOutgoingMissed || isMissed || isIncoming

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            // 헤더
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(template.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
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
                            Icon(Icons.Default.Image, null, Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(3.dp))
                            Text("사진", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(template.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            // 어떤 경우에 보낼까요?
            Text("어떤 경우에 이 메시지를 보낼까요?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeToggleChip(
                        label = "내가 건 전화 후",
                        icon = Icons.Default.CallMade,
                        selected = isOutgoing,
                        onClick = onToggleOutgoing,
                        modifier = Modifier.weight(1f)
                    )
                    TypeToggleChip(
                        label = "내가 건 전화 부재중",
                        icon = Icons.Default.CallEnd,
                        selected = isOutgoingMissed,
                        onClick = onToggleOutgoingMissed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeToggleChip(
                        label = "상대방이 건 전화 부재중",
                        icon = Icons.Default.PhoneMissed,
                        selected = isMissed,
                        onClick = onToggleMissed,
                        modifier = Modifier.weight(1f)
                    )
                    TypeToggleChip(
                        label = "상대방이 건 전화 후",
                        icon = Icons.Default.CallReceived,
                        selected = isIncoming,
                        onClick = onToggleIncoming,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!hasAnyType) {
                Spacer(Modifier.height(6.dp))
                Text("아직 설정된 경우가 없어요. 위에서 선택해보세요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }

            Spacer(Modifier.height(8.dp))
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

@Composable
private fun TypeToggleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1)
        },
        leadingIcon = {
            Icon(icon, null, Modifier.size(14.dp))
        },
        modifier = modifier
    )
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
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
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
