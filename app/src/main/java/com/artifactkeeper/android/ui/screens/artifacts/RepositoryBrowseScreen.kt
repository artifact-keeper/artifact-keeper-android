package com.artifactkeeper.android.ui.screens.artifacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artifactkeeper.android.ui.components.LoadingErrorContainer
import com.artifactkeeper.android.ui.util.formatBytes
import com.artifactkeeper.android.ui.util.formatRelativeTime
import com.artifactkeeper.client.models.ArtifactResponse
import com.artifactkeeper.client.models.TreeNodeResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryBrowseScreen(
    repoKey: String,
    onBack: () -> Unit,
    viewModel: RepositoryBrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<TreeNodeResponse?>(null) }

    LaunchedEffect(repoKey) { viewModel.load(repoKey) }

    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repoKey) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Breadcrumbs(
                breadcrumbs = state.breadcrumbs,
                onRoot = { viewModel.navigateToRoot() },
                onCrumb = { viewModel.navigateToCrumb(it) },
            )
            HorizontalDivider()

            LoadingErrorContainer(
                isLoading = state.isLoading,
                error = state.error,
                onRetry = { viewModel.load(repoKey, state.currentPath) },
            ) {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.load(repoKey, state.currentPath, refresh = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.nodes.isEmpty()) {
                        EmptyFolder()
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.nodes, key = { it.id }) { node ->
                                NodeRow(
                                    node = node,
                                    onClick = {
                                        if (node.type == "folder") viewModel.openFolder(node)
                                        else viewModel.openFile(node)
                                    },
                                    onDelete = if (node.type == "folder") null else {
                                        { pendingDelete = node }
                                    },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    state.selectedFile?.let { artifact ->
        FileDetailDialog(
            artifact = artifact,
            onDismiss = { viewModel.dismissFile() },
            onDelete = {
                viewModel.dismissFile()
                pendingDelete = TreeNodeResponse(
                    hasChildren = false,
                    id = artifact.path,
                    name = artifact.name,
                    path = artifact.path,
                    type = "file",
                )
            },
        )
    }

    pendingDelete?.let { node ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete artifact") },
            text = { Text("Delete ${node.name}? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteArtifact(node.path)
                        pendingDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Breadcrumbs(
    breadcrumbs: List<BrowseCrumb>,
    onRoot: () -> Unit,
    onCrumb: (path: String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onRoot) {
            Icon(Icons.Filled.Home, contentDescription = "Repository root")
        }
        breadcrumbs.forEach { crumb ->
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onCrumb(crumb.path) }) {
                Text(crumb.name)
            }
        }
    }
}

@Composable
private fun NodeRow(
    node: TreeNodeResponse,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(node.name) },
        supportingContent = {
            if (node.type == "folder") {
                Text(node.childrenCount?.let { "$it item${if (it == 1L) "" else "s"}" } ?: "Folder")
            } else {
                Text(node.sizeBytes?.let { formatBytes(it) } ?: "File")
            }
        },
        leadingContent = {
            if (node.type == "folder") {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = "Folder",
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = "File",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete ${node.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        },
    )
}

@Composable
private fun FileDetailDialog(
    artifact: ArtifactResponse,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(artifact.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaRow("Path", artifact.path)
                artifact.version?.let { MetaRow("Version", it) }
                MetaRow("Content type", artifact.contentType)
                MetaRow("Size", formatBytes(artifact.sizeBytes))
                MetaRow("Downloads", artifact.downloadCount.toString())
                MetaRow("Created", formatRelativeTime(artifact.createdAt))
                MetaRow("SHA-256", artifact.checksumSha256, monospace = true)
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun MetaRow(label: String, value: String, monospace: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

@Composable
private fun EmptyFolder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "This folder is empty",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
