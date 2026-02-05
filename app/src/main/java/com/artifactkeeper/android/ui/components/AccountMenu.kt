package com.artifactkeeper.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.LoginRequest
import com.artifactkeeper.android.data.models.SavedServer
import com.artifactkeeper.android.data.models.UserInfo
import kotlinx.coroutines.launch

@Composable
fun AccountMenu(
    currentUser: UserInfo?,
    servers: List<SavedServer>,
    activeServerId: String?,
    serverStatuses: Map<String, Boolean>,
    onLoggedIn: (UserInfo, String, Boolean) -> Unit,
    onLoggedOut: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSwitchServer: (String) -> Unit,
    onAddServer: (String, String) -> Unit,
    onRemoveServer: (String) -> Unit,
    onRefreshStatuses: () -> Unit,
) {
    var showAccountMenu by remember { mutableStateOf(false) }
    var showServerMenu by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }

    // Check setup status for first-time setup banner
    var setupRequired by remember { mutableStateOf(false) }
    LaunchedEffect(activeServerId) {
        try {
            val status = ApiClient.api.getSetupStatus()
            setupRequired = status.setupRequired
        } catch (_: Exception) {
            setupRequired = false
        }
    }

    LaunchedEffect(showServerMenu) {
        if (showServerMenu) {
            onRefreshStatuses()
        }
    }

    // Server management icon button
    Box {
        IconButton(onClick = { showServerMenu = true }) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = "Servers",
            )
        }

        DropdownMenu(
            expanded = showServerMenu,
            onDismissRequest = { showServerMenu = false },
        ) {
            servers.forEach { server ->
                DropdownMenuItem(
                    text = { Text(server.name) },
                    leadingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                drawCircle(
                                    color = if (serverStatuses[server.id] == true) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                )
                            }
                            if (server.id == activeServerId) {
                                Icon(Icons.Default.Check, contentDescription = "Active", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    trailingIcon = {
                        if (server.id != activeServerId) {
                            IconButton(
                                onClick = {
                                    showServerMenu = false
                                    onRemoveServer(server.id)
                                },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    onClick = {
                        if (server.id != activeServerId) {
                            showServerMenu = false
                            onSwitchServer(server.id)
                        }
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Add Server") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    showServerMenu = false
                    showAddServerDialog = true
                },
            )
        }
    }

    // Account icon button (login / account circle)
    Box {
        IconButton(onClick = {
            if (currentUser != null) showAccountMenu = true
            else showLoginDialog = true
        }) {
            Icon(
                imageVector = if (currentUser != null) Icons.Default.AccountCircle else Icons.Default.Login,
                contentDescription = if (currentUser != null) "Account" else "Sign in",
            )
        }

        DropdownMenu(
            expanded = showAccountMenu,
            onDismissRequest = { showAccountMenu = false },
        ) {
            currentUser?.let { user ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(user.username, fontWeight = FontWeight.Bold)
                            user.email?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    onClick = {},
                    enabled = false,
                )
                if (user.isAdmin) {
                    DropdownMenuItem(
                        text = { Text("Admin") },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                        onClick = {},
                        enabled = false,
                    )
                }
                DropdownMenuItem(
                    text = { Text("Profile") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = {
                        showAccountMenu = false
                        onProfileClick()
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Sign Out") },
                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    onClick = {
                        showAccountMenu = false
                        onLoggedOut()
                    },
                )
            }
        }
    }

    if (showLoginDialog) {
        LoginDialog(
            setupRequired = setupRequired,
            onDismiss = { showLoginDialog = false },
            onSuccess = { user, token, mustChangePassword ->
                showLoginDialog = false
                onLoggedIn(user, token, mustChangePassword)
            },
        )
    }

    if (showAddServerDialog) {
        AddServerDialog(
            onDismiss = { showAddServerDialog = false },
            onServerAdded = { name, url ->
                showAddServerDialog = false
                onAddServer(name, url)
            },
        )
    }
}

@Composable
private fun LoginDialog(
    setupRequired: Boolean = false,
    onDismiss: () -> Unit,
    onSuccess: (UserInfo, String, Boolean) -> Unit,
) {
    var username by remember { mutableStateOf(if (setupRequired) "admin" else "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Sign In") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (setupRequired) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "First-Time Setup",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                "A random admin password was generated. Retrieve it from the server:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                "docker exec artifact-keeper-backend cat /data/storage/admin.password",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                "Log in with username admin and the password from the file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        error = "Username and password are required"
                        return@TextButton
                    }
                    coroutineScope.launch {
                        isLoading = true
                        error = null
                        try {
                            val response = ApiClient.api.login(LoginRequest(username.trim(), password))
                            ApiClient.setToken(response.accessToken)
                            val user = ApiClient.api.getMe()
                            onSuccess(user, response.accessToken, response.mustChangePassword)
                        } catch (e: Exception) {
                            error = e.message ?: "Login failed"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sign In")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onServerAdded: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Add Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    placeholder = { Text("https://artifacts.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || url.isBlank()) {
                        error = "Name and URL are required"
                        return@TextButton
                    }
                    coroutineScope.launch {
                        isLoading = true
                        error = null
                        try {
                            ApiClient.configure(url.trim(), null)
                            ApiClient.api.getHealth()
                            onServerAdded(name.trim(), url.trim())
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to connect to server"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Test & Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        },
    )
}
