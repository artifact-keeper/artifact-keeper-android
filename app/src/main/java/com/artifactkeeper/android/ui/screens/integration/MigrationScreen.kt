@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.client.models.MigrationJobResponse

private val RunningColor = Color(0xFF1677FF)
private val DoneColor = Color(0xFF52C41A)
private val FailedColor = Color(0xFFF5222D)
private val PausedColor = Color(0xFFFAAD14)

internal fun migrationStatusColor(status: String): Color = when (status.lowercase()) {
    "running", "in_progress", "started" -> RunningColor
    "completed", "succeeded", "done" -> DoneColor
    "failed", "cancelled", "canceled", "error" -> FailedColor
    "paused" -> PausedColor
    else -> PausedColor
}

/**
 * Migration jobs list. Tapping a job opens its detail (status, items, and
 * operate actions). Creating migrations and connections is not exposed here.
 */
@Composable
fun MigrationScreen(
    onJobClick: (String) -> Unit,
    viewModel: MigrationViewModel = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadJobs() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.jobs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadJobs(refresh = true) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            state.jobs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No migration jobs.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.jobs, key = { it.id }) { job ->
                        MigrationJobCard(job = job, onClick = { onJobClick(job.id.toString()) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MigrationJobCard(job: MigrationJobResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = job.jobType,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MigrationStatusBadge(job.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (job.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${job.completedItems}/${job.totalItems} items  -  ${"%.0f".format(job.progressPercent)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (job.failedItems > 0) {
                Text(
                    text = "${job.failedItems} failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun MigrationStatusBadge(status: String) {
    val color = migrationStatusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
