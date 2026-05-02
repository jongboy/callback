package com.callbacksms.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BlockedScreen(
    reason: String,
    isNetworkError: Boolean = false,
    onRetry: () -> Unit,
    onEnterNewCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Block,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = if (isNetworkError) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(24.dp))

        Text(
            if (isNetworkError) "네트워크 오류" else "사용 불가",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("다시 확인")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onEnterNewCode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("다른 코드 입력")
        }
    }
}
