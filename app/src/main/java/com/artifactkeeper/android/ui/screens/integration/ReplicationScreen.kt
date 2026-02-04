@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.AssignRepoRequest
import com.artifactkeeper.android.data.models.PeerConnection
import com.artifactkeeper.android.data.models.PeerInstance
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.ui.util.formatBytes
import com.artifactkeeper.android.ui.util.formatRelativeTime
import kotlinx.coroutines.launch

private val ReplicationOnline = Color(0xFF52C41A)
private val ReplicationOffline = Color(0xFFF5222D)
private val ReplicationSyncing = Color(0xFFFA8C16)

@Composable
fun ReplicationScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var peers by remember { mutableStateOf<List<PeerInstance>>(emptyList()) }
    var repositories by remember { mutableStateOf<List<Repository>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Overview", "Subscriptions", "Topology")

    fun loadData(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                peers = ApiClient.api.listPeers().items
                repositories = ApiClient.api.listRepositories(perPage = 100).items
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load replication data"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Replication") })

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { loadData() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { loadData(refresh = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (selectedTab) {
                        0 -> ReplicationOverviewTab(peers)
                        1 -> ReplicationSubscriptionsTab(peers, repositories)
                        2 -> ReplicationTopologyTab(peers)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplicationOverviewTab(peers: List<PeerInstance>) {
    val onlineCount = peers.count { it.status.lowercase() == "online" }
    val syncingCount = peers.count { it.status.lowercase() == "syncing" }
    val avgCacheUsage = if (peers.isNotEmpty()) {
        peers.sumOf { it.cacheUsagePercent } / peers.size
    } else {
        0.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReplicationStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Peers",
                    value = peers.size.toString(),
                )
                ReplicationStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Online",
                    value = onlineCount.toString(),
                    valueColor = ReplicationOnline,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReplicationStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Syncing",
                    value = syncingCount.toString(),
                    valueColor = ReplicationSyncing,
                )
                ReplicationStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Cache Usage",
                    value = "${avgCacheUsage.toInt()}%",
                )
            }
        }

        items(peers, key = { it.id }) { peer ->
            ReplicationPeerCard(peer)
        }

        if (peers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No peers registered",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplicationStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReplicationPeerCard(peer: PeerInstance) {
    val statusColor = when (peer.status.lowercase()) {
        "online" -> ReplicationOnline
        "syncing" -> ReplicationSyncing
        else -> ReplicationOffline
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = peer.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = peer.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = peer.endpointUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (peer.region != null) {
                Text(
                    text = "Region: ${peer.region}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (peer.cacheUsagePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (peer.cacheUsagePercent > 90) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatBytes(peer.cacheUsedBytes)} / ${formatBytes(peer.cacheSizeBytes)} (${peer.cacheUsagePercent.toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (peer.lastSyncAt != null) {
                    Text(
                        text = "Last sync: ${formatRelativeTime(peer.lastSyncAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (peer.lastHeartbeatAt != null) {
                    Text(
                        text = "Heartbeat: ${formatRelativeTime(peer.lastHeartbeatAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplicationSubscriptionsTab(
    peers: List<PeerInstance>,
    repositories: List<Repository>,
) {
    var selectedPeerId by remember { mutableStateOf(peers.firstOrNull()?.id) }
    var assignedRepoIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingRepos by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedPeerId) {
        val peerId = selectedPeerId ?: return@LaunchedEffect
        isLoadingRepos = true
        try {
            val repoIds = ApiClient.api.getPeerRepositories(peerId)
            assignedRepoIds = repoIds.toSet()
        } catch (_: Exception) {
            assignedRepoIds = emptySet()
        } finally {
            isLoadingRepos = false
        }
    }

    val selectedPeer = peers.find { it.id == selectedPeerId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedPeer?.name ?: "Select a peer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Peer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    peers.forEach { peer ->
                        DropdownMenuItem(
                            text = { Text(peer.name) },
                            onClick = {
                                selectedPeerId = peer.id
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        if (selectedPeerId == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Select a peer to manage subscriptions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (isLoadingRepos) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(repositories, key = { it.id }) { repo ->
                val isAssigned = repo.id in assignedRepoIds

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = repo.name,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = repo.format.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                            Text(
                                text = repo.key,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (isAssigned) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ReplicationOnline.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "Assigned",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ReplicationOnline,
                                )
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    val peerId = selectedPeerId ?: return@TextButton
                                    coroutineScope.launch {
                                        try {
                                            ApiClient.api.assignPeerRepository(
                                                peerId,
                                                AssignRepoRequest(
                                                    repositoryId = repo.id,
                                                    replicationMode = "pull",
                                                )
                                            )
                                            assignedRepoIds = assignedRepoIds + repo.id
                                        } catch (_: Exception) {
                                            // Silently handle
                                        }
                                    }
                                },
                            ) {
                                Text("Assign")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplicationTopologyTab(peers: List<PeerInstance>) {
    var selectedPeerId by remember { mutableStateOf(peers.firstOrNull()?.id) }
    var connections by remember { mutableStateOf<List<PeerConnection>>(emptyList()) }
    var isLoadingConnections by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val peerMap = remember(peers) { peers.associateBy { it.id } }

    LaunchedEffect(selectedPeerId) {
        val peerId = selectedPeerId ?: return@LaunchedEffect
        isLoadingConnections = true
        try {
            connections = ApiClient.api.getPeerConnections(peerId)
        } catch (_: Exception) {
            connections = emptyList()
        } finally {
            isLoadingConnections = false
        }
    }

    val selectedPeer = peers.find { it.id == selectedPeerId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedPeer?.name ?: "Select a peer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Peer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    peers.forEach { peer ->
                        DropdownMenuItem(
                            text = { Text(peer.name) },
                            onClick = {
                                selectedPeerId = peer.id
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        if (selectedPeerId == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Select a peer to view topology",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (isLoadingConnections) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (connections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No connections found for this peer",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(connections, key = { it.id }) { connection ->
                ReplicationConnectionCard(connection, peerMap)
            }
        }
    }
}

@Composable
private fun ReplicationConnectionCard(
    connection: PeerConnection,
    peerMap: Map<String, PeerInstance>,
) {
    val statusColor = when (connection.status.lowercase()) {
        "connected" -> ReplicationOnline
        "disconnected" -> ReplicationOffline
        else -> ReplicationSyncing
    }
    val targetPeer = peerMap[connection.targetPeerId]

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = targetPeer?.name ?: connection.targetPeerId,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = connection.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (connection.latencyMs != null) {
                    Text(
                        text = "Latency: ${connection.latencyMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (connection.bandwidthEstimateBps != null) {
                    Text(
                        text = "Bandwidth: ${formatReplicationBandwidth(connection.bandwidthEstimateBps)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Shared: ${connection.sharedArtifactsCount} artifacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Transferred: ${formatBytes(connection.bytesTransferredTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Success: ${connection.transferSuccessCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ReplicationOnline,
                )
                Text(
                    text = "Failures: ${connection.transferFailureCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (connection.transferFailureCount > 0) ReplicationOffline
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatReplicationBandwidth(bps: Long): String {
    return when {
        bps < 1_000 -> "$bps bps"
        bps < 1_000_000 -> "%.1f Kbps".format(bps / 1_000.0)
        bps < 1_000_000_000 -> "%.1f Mbps".format(bps / 1_000_000.0)
        else -> "%.1f Gbps".format(bps / 1_000_000_000.0)
    }
}
