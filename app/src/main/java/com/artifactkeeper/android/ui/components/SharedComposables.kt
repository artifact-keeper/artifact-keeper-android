package com.artifactkeeper.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Configuration for how the container should handle an empty data state.
 * Extracted as a data class to keep the LoadingErrorContainer parameter count at 7.
 */
data class EmptyState(
    val isEmpty: Boolean = false,
    val message: String = "No data available",
    val content: (@Composable () -> Unit)? = null,
)

/**
 * Displays a centered loading indicator, error state with retry, or the provided content.
 * Used across many screens (SecurityScreen, MonitoringScreen, RepositoriesScreen,
 * PackagesScreen, BuildsScreen, etc.) to avoid duplicating the loading/error scaffold.
 */
@Composable
fun LoadingErrorContainer(
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyState: EmptyState = EmptyState(),
    content: @Composable () -> Unit,
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
        emptyState.isEmpty -> {
            if (emptyState.content != null) {
                emptyState.content.invoke()
            } else {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emptyState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        else -> content()
    }
}

/**
 * Shared row displaying a title with a format chip on the right. Used by RepositoryCard,
 * StagingRepoCard, RepoSearchResultCard, ArtifactSearchResultCard, and PackageCard.
 */
@Composable
fun ItemTitleWithChip(
    title: String,
    chipLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        AssistChip(
            onClick = {},
            label = { Text(chipLabel, style = MaterialTheme.typography.labelSmall) },
        )
    }
}

/**
 * Shared footer row showing two text values on opposite sides with a divider above.
 * Used in RepositoryCard (RepositoriesScreen) and RepoDetailHeader (RepositoryDetailScreen).
 */
@Composable
fun MetadataFooterRow(
    startText: String,
    endText: String,
) {
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = startText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = endText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
