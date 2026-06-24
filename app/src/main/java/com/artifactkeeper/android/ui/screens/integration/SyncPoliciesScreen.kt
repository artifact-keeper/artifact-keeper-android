package com.artifactkeeper.android.ui.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.client.models.SyncPolicyResponse
import com.artifactkeeper.android.ui.util.formatRelativeTime

private val EnabledColor = Color(0xFF52C41A)
private val DisabledColor = Color(0xFF8C8C8C)

/**
 * Replication sync policies: lists policies (highest priority first), toggles a
 * policy's enabled flag, triggers a global evaluation, and opens a read-only
 * detail loaded fresh by id. Policy authoring (create/update/delete/preview) is
 * not exposed here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncPoliciesScreen(
    viewModel: SyncPoliciesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()
    var policyToToggle by remember { mutableStateOf<SyncPolicyResponse?>(null) }
    var showDetail by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadPolicies() }

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
            ExtendedFloatingActionButton(
                onClick = { viewModel.evaluatePolicies() },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = { Text("Evaluate") },
                expanded = !state.isMutating,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.policies.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.loadPolicies(refresh = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.policies.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No sync policies configured.",
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
                        items(state.policies, key = { it.id }) { policy ->
                            SyncPolicyCard(
                                policy = policy,
                                isMutating = state.isMutating,
                                onView = {
                                    viewModel.loadPolicyDetail(policy.id)
                                    showDetail = true
                                },
                                onToggle = { policyToToggle = policy },
                            )
                        }
                    }
                }
            }
        }
    }

    policyToToggle?.let { policy ->
        val turningOn = !policy.enabled
        AlertDialog(
            onDismissRequest = { policyToToggle = null },
            title = { Text(if (turningOn) "Enable policy" else "Disable policy") },
            text = {
                Text(
                    if (turningOn) {
                        "Enable \"${policy.name}\"? It will participate in the next evaluation."
                    } else {
                        "Disable \"${policy.name}\"? It will be skipped during evaluation."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.togglePolicy(policy.id, enabled = turningOn)
                    policyToToggle = null
                }) {
                    Text(if (turningOn) "Enable" else "Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { policyToToggle = null }) { Text("Cancel") }
            },
        )
    }

    if (showDetail) {
        SyncPolicyDetailDialog(
            isLoading = detailState.isLoading,
            policy = detailState.policy,
            error = detailState.error,
            onDismiss = { showDetail = false },
        )
    }
}

@Composable
private fun SyncPolicyCard(
    policy: SyncPolicyResponse,
    isMutating: Boolean,
    onView: () -> Unit,
    onToggle: () -> Unit,
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
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = policy.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusBadge(enabled = policy.enabled)
            }

            if (policy.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = policy.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${policy.replicationMode} - priority ${policy.priority}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onView) { Text("View") }
                TextButton(onClick = onToggle, enabled = !isMutating) {
                    Text(if (policy.enabled) "Disable" else "Enable")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(enabled: Boolean) {
    val color = if (enabled) EnabledColor else DisabledColor
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = if (enabled) "Enabled" else "Disabled",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun SyncPolicyDetailDialog(
    isLoading: Boolean,
    policy: SyncPolicyResponse?,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(policy?.name ?: "Sync policy") },
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
                policy != null -> {
                    Column {
                        DetailRow("Status", if (policy.enabled) "Enabled" else "Disabled")
                        DetailRow("Mode", policy.replicationMode)
                        DetailRow("Priority", policy.priority.toString())
                        DetailRow("Precedence", policy.precedence.toString())
                        if (policy.filter.isNotBlank()) {
                            DetailRow("Filter", policy.filter)
                        }
                        if (policy.description.isNotBlank()) {
                            DetailRow("Description", policy.description)
                        }
                        DetailRow("Updated", formatRelativeTime(policy.updatedAt))
                    }
                }
                else -> {
                    Text("No detail available.")
                }
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
