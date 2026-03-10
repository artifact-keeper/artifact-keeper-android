package com.artifactkeeper.android.ui.screens.builds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.BuildItem
import com.artifactkeeper.android.ui.components.EmptyState
import com.artifactkeeper.android.ui.components.LoadingErrorContainer
import kotlinx.coroutines.launch

private val StatusSuccess = Color(0xFF52C41A)
private val StatusRunning = Color(0xFF1890FF)
private val StatusFailed = Color(0xFFF5222D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildDetailScreen(
    buildId: String,
    onBack: () -> Unit,
) {
    var build by remember { mutableStateOf<BuildItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val id = java.util.UUID.fromString(buildId)
                build = ApiClient.buildsApi.getBuild(id).unwrap()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load build details"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(build?.name ?: "Build Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LoadingErrorContainer(
            isLoading = isLoading,
            error = errorMessage,
            onRetry = { loadData() },
            modifier = Modifier.padding(innerPadding),
            emptyState = EmptyState(isEmpty = build == null, message = "Build not found"),
        ) {
            val b = build ?: return@LoadingErrorContainer
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                    // Build overview card
                    item(key = "overview") {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${b.name} #${b.number}",
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                    val statusColor = when (b.status.lowercase()) {
                                        "success", "completed" -> StatusSuccess
                                        "running", "pending" -> StatusRunning
                                        else -> StatusFailed
                                    }
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                b.status.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Details card
                    item(key = "details") {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )

                                DetailRow("Build Number", "#${b.number}")

                                b.agent?.let { DetailRow("Agent", it) }

                                b.durationMs?.let { ms ->
                                    val seconds = ms / 1000
                                    val display = if (seconds >= 60) {
                                        "${seconds / 60}m ${seconds % 60}s"
                                    } else {
                                        "${seconds}s"
                                    }
                                    DetailRow("Duration", display)
                                }

                                b.artifactCount?.let { DetailRow("Artifacts", it.toString()) }

                                DetailRow("Created", b.createdAt.toString().take(19).replace("T", " "))

                                b.startedAt?.let {
                                    DetailRow("Started", it.toString().take(19).replace("T", " "))
                                }
                                b.finishedAt?.let {
                                    DetailRow("Finished", it.toString().take(19).replace("T", " "))
                                }
                            }
                        }
                    }

                    // VCS info card (if any VCS data exists)
                    if (b.vcsBranch != null || b.vcsRevision != null || b.vcsUrl != null) {
                        item(key = "vcs") {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "Source Control",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )

                                    b.vcsBranch?.let { DetailRow("Branch", it) }
                                    b.vcsRevision?.let {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "Revision",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = it.take(12),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                        }
                                    }
                                    b.vcsUrl?.let { DetailRow("Repository", it) }
                                    b.vcsMessage?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Modules card
                    if (!b.modules.isNullOrEmpty()) {
                        item(key = "modules-header") {
                            Text(
                                text = "Modules (${b.modules!!.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                    items(b.modules!!, key = { it.id.toString() }) { module ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = module.id.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
