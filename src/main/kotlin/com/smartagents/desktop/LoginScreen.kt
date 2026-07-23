package com.smartagents.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import com.smartagents.auth.AuthApi
import com.smartagents.auth.AuthState
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (AuthState) -> Unit, onRetry: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.width(360.dp)
            ) {
                Text(
                    "SmartAgents",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "AI 本地协作，数据不离开设备",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            username = it
                            errorMessage = null
                        }
                    },
                    label = { Text("用户名 (4位数字)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            password = it
                            errorMessage = null
                        }
                    },
                    label = { Text("密码 (4位数字)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isLoading
                )

                if (errorMessage != null) {
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        if (username.length != 4 || password.length != 4) {
                            errorMessage = "用户名和密码必须为4位数字"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val res = AuthApi.login(username, password)
                                if (res.ok && res.token != null && res.username != null) {
                                    onLoginSuccess(AuthState(res.token, res.username))
                                } else {
                                    errorMessage = res.message ?: "登录失败"
                                }
                            } catch (e: Exception) {
                                errorMessage = "无法连接服务器，请检查网络"
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("登 录", fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "内测阶段，请通过 off.by.one.com 预约获取凭证",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )

                // Retry button if auto-login failed
                TextButton(onClick = onRetry) {
                    Text("重新检测凭证", fontSize = 12.sp)
                }
            }
        }
    }
}
