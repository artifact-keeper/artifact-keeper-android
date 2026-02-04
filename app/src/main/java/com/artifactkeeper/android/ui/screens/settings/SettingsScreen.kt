package com.artifactkeeper.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.ServerManager
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.LoginRequest
import com.artifactkeeper.android.data.models.SavedServer
import com.artifactkeeper.android.data.models.UserInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onDisconnect: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("artifact_keeper_prefs", android.content.Context.MODE_PRIVATE)
    }

    val servers by ServerManager.servers.collectAsState()
    val activeServerId by ServerManager.activeServerId.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf<UserInfo?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<SavedServer?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Restore auth user info on load
    LaunchedEffect(Unit) {
        val savedToken = prefs.getString("auth_token", null)
        val savedUsername = prefs.getString("user_username", null)
        val savedEmail = prefs.getString("user_email", null)
        val savedIsAdmin = prefs.getBoolean("user_is_admin", false)
        val savedUserId = prefs.getString("user_id", null)

        if (savedToken != null && savedUsername != null && savedUserId != null) {
            currentUser = UserInfo(
                id = savedUserId,
                username = savedUsername,
                email = savedEmail,
                isAdmin = savedIsAdmin,
            )
        }
    }

    // Disconnect confirmation dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect from server?") },
            text = {
                Text("This will remove the server configuration and sign you out. You will need to set up the server connection again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectDialog = false
                        onDisconnect()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Remove server confirmation dialog
    if (showRemoveDialog != null) {
        val server = showRemoveDialog!!
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("Remove server?") },
            text = {
                Text("Remove \"${server.name}\" (${server.url}) from saved servers?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ServerManager.removeServer(server.id)
                        showRemoveDialog = null
                        if (servers.isEmpty()) {
                            onDisconnect()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Add server dialog
    if (showAddServerDialog) {
        AddServerDialog(
            onDismiss = { showAddServerDialog = false },
            onServerAdded = { name, url ->
                ServerManager.addServer(name, url)
                prefs.edit().putString("server_url", url).apply()
                ApiClient.configure(url, null)
                showAddServerDialog = false
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // --- Servers section ---
        item {
            Text(
                text = "Servers",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        items(servers, key = { it.id }) { server ->
            val isActive = server.id == activeServerId
            ServerCard(
                server = server,
                isActive = isActive,
                onSwitch = {
                    ServerManager.switchTo(server.id)
                    prefs.edit().putString("server_url", server.url).apply()
                },
                onRemove = { showRemoveDialog = server },
            )
        }

        item {
            OutlinedButton(
                onClick = { showAddServerDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Server")
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // --- Account section ---
        item {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        item {
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    val user = ApiClient.api.getMe()
                                    currentUser = user
                                    prefs.edit()
                                        .putString("auth_token", response.accessToken)
                                        .putString("user_id", user.id)
                                        .putString("user_username", user.username)
                                        .putString("user_email", user.email)
                                        .putBoolean("user_is_admin", user.isAdmin)
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
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // --- About section ---
        item {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Artifact Keeper",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Platform: Android",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // --- Disconnect ---
        item {
            OutlinedButton(
                onClick = { showDisconnectDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Change Server / Disconnect")
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: SavedServer,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isActive) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = server.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isActive) {
                IconButton(onClick = onSwitch) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Switch to this server",
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove server",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onServerAdded: (name: String, url: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server URL") },
                    placeholder = { Text("https://artifacts.example.com") },
                    singleLine = true,
                    isError = errorMessage != null,
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (isTesting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Testing connection...",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanedUrl = url.trim()
                    if (cleanedUrl.isBlank()) {
                        errorMessage = "Please enter a server URL"
                        return@TextButton
                    }
                    coroutineScope.launch {
                        isTesting = true
                        errorMessage = null
                        try {
                            ApiClient.configure(cleanedUrl)
                            ApiClient.api.listRepositories(page = 1, perPage = 1)
                            val host = try {
                                java.net.URI(cleanedUrl).host ?: cleanedUrl
                            } catch (_: Exception) {
                                cleanedUrl
                            }
                            onServerAdded(host, cleanedUrl)
                        } catch (e: Exception) {
                            errorMessage = "Connection failed: ${e.message}"
                        } finally {
                            isTesting = false
                        }
                    }
                },
                enabled = !isTesting,
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
