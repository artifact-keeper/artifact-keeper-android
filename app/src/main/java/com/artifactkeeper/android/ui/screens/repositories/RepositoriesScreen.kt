package com.artifactkeeper.android.ui.screens.repositories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.android.ui.components.ItemTitleWithChip
import com.artifactkeeper.android.ui.components.LoadingErrorContainer
import com.artifactkeeper.android.ui.components.MetadataFooterRow
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.ui.util.formatBytes
import com.artifactkeeper.android.ui.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoriesScreen(
    onRepoClick: (String) -> Unit = {},
    onCreateRepo: () -> Unit = {},
    viewModel: RepositoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LoadingErrorContainer(
            isLoading = uiState.isLoading,
            error = uiState.error,
            onRetry = { viewModel.loadRepositories() },
            isEmpty = uiState.repositories.isEmpty(),
            emptyMessage = "No repositories found",
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.loadRepositories(refresh = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.repositories, key = { it.id }) { repo ->
                        RepositoryCard(repo, onClick = { onRepoClick(repo.key) })
                    }
                }
            }
        }

        // Floating action button for creating new repositories
        FloatingActionButton(
            onClick = onCreateRepo,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create repository")
        }
    }
}

@Composable
private fun RepositoryCard(repo: Repository, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ItemTitleWithChip(title = repo.name, chipLabel = repo.format.uppercase())

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(repo.repoType, style = MaterialTheme.typography.labelSmall) },
                )
                if (repo.isPublic) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Public", style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            repo.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            MetadataFooterRow(
                startText = formatBytes(repo.storageUsedBytes),
                endText = formatRelativeTime(repo.createdAt),
            )
        }
    }
}
