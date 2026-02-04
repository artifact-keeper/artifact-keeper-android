package com.artifactkeeper.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.LoginRequest
import com.artifactkeeper.android.data.models.UserInfo
import kotlinx.coroutines.launch

@Composable
fun AccountMenu(
    currentUser: UserInfo?,
    onLoggedIn: (UserInfo, String) -> Unit,
    onLoggedOut: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = {
            if (currentUser != null) showMenu = true
            else showLoginDialog = true
        }) {
            Icon(
                imageVector = if (currentUser != null) Icons.Default.AccountCircle else Icons.Default.Login,
                contentDescription = if (currentUser != null) "Account" else "Sign in",
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
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
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Sign Out") },
                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onLoggedOut()
                    },
                )
            }
        }
    }

    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onSuccess = { user, token ->
                showLoginDialog = false
                onLoggedIn(user, token)
            },
        )
    }
}

@Composable
private fun LoginDialog(
    onDismiss: () -> Unit,
    onSuccess: (UserInfo, String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Sign In") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            onSuccess(user, response.accessToken)
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
