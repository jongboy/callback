package com.callbacksms.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.callbacksms.app.auth.DeviceAuth
import com.callbacksms.app.ui.Navigation
import com.callbacksms.app.ui.screen.BlockedScreen
import com.callbacksms.app.ui.theme.CallbackSMSTheme
import com.callbacksms.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onPermissionsResult(permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CallbackSMSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthGate {
                        Navigation(
                            viewModel = viewModel,
                            onRequestPermissions = { requestAllPermissions() }
                        )
                    }
                }
            }
        }
    }

    fun requestAllPermissions() {
        val perms = buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(perms.toTypedArray())
    }
}

// null = 확인 중, true = 승인, false = 차단
@Composable
private fun AuthGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var authState by remember { mutableStateOf<Boolean?>(null) }

    fun doCheck() {
        authState = null
        DeviceAuth.check(context) { authState = it }
    }

    LaunchedEffect(Unit) { doCheck() }

    when (authState) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        true -> content()
        false -> BlockedScreen(
            deviceId = DeviceAuth.getDeviceId(context),
            onRetry = { doCheck() }
        )
    }
}
