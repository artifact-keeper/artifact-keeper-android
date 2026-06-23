package com.artifactkeeper.android.ui.screens.artifacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artifactkeeper.android.ui.components.LoadingErrorContainer
import com.artifactkeeper.android.ui.util.formatBytes
import com.artifactkeeper.android.ui.util.formatRelativeTime
import com.artifactkeeper.client.models.ArtifactLabelResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactDetailScreen(
    artifactId: String,
    onBack: () -> Unit,
    viewModel: ArtifactDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(artifactId) { viewModel.load(artifactId) }

    LaunchedEffect(state.labelError) {
        state.labelError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLabelError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.artifact?.name ?: "Artifact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LoadingErrorContainer(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { viewModel.load(artifactId) },
            modifier = Modifier.padding(innerPadding),
        ) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.load(artifactId, refresh = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.artifact?.let { artifact ->
                        item {
                            SectionCard(title = "Details") {
                                DetailRow("Repository", artifact.repositoryKey)
                                DetailRow("Path", artifact.path)
                                artifact.version?.let { DetailRow("Version", it) }
                                DetailRow("Content type", artifact.contentType)
                                DetailRow("Size", formatBytes(artifact.sizeBytes))
                                DetailRow("Downloads", artifact.downloadCount.toString())
                                DetailRow("Created", formatRelativeTime(artifact.createdAt))
                                DetailRow("SHA-256", artifact.checksumSha256, monospace = true)
                            }
                        }
                    }

                    state.stats?.let { stats ->
                        item {
                            SectionCard(title = "Download stats") {
                                DetailRow("Total downloads", stats.downloadCount.toString())
                                stats.firstDownloaded?.let { DetailRow("First downloaded", formatRelativeTime(it)) }
                                stats.lastDownloaded?.let { DetailRow("Last downloaded", formatRelativeTime(it)) }
                            }
                        }
                    }

                    state.metadata?.let { metadata ->
                        item {
                            SectionCard(title = "Metadata") {
                                DetailRow("Format", metadata.format)
                            }
                        }
                    }

                    item {
                        LabelsCard(
                            labels = state.labels,
                            isMutating = state.isLabelMutating,
                            onAdd = { key, value -> viewModel.addLabel(key, value) },
                            onDelete = { viewModel.deleteLabel(it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun LabelsCard(
    labels: List<ArtifactLabelResponse>,
    isMutating: Boolean,
    onAdd: (key: String, value: String?) -> Unit,
    onDelete: (key: String) -> Unit,
) {
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Labels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (labels.isEmpty()) {
                Text(
                    text = "No labels yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                labels.forEach { label ->
                    AssistChip(
                        onClick = { },
                        label = { Text(labelText(label)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onDelete(label.key) },
                                enabled = !isMutating,
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove ${label.key}",
                                )
                            }
                        },
                    )
                }
            }

            OutlinedTextField(
                value = newKey,
                onValueChange = { newKey = it },
                label = { Text("Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = newValue,
                onValueChange = { newValue = it },
                label = { Text("Value (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        onAdd(newKey.trim(), newValue.trim().ifBlank { null })
                        newKey = ""
                        newValue = ""
                    },
                    enabled = !isMutating && newKey.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(text = "Add label", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

private fun labelText(label: ArtifactLabelResponse): String =
    if (label.value.isBlank()) label.key else "${label.key}: ${label.value}"
