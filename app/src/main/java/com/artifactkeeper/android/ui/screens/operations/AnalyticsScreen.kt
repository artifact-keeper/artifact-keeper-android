@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.AdminStats
import com.artifactkeeper.android.data.models.StorageBreakdownItem
import com.artifactkeeper.android.ui.util.formatBytes
import com.artifactkeeper.android.ui.util.formatDownloadCount
import kotlinx.coroutines.launch

@Composable
fun AnalyticsScreen() {
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var storageBreakdown by remember { mutableStateOf<List<StorageBreakdownItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadData(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                stats = ApiClient.api.getAdminStats()
                storageBreakdown = ApiClient.api.getStorageBreakdown()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load analytics"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Analytics") })

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
                val currentStats = stats ?: return

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { loadData(refresh = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Stat cards row 1
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AnalyticsStatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Folder,
                                    label = "Repositories",
                                    value = currentStats.totalRepositories.toString(),
                                )
                                AnalyticsStatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Inventory2,
                                    label = "Artifacts",
                                    value = currentStats.totalArtifacts.toString(),
                                )
                            }
                        }

                        // Stat cards row 2
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AnalyticsStatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.People,
                                    label = "Users",
                                    value = currentStats.totalUsers.toString(),
                                )
                                AnalyticsStatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Storage,
                                    label = "Storage",
                                    value = formatBytes(currentStats.totalStorageBytes),
                                )
                            }
                        }

                        // Downloads card
                        item {
                            AnalyticsStatCard(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.CloudDownload,
                                label = "Total Downloads",
                                value = formatDownloadCount(currentStats.totalDownloads),
                            )
                        }

                        // Storage breakdown header
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Storage Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        if (storageBreakdown.isEmpty()) {
                            item {
                                Text(
                                    text = "No storage data available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        items(storageBreakdown, key = { it.repositoryId }) { item ->
                            StorageBreakdownCard(item)
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
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
private fun StorageBreakdownCard(item: StorageBreakdownItem) {
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
                        text = item.repositoryKey,
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
                            text = item.format.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    text = "${item.artifactCount} artifacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = formatBytes(item.storageBytes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
