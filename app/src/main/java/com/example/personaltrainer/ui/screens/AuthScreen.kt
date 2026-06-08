@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.personaltrainer.data.AuthRepository
import kotlinx.coroutines.launch

private enum class AuthScreenMode(val title: String) {
    Login("Вход"),
    Register("Регистрация")
}

@Composable
fun AuthScreen(
    authRepository: AuthRepository
) {
    val scope = rememberCoroutineScope()

    var authMode by rememberSaveable { mutableStateOf(AuthScreenMode.Login) }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var repeatPassword by rememberSaveable { mutableStateOf("") }

    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Personal Trainer") })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = authMode == AuthScreenMode.Login,
                        onClick = {
                            authMode = AuthScreenMode.Login
                            error = null
                        },
                        label = { Text("Вход") }
                    )

                    FilterChip(
                        selected = authMode == AuthScreenMode.Register,
                        onClick = {
                            authMode = AuthScreenMode.Register
                            error = null
                        },
                        label = { Text("Регистрация") }
                    )
                }
            }

            Text(authMode.title, style = MaterialTheme.typography.titleLarge)

            if (authMode == AuthScreenMode.Register) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            if (authMode == AuthScreenMode.Register) {
                OutlinedTextField(
                    value = repeatPassword,
                    onValueChange = {
                        repeatPassword = it
                        error = null
                    },
                    label = { Text("Повтор пароля") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                enabled = !loading,
                onClick = {
                    when (authMode) {
                        AuthScreenMode.Login -> {
                            if (email.isBlank()) {
                                error = "Введите email"
                                return@Button
                            }

                            if (password.isBlank()) {
                                error = "Введите пароль"
                                return@Button
                            }

                            scope.launch {
                                loading = true
                                error = null

                                runCatching {
                                    authRepository.login(email, password)
                                }.onSuccess {
                                    password = ""
                                    repeatPassword = ""
                                }.onFailure {
                                    error = "Ошибка входа. Проверьте email, пароль и сервер."
                                }

                                loading = false
                            }
                        }

                        AuthScreenMode.Register -> {
                            if (name.isBlank()) {
                                error = "Введите имя"
                                return@Button
                            }

                            if (email.isBlank()) {
                                error = "Введите email"
                                return@Button
                            }

                            if (password.isBlank()) {
                                error = "Введите пароль"
                                return@Button
                            }

                            if (password.length < 4) {
                                error = "Пароль должен быть не короче 4 символов"
                                return@Button
                            }

                            if (password != repeatPassword) {
                                error = "Пароли не совпадают"
                                return@Button
                            }

                            scope.launch {
                                loading = true
                                error = null

                                runCatching {
                                    authRepository.register(
                                        name = name,
                                        email = email,
                                        password = password
                                    )
                                }.onSuccess {
                                    password = ""
                                    repeatPassword = ""
                                }.onFailure {
                                    error = "Ошибка регистрации. Проверьте данные и сервер."
                                }

                                loading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (loading) {
                        "Подождите..."
                    } else {
                        when (authMode) {
                            AuthScreenMode.Login -> "Войти"
                            AuthScreenMode.Register -> "Зарегистрироваться"
                        }
                    }
                )
            }

            Text(
                "Для проверки администратора используй email: admin@example.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}