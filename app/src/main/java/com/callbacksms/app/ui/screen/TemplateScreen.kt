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
import com.callbacksms.app.data.model.MessageTemplate
import com.callbacksms.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(viewModel: MainViewModel, paddingValues: PaddingValues) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<MessageTemplate?>(null) }
    var deleteTarget by remember { mutableStateOf<MessageTemplate?>(null) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(title = { Text("메시지 템플릿", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "템플릿 추가")
            }
        }
    ) { inner ->
        if (state.templates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Message, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("템플릿이 없습니다", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+ 버튼으로 추가하세요", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("변수: {이름} {시간} {날짜} 를 메시지에 넣으면 자동으로 바뀝니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                items(state.templates, key = { it.id }) { template ->
                    val isActive = if (state.settings.activeTemplateId > 0)
                        template.id == state.settings.activeTemplateId
                    else template.isDefault

                    TemplateCard(
                        template = template,
                        isActive = isActive,
                        onSetActive = { viewModel.setActiveTemplate(template.id) },
                        onEdit = { editingTemplate = template },
                        onDelete = { deleteTarget = template }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        TemplateDialog(
            title = "새 템플릿",
            initial = MessageTemplate(name = "", content = ""),
            onConfirm = { name, content ->
                viewModel.addTemplate(name, content)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingTemplate?.let { t ->
        TemplateDialog(
            title = "템플릿 수정",
            initial = t,
            onConfirm = { name, content ->
                viewModel.updateTemplate(t.copy(name = name, content = content))
                editingTemplate = null
            },
            onDismiss = { editingTemplate = null }
        )
    }

    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("삭제 확인") },
            text = { Text("'${t.name}' 템플릿을 삭제하시겠어요?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTemplate(t); deleteTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: MessageTemplate,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isActive) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                }
                Text(template.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                if (isActive) {
                    SuggestionChip(onClick = {}, label = { Text("사용 중", style = MaterialTheme.typography.labelMedium) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(template.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (!isActive) {
                    TextButton(onClick = onSetActive) {
                        Icon(Icons.Default.RadioButtonUnchecked, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("선택")
                    }
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("수정")
                }
                TextButton(onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
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
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var content by remember { mutableStateOf(initial.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("템플릿 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("메시지 내용") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("{이름} {시간} {날짜} 변수 사용 가능") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && content.isNotBlank()) onConfirm(name.trim(), content.trim()) },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
