package com.artifactkeeper.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.LoginRequest
import com.artifactkeeper.android.data.models.UserInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("artifact_keeper_prefs", android.content.Context.MODE_PRIVATE)
    }
    var serverUrl by remember { mutableStateOf(prefs.getString("server_url", ApiClient.baseUrl) ?: ApiClient.baseUrl) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf<UserInfo?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var serverSaved by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Restore token on load
    LaunchedEffect(Unit) {
        val savedUrl = prefs.getString("server_url", null)
        val savedToken = prefs.getString("auth_token", null)
        val savedUsername = prefs.getString("user_username", null)
        val savedEmail = prefs.getString("user_email", null)
        val savedIsAdmin = prefs.getBoolean("user_is_admin", false)
        val savedUserId = prefs.getString("user_id", null)

        if (savedUrl != null) {
            serverUrl = savedUrl
            ApiClient.configure(savedUrl, savedToken)
        }
        if (savedToken != null && savedUsername != null && savedUserId != null) {
            currentUser = UserInfo(
                id = savedUserId,
                username = savedUsername,
                email = savedEmail,
                isAdmin = savedIsAdmin,
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Server URL section
            Text(
                text = "Server Configuration",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    serverSaved = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Server URL") },
                placeholder = { Text("http://10.0.2.2:30080") },
                singleLine = true,
            )

            Button(
                onClick = {
                    val url = serverUrl.trim()
                    prefs.edit().putString("server_url", url).apply()
                    ApiClient.configure(url, ApiClient.token)
                    serverSaved = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (serverSaved) "Saved" else "Save Server URL")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Auth section
            Text(
                text = "Authentication",
                style = MaterialTheme.typography.titleMedium,
            )

            if (currentUser != null) {
                // Logged in state
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = currentUser!!.username,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (currentUser!!.isAdmin) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Admin", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = "Admin",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }
                        }
                        if (currentUser!!.email != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentUser!!.email!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        currentUser = null
                        ApiClient.setToken(null)
                        prefs.edit()
                            .remove("auth_token")
                            .remove("user_id")
                            .remove("user_username")
                            .remove("user_email")
                            .remove("user_is_admin")
                            .apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Logout")
                }
            } else {
                // Login form
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        loginError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        loginError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )

                if (loginError != null) {
                    Text(
                        text = loginError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            loginError = "Username and password are required"
                            return@Button
                        }
                        coroutineScope.launch {
                            isLoggingIn = true
                            loginError = null
                            try {
                                val response = ApiClient.api.login(
                                    LoginRequest(username.trim(), password)
                                )
                                ApiClient.setToken(response.accessToken)
                                currentUser = response.user
                                prefs.edit()
                                    .putString("auth_token", response.accessToken)
                                    .putString("user_id", response.user.id)
                                    .putString("user_username", response.user.username)
                                    .putString("user_email", response.user.email)
                                    .putBoolean("user_is_admin", response.user.isAdmin)
                                    .apply()
                                username = ""
                                password = ""
                            } catch (e: Exception) {
                                loginError = e.message ?: "Login failed"
                            } finally {
                                isLoggingIn = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoggingIn,
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // App version
            Text(
                text = "Artifact Keeper for Android v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
