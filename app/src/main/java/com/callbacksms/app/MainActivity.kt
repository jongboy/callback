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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.callbacksms.app.auth.DeviceAuth
import com.callbacksms.app.ui.Navigation
import com.callbacksms.app.ui.screen.AdminScreen
import com.callbacksms.app.ui.screen.BlockedScreen
import com.callbacksms.app.ui.screen.LicenseInputScreen
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

private sealed class AuthState {
    object Loading : AuthState()
    object Admin : AuthState()
    object AdminAllowed : AuthState()  // 관리자가 앱 사용 중
    object Allowed : AuthState()
    object NeedCode : AuthState()
    data class Checking(val key: String) : AuthState()
    data class Denied(val reason: String) : AuthState()
    object NetworkError : AuthState()
}

@Composable
private fun AuthGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<AuthState>(AuthState.Loading) }
    var inputError by remember { mutableStateOf<String?>(null) }

    fun validate(key: String) {
        // 관리자 코드면 Firebase 검증 없이 바로 관리자 화면으로
        if (DeviceAuth.isAdminCode(key)) {
            DeviceAuth.saveLicense(context, key)
            state = AuthState.Admin
            return
        }
        state = AuthState.Checking(key)
        inputError = null
        DeviceAuth.validate(context, key) { result ->
            when (result) {
                DeviceAuth.Result.Allowed -> {
                    DeviceAuth.saveLicense(context, key)
                    state = AuthState.Allowed
                }
                is DeviceAuth.Result.Denied -> {
                    if (state is AuthState.Checking) {
                        inputError = result.reason
                        state = AuthState.NeedCode
                    } else {
                        state = AuthState.Denied(result.reason)
                    }
                }
                DeviceAuth.Result.NetworkError -> state = AuthState.NetworkError
            }
        }
    }

    // 앱 시작 시: 저장된 코드 자동 검증
    LaunchedEffect(Unit) {
        val stored = DeviceAuth.getStoredLicense(context)
        when {
            stored == null -> state = AuthState.NeedCode
            DeviceAuth.isAdminCode(stored) -> state = AuthState.Admin
            else -> validate(stored)
        }
    }

    // 포그라운드 복귀 시마다 재검증 (관리자/일반 모두)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && state == AuthState.Allowed) {
                val stored = DeviceAuth.getStoredLicense(context)
                if (stored != null) validate(stored)
                else state = AuthState.NeedCode
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    when (val s = state) {
        AuthState.Loading, is AuthState.Checking -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AuthState.Admin -> AdminScreen(
            onLogout = {
                DeviceAuth.clearLicense(context)
                inputError = null
                state = AuthState.NeedCode
            },
            onUseApp = { state = AuthState.AdminAllowed }
        )
        AuthState.AdminAllowed -> content()
        AuthState.Allowed -> content()

        AuthState.NeedCode -> LicenseInputScreen(
            isLoading = false,
            errorMessage = inputError,
            onSubmit = { key -> validate(key) }
        )

        is AuthState.Denied -> BlockedScreen(
            reason = s.reason,
            isNetworkError = false,
            onRetry = {
                val stored = DeviceAuth.getStoredLicense(context)
                if (stored != null) validate(stored)
                else state = AuthState.NeedCode
            },
            onEnterNewCode = {
                DeviceAuth.clearLicense(context)
                inputError = null
                state = AuthState.NeedCode
            }
        )

        AuthState.NetworkError -> BlockedScreen(
            reason = "인터넷 연결을 확인하고 다시 시도해주세요",
            isNetworkError = true,
            onRetry = {
                val stored = DeviceAuth.getStoredLicense(context)
                if (stored != null) validate(stored)
                else state = AuthState.NeedCode
            },
            onEnterNewCode = {
                DeviceAuth.clearLicense(context)
                inputError = null
                state = AuthState.NeedCode
            }
        )
    }
}
