package com.smartagents.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.smartagents.auth.AuthApi
import com.smartagents.auth.AuthState
import com.smartagents.auth.AuthStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface AppState {
    data object Checking : AppState
    data class LoggedIn(val auth: AuthState, val currentView: String = "home") : AppState
    data object Login : AppState
}

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)
    var appState by mutableStateOf<AppState>(AppState.Checking)
    val scope = rememberCoroutineScope()

    fun doAutoLogin() {
        appState = AppState.Checking
        scope.launch {
            val saved = withContext(Dispatchers.IO) { AuthStorage.load() }
            if (saved != null) {
                val verify = AuthApi.verify(saved.token)
                if (verify.ok && verify.username != null) {
                    appState = AppState.LoggedIn(AuthState(saved.token, verify.username))
                } else {
                    withContext(Dispatchers.IO) { AuthStorage.clear() }
                    appState = AppState.Login
                }
            } else {
                appState = AppState.Login
            }
        }
    }

    LaunchedEffect(Unit) { doAutoLogin() }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "SmartAgents"
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            when (val s = appState) {
                is AppState.Checking -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AppState.Login -> {
                    LoginScreen(
                        onLoginSuccess = { state ->
                            AuthStorage.save(state)
                            appState = AppState.LoggedIn(state)
                        },
                        onRetry = { doAutoLogin() }
                    )
                }
                is AppState.LoggedIn -> {
                    when (s.currentView) {
                        "office" -> OfficeScreen(
                            auth = s.auth,
                            onBack = { appState = AppState.LoggedIn(s.auth, "home") },
                            onLogout = { AuthStorage.clear(); appState = AppState.Login }
                        )
                        else -> HomeScreen(
                            auth = s.auth,
                            onNavigate = { view -> appState = AppState.LoggedIn(s.auth, view) },
                            onLogout = { AuthStorage.clear(); appState = AppState.Login }
                        )
                    }
                }
            }
        }
    }
}
