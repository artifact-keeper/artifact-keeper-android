package com.artifactkeeper.android.ui.screens.staging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artifactkeeper.android.ui.components.EmptyState
import com.artifactkeeper.android.ui.components.ItemTitleWithChip
import com.artifactkeeper.android.ui.components.LoadingErrorContainer
import com.artifactkeeper.android.data.models.StagingRepository
import com.artifactkeeper.android.ui.util.formatBytes

// Policy status colors matching web UI
private val StatusPassing = Color(0xFF52C41A)
private val StatusFailing = Color(0xFFF5222D)
private val StatusWarning = Color(0xFFFAAD14)
private val StatusPending = Color(0xFF8C8C8C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagingListScreen(
    viewModel: StagingViewModel = viewModel(),
    onRepoClick: (StagingRepository) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStagingRepos()
    }

    LaunchedEffect(uiState.isLoadingRepos) {
        if (!uiState.isLoadingRepos) {
            isRefreshing = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LoadingErrorContainer(
            isLoading = uiState.isLoadingRepos && uiState.stagingRepos.isEmpty(),
            error = if (uiState.stagingRepos.isEmpty()) uiState.reposError else null,
            onRetry = { viewModel.loadStagingRepos() },
            emptyState = EmptyState(
                isEmpty = uiState.stagingRepos.isEmpty(),
                content = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No staging repositories",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Staging repositories allow you to review and promote artifacts before release",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                        }
                    }
                },
            ),
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.loadStagingRepos()
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.stagingRepos, key = { it.id }) { repo ->
                        StagingRepoCard(
                            repo = repo,
                            onClick = { onRepoClick(repo) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StagingRepoCard(
    repo: StagingRepository,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ItemTitleWithChip(title = repo.name, chipLabel = repo.format.uppercase())

            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Policy status summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PolicyStatusPill(
                    icon = Icons.Default.CheckCircle,
                    count = repo.passingCount,
                    color = StatusPassing,
                    label = "Passing",
                )
                PolicyStatusPill(
                    icon = Icons.Default.Cancel,
                    count = repo.failingCount,
                    color = StatusFailing,
                    label = "Failing",
                )
                PolicyStatusPill(
                    icon = Icons.Default.Schedule,
                    count = repo.pendingCount,
                    color = StatusPending,
                    label = "Pending",
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${repo.artifactCount} artifact${if (repo.artifactCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatBytes(repo.storageUsedBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (repo.targetRepositoryKey != null) {
                    Text(
                        text = "-> ${repo.targetRepositoryKey}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyStatusPill(
    icon: ImageVector,
    count: Int,
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
