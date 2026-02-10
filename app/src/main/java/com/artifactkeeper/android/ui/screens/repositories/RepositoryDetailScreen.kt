package com.artifactkeeper.android.ui.screens.repositories

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.Artifact
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.ui.util.formatBytes
import com.artifactkeeper.android.ui.util.formatDownloadCount
import com.artifactkeeper.android.ui.util.formatRelativeTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryDetailScreen(
    repoKey: String,
    onBack: () -> Unit,
    onArtifactSecurityClick: (id: String, name: String) -> Unit = { _, _ -> },
    onNavigateToMembers: ((repoKey: String, repoName: String, repoFormat: String) -> Unit)? = null,
) {
    var repository by remember { mutableStateOf<Repository?>(null) }
    var artifacts by remember { mutableStateOf<List<Artifact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun loadData(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                val repo = ApiClient.reposApi.getRepository(repoKey).unwrap()
                repository = repo
                val response = ApiClient.reposApi.listArtifacts(
                    key = repoKey,
                    q = searchQuery.ifBlank { null },
                ).unwrap()
                artifacts = response.items
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load repository"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }
    LaunchedEffect(searchQuery) { loadData() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(repository?.name ?: repoKey) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Repository metadata header
                        repository?.let { repo ->
                            item(key = "header") {
                                RepoDetailHeader(repo)
                            }

                            // Members card for virtual repositories
                            if (repo.repoType.equals("virtual", ignoreCase = true)) {
                                item(key = "members") {
                                    VirtualMembersCard(
                                        onClick = {
                                            onNavigateToMembers?.invoke(repo.key, repo.name, repo.format)
                                        },
                                    )
                                }
                            }
                        }

                        // Search bar
                        item(key = "search") {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search artifacts...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                singleLine = true,
                            )
                        }

                        // Artifacts section title
                        item(key = "title") {
                            Text(
                                text = "Artifacts (${artifacts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        if (artifacts.isEmpty()) {
                            item(key = "empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No artifacts found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        items(artifacts, key = { it.id }) { artifact ->
                            ArtifactCard(
                                artifact = artifact,
                                repoKey = repoKey,
                                onSecurityClick = { onArtifactSecurityClick(artifact.id.toString(), artifact.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoDetailHeader(repo: Repository) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = repo.name,
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(repo.format.uppercase(), style = MaterialTheme.typography.labelSmall) },
                )
                AssistChip(
                    onClick = {},
                    label = { Text(repo.repoType, style = MaterialTheme.typography.labelSmall) },
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (repo.isPublic) "Public" else "Private",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }

            repo.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatBytes(repo.storageUsedBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatRelativeTime(repo.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ArtifactCard(artifact: Artifact, repoKey: String, onSecurityClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val downloadUrl = "${ApiClient.baseUrl}api/v1/repositories/$repoKey/artifacts/${artifact.path}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                context.startActivity(intent)
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = artifact.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artifact.version != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("v${artifact.version}", style = MaterialTheme.typography.labelSmall) },
                    )
                }
                IconButton(
                    onClick = onSecurityClick,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "SBOM & Security",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = artifact.contentType ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Downloads",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDownloadCount(artifact.downloadCount.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatBytes(artifact.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatRelativeTime(artifact.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VirtualMembersCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Groups,
                contentDescription = "Members",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Member Repositories",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Manage local and remote repositories included in this virtual repository",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
