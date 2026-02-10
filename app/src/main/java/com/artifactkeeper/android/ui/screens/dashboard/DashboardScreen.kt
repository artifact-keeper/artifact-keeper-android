package com.artifactkeeper.android.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onSettingsClick: () -> Unit = {}) {
    var repoCount by remember { mutableStateOf<Long?>(null) }
    var packageCount by remember { mutableStateOf<Long?>(null) }
    var buildCount by remember { mutableStateOf<Long?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadDashboard() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                repoCount = ApiClient.reposApi.listRepositories(perPage = 1).unwrap().pagination.total
                packageCount = ApiClient.packagesApi.listPackages(perPage = 1).unwrap().pagination.total
                buildCount = ApiClient.buildsApi.listBuilds(perPage = 1).unwrap().pagination.total
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load dashboard"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadDashboard() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Artifact Keeper") },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
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
                    TextButton(onClick = { loadDashboard() }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Folder,
                        label = "Repositories",
                        value = repoCount?.toString() ?: "--",
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Inventory2,
                        label = "Packages",
                        value = packageCount?.toString() ?: "--",
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Build,
                        label = "Builds",
                        value = buildCount?.toString() ?: "--",
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Quick Links",
                    style = MaterialTheme.typography.titleMedium,
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Manage your artifact repositories, browse packages, and monitor build status from this dashboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
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
