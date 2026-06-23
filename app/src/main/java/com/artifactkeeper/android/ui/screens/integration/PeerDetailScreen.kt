@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.artifactkeeper.client.models.PeerInstanceResponse
import com.artifactkeeper.client.models.SyncTaskResponse
import java.util.UUID

private val OnlineColor = Color(0xFF52C41A)
private val OfflineColor = Color(0xFFF5222D)
private val PendingColor = Color(0xFFFAAD14)

private fun peerStatusColor(status: String): Color = when (status.lowercase()) {
    "online", "active", "healthy", "connected" -> OnlineColor
    "offline", "unreachable", "error" -> OfflineColor
    else -> PendingColor
}

/**
 * Detail for a single peer instance: status and cache usage, a Sync now action,
 * its assigned repositories (each syncable), and its recent sync tasks.
 */
@Composable
fun PeerDetailScreen(
    peerId: String,
    onBack: () -> Unit,
    viewModel: PeerDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val parsedId = remember(peerId) { runCatching { UUID.fromString(peerId) }.getOrNull() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(parsedId) {
        parsedId?.let { viewModel.load(it) }
    }

    LaunchedEffect(state.message, state.error, state.peer) {
        val text = state.message ?: state.error?.takeIf { state.peer != null }
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TopAppBar(
                title = { Text(state.peer?.name ?: "Peer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { parsedId?.let { viewModel.triggerSync(it) } },
                        enabled = parsedId != null && !state.isMutating,
                    ) { Text("Sync now") }
                },
            )

            when {
                parsedId == null -> CenteredText("Invalid peer id")
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.peer == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.load(parsedId) }) { Text("Retry") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.peer?.let { peer ->
                            item { PeerStatusCard(peer) }
                        }

                        if (state.assignedRepoIds.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Assigned repositories (${state.assignedRepoIds.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            items(state.assignedRepoIds, key = { it }) { repoId ->
                                AssignedRepoCard(
                                    repoId = repoId,
                                    isMutating = state.isMutating,
                                    onSyncNow = {
                                        parsedId?.let { viewModel.runSubscriptionNow(it, repoId) }
                                    },
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Sync tasks (${state.syncTasks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (state.syncTasks.isEmpty()) {
                            item {
                                Text(
                                    text = "No active sync tasks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(state.syncTasks, key = { it.id }) { task ->
                                SyncTaskCard(task)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerStatusCard(peer: PeerInstanceResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = peer.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = peer.endpointUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PeerStatusBadge(peer.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cache ${"%.0f".format(peer.cacheUsagePercent)}% used  -  ${peer.region ?: "no region"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            peer.lastSyncAt?.let {
                Text(
                    text = "Last sync ${it.toLocalDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            peer.lastHeartbeatAt?.let {
                Text(
                    text = "Last heartbeat ${it.toLocalDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PeerStatusBadge(status: String) {
    val color = peerStatusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun AssignedRepoCard(repoId: UUID, isMutating: Boolean, onSyncNow: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = repoId.toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onSyncNow, enabled = !isMutating) { Text("Sync now") }
        }
    }
}

@Composable
private fun SyncTaskCard(task: SyncTaskResponse) {
    val color = peerStatusColor(task.status)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.storageKey,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "priority ${task.priority}  -  ${task.createdAt.toLocalDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = task.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
