package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
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
import com.artifactkeeper.client.models.SigningKeyPublic
import java.util.UUID

private val ActiveColor = Color(0xFF52C41A)
private val RevokedColor = Color(0xFFF5222D)

/** Key algorithms offered in the create-key dialog. */
private val KeyAlgorithms = listOf("ed25519", "rsa", "ecdsa")

/**
 * Signing keys management: lists keys, creates new keys, and rotates, revokes,
 * or deletes existing ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigningKeysScreen(
    viewModel: SigningViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val keyDetailState by viewModel.keyDetailState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var keyToRevoke by remember { mutableStateOf<SigningKeyPublic?>(null) }
    var keyToDelete by remember { mutableStateOf<SigningKeyPublic?>(null) }
    var showKeyDetail by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadKeys() }

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create signing key")
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
                state.error != null && state.keys.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.loadKeys(refresh = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.keys.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No signing keys. Use + to create one.",
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
                        items(state.keys, key = { it.id }) { key ->
                            SigningKeyCard(
                                key = key,
                                isMutating = state.isMutating,
                                onView = {
                                    viewModel.loadKeyDetail(key.id)
                                    showKeyDetail = true
                                },
                                onRotate = { viewModel.rotateKey(key.id) },
                                onRevoke = { keyToRevoke = key },
                                onDelete = { keyToDelete = key },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateKeyDialog(
            isMutating = state.isMutating,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, algorithm ->
                viewModel.createKey(name = name, algorithm = algorithm, keyType = null)
                showCreateDialog = false
            },
        )
    }

    keyToRevoke?.let { key ->
        ConfirmDialog(
            title = "Revoke key",
            message = "Revoke \"${key.name}\"? Signatures made with it will no longer validate.",
            confirmLabel = "Revoke",
            onConfirm = {
                viewModel.revokeKey(key.id)
                keyToRevoke = null
            },
            onDismiss = { keyToRevoke = null },
        )
    }

    keyToDelete?.let { key ->
        ConfirmDialog(
            title = "Delete key",
            message = "Permanently delete \"${key.name}\"? This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteKey(key.id)
                keyToDelete = null
            },
            onDismiss = { keyToDelete = null },
        )
    }

    if (showKeyDetail) {
        KeyDetailDialog(
            isLoading = keyDetailState.isLoading,
            keyName = keyDetailState.key?.name,
            fingerprint = keyDetailState.key?.fingerprint,
            publicKeyPem = keyDetailState.publicKeyPem,
            error = keyDetailState.error,
            onDismiss = { showKeyDetail = false },
        )
    }
}

@Composable
private fun SigningKeyCard(
    key: SigningKeyPublic,
    isMutating: Boolean,
    onView: () -> Unit,
    onRotate: () -> Unit,
    onRevoke: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = key.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusBadge(active = key.isActive)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${key.keyType.uppercase()} - ${key.algorithm}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            key.fingerprint?.takeIf { it.isNotBlank() }?.let { fp ->
                Text(
                    text = fp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onView) { Text("View") }
                TextButton(onClick = onRotate, enabled = !isMutating) { Text("Rotate") }
                if (key.isActive) {
                    TextButton(onClick = onRevoke, enabled = !isMutating) { Text("Revoke") }
                }
                TextButton(onClick = onDelete, enabled = !isMutating) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun StatusBadge(active: Boolean) {
    val color = if (active) ActiveColor else RevokedColor
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = if (active) "Active" else "Revoked",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateKeyDialog(
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, algorithm: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var algorithm by remember { mutableStateOf(KeyAlgorithms.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create signing key") },
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
                Text("Algorithm", style = MaterialTheme.typography.labelMedium)
                KeyAlgorithms.forEach { algo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = algorithm == algo,
                            onClick = { algorithm = algo },
                        )
                        Text(algo)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), algorithm) },
                enabled = !isMutating && name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun KeyDetailDialog(
    isLoading: Boolean,
    keyName: String?,
    fingerprint: String?,
    publicKeyPem: String?,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(keyName ?: "Signing key") },
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
                else -> {
                    Column {
                        fingerprint?.takeIf { it.isNotBlank() }?.let { fp ->
                            Text(
                                text = "Fingerprint",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = fp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = "Public key",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = publicKeyPem ?: "Unavailable",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
