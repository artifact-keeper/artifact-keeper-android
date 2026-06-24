@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.repositories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.client.models.RepoTokenResponse

private val ActiveColor = Color(0xFF52C41A)
private val RevokedColor = Color(0xFFF5222D)

/** Scopes offered in the create-token dialog. */
private val TokenScopes = listOf("read", "write", "delete", "admin")

/**
 * Access tokens for a single repository: list, create (the plaintext token is
 * shown once), view a token's detail by id, and revoke a token with a confirm
 * dialog.
 */
@Composable
fun RepoTokensScreen(
    repoKey: String,
    onBack: () -> Unit,
    viewModel: RepoTokensViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val detailState by viewModel.tokenDetailState.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var tokenToRevoke by remember { mutableStateOf<RepoTokenResponse?>(null) }
    var showDetail by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(repoKey) { viewModel.load(repoKey) }

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tokens - $repoKey", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create token")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.tokens.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.load(repoKey, refresh = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.tokens.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No tokens. Use + to create one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.tokens, key = { it.id }) { token ->
                            RepoTokenCard(
                                token = token,
                                isMutating = state.isMutating,
                                onView = {
                                    viewModel.loadTokenDetail(repoKey, token.id)
                                    showDetail = true
                                },
                                onRevoke = { tokenToRevoke = token },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateTokenDialog(
            isMutating = state.isMutating,
            onDismiss = { showCreate = false },
            onConfirm = { name, scopes, expiresInDays ->
                viewModel.createToken(repoKey, name, scopes, expiresInDays)
                showCreate = false
            },
        )
    }

    state.newTokenSecret?.let { secret ->
        AlertDialog(
            onDismissRequest = { viewModel.clearNewTokenSecret() },
            title = { Text("Copy your token now") },
            text = {
                Column {
                    Text(
                        text = "This is the only time the token is shown. Store it somewhere safe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = secret,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearNewTokenSecret() }) { Text("Done") }
            },
        )
    }

    tokenToRevoke?.let { token ->
        AlertDialog(
            onDismissRequest = { tokenToRevoke = null },
            title = { Text("Revoke token") },
            text = { Text("Revoke \"${token.name}\"? Anything using it will lose access.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.revokeToken(repoKey, token.id)
                    tokenToRevoke = null
                }) { Text("Revoke") }
            },
            dismissButton = {
                TextButton(onClick = { tokenToRevoke = null }) { Text("Cancel") }
            },
        )
    }

    if (showDetail) {
        TokenDetailDialog(
            isLoading = detailState.isLoading,
            token = detailState.token,
            error = detailState.error,
            onDismiss = { showDetail = false },
        )
    }
}

@Composable
private fun RepoTokenCard(
    token: RepoTokenResponse,
    isMutating: Boolean,
    onView: () -> Unit,
    onRevoke: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = token.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TokenStatusBadge(revoked = token.isRevoked, expired = token.isExpired)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${token.tokenPrefix}... - ${token.scopes.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onView) { Text("View") }
                if (!token.isRevoked) {
                    TextButton(onClick = onRevoke, enabled = !isMutating) { Text("Revoke") }
                }
            }
        }
    }
}

@Composable
private fun TokenStatusBadge(revoked: Boolean, expired: Boolean) {
    val (text, color) = when {
        revoked -> "Revoked" to RevokedColor
        expired -> "Expired" to RevokedColor
        else -> "Active" to ActiveColor
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun CreateTokenDialog(
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, scopes: List<String>, expiresInDays: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val selectedScopes = remember { mutableStateListOf("read") }
    var expiresText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create token") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Scopes", style = MaterialTheme.typography.labelMedium)
                TokenScopes.forEach { scope ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedScopes.contains(scope),
                            onCheckedChange = { checked ->
                                if (checked) selectedScopes.add(scope) else selectedScopes.remove(scope)
                            },
                        )
                        Text(scope)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = expiresText,
                    onValueChange = { expiresText = it.filter { c -> c.isDigit() } },
                    label = { Text("Expires in days (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name.trim(), selectedScopes.toList(), expiresText.toLongOrNull())
                },
                enabled = !isMutating && name.isNotBlank() && selectedScopes.isNotEmpty(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TokenDetailDialog(
    isLoading: Boolean,
    token: RepoTokenResponse?,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(token?.name ?: "Token") },
        text = {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
                token != null -> {
                    Column {
                        DetailRow("Prefix", "${token.tokenPrefix}...")
                        DetailRow("Scopes", token.scopes.joinToString(", "))
                        token.description?.takeIf { it.isNotBlank() }?.let { DetailRow("Description", it) }
                        token.createdBy?.takeIf { it.isNotBlank() }?.let { DetailRow("Created by", it) }
                        DetailRow("Created", token.createdAt.toLocalDate().toString())
                        token.expiresAt?.let { DetailRow("Expires", it.toLocalDate().toString()) }
                        token.lastUsedAt?.let { DetailRow("Last used", it.toLocalDate().toString()) }
                        DetailRow(
                            "Status",
                            when {
                                token.isRevoked -> "Revoked"
                                token.isExpired -> "Expired"
                                else -> "Active"
                            },
                        )
                    }
                }
                else -> Text("No detail available.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
