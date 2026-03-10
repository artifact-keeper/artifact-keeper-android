package com.artifactkeeper.android.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.CreateApiTokenRequest
import com.artifactkeeper.client.models.CreateApiTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request

// Local model for listing tokens (uses OkHttp because the SDK's AuthApi lacks a list endpoint)
@Serializable
private data class ApiTokenItem(
    val id: String,
    val name: String,
    val scopes: List<String> = emptyList(),
    @SerialName("token_prefix") val tokenPrefix: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)

@Serializable
private data class ApiTokenListLocal(
    val items: List<ApiTokenItem> = emptyList(),
)

private val json = Json { ignoreUnknownKeys = true }

private val AVAILABLE_SCOPES = listOf(
    "read:repos", "write:repos", "read:packages", "write:packages",
    "read:builds", "write:builds", "admin",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTokensScreen(
    onBack: () -> Unit,
) {
    var tokens by remember { mutableStateOf<List<ApiTokenItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createdToken by remember { mutableStateOf<CreateApiTokenResponse?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ApiTokenItem?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    fun loadTokens(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                val response = withContext(Dispatchers.IO) {
                    val url = "${ApiClient.baseUrl}api/v1/auth/tokens"
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .apply {
                            ApiClient.token?.let { addHeader("Authorization", "Bearer $it") }
                        }
                        .build()
                    val resp = ApiClient.httpClient.newCall(request).execute()
                    val body = resp.body?.string() ?: "{\"items\":[]}"
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}: $body")
                    json.decodeFromString<ApiTokenListLocal>(body)
                }
                tokens = response.items
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load tokens"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadTokens() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Tokens") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create token")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { loadTokens() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { loadTokens(refresh = true) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (tokens.isEmpty() && createdToken == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No API tokens",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { showCreateDialog = true }) {
                                    Text("Create Token")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Show newly created token value (only visible once)
                            if (createdToken != null) {
                                item(key = "new-token") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        ),
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Token created: ${createdToken!!.name}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Copy this token now. It will not be shown again.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = createdToken!!.token,
                                                    fontFamily = FontFamily.Monospace,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                IconButton(onClick = {
                                                    clipboardManager.setText(AnnotatedString(createdToken!!.token))
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Token copied to clipboard")
                                                    }
                                                }) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "Copy token",
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(onClick = { createdToken = null }) {
                                                Text("Dismiss")
                                            }
                                        }
                                    }
                                }
                            }

                            items(tokens, key = { it.id }) { token ->
                                TokenCard(
                                    token = token,
                                    onRevoke = { showDeleteDialog = token },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create token dialog
    if (showCreateDialog) {
        CreateTokenDialog(
            onDismiss = { showCreateDialog = false },
            onCreated = { response ->
                showCreateDialog = false
                createdToken = response
                loadTokens(refresh = true)
            },
        )
    }

    // Revoke confirmation dialog
    if (showDeleteDialog != null) {
        val tokenToDelete = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Revoke Token") },
            text = {
                Text("Revoke \"${tokenToDelete.name}\"? This cannot be undone. Any applications using this token will lose access.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                ApiClient.authApi.revokeApiToken(
                                    java.util.UUID.fromString(tokenToDelete.id)
                                ).unwrap()
                                showDeleteDialog = null
                                loadTokens(refresh = true)
                                snackbarHostState.showSnackbar("Token revoked")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to revoke: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Revoke")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun TokenCard(token: ApiTokenItem, onRevoke: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = token.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${token.tokenPrefix}...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (token.scopes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scopes: ${token.scopes.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Created: ${token.createdAt.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                token.expiresAt?.let { exp ->
                    Text(
                        text = "Expires: ${exp.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onRevoke) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Revoke token",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CreateTokenDialog(
    onDismiss: () -> Unit,
    onCreated: (CreateApiTokenResponse) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedScopes by remember { mutableStateOf(setOf("read:repos", "read:packages")) }
    var expiresInDays by remember { mutableStateOf("90") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Create API Token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Token Name") },
                    placeholder = { Text("CI/CD Pipeline") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                )

                Text(
                    text = "Scopes",
                    style = MaterialTheme.typography.labelMedium,
                )
                AVAILABLE_SCOPES.forEach { scope ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = scope in selectedScopes,
                            onCheckedChange = { checked ->
                                selectedScopes = if (checked) selectedScopes + scope
                                else selectedScopes - scope
                            },
                            enabled = !isSaving,
                        )
                        Text(
                            text = scope,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                OutlinedTextField(
                    value = expiresInDays,
                    onValueChange = {
                        expiresInDays = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Expires in (days)") },
                    supportingText = { Text("Leave empty for no expiration") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Token name is required"
                        return@TextButton
                    }
                    if (selectedScopes.isEmpty()) {
                        errorMessage = "Select at least one scope"
                        return@TextButton
                    }
                    coroutineScope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val request = CreateApiTokenRequest(
                                name = name.trim(),
                                scopes = selectedScopes.toList(),
                                expiresInDays = expiresInDays.toLongOrNull(),
                            )
                            val response = ApiClient.authApi.createApiToken(request).unwrap()
                            onCreated(response)
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to create token"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
            ) {
                Text("Cancel")
            }
        },
    )
}
