@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.client.models.AssessmentResult
import com.artifactkeeper.client.models.MigrationItemResponse
import com.artifactkeeper.client.models.MigrationJobResponse
import com.artifactkeeper.android.ui.util.formatBytes
import java.util.UUID

/**
 * Detail for a single migration job: progress and counts, operate actions
 * (assess, start, pause, resume, cancel) gated by status, the latest
 * assessment, and the job's items.
 */
@Composable
fun MigrationDetailScreen(
    jobId: String,
    onBack: () -> Unit,
    viewModel: MigrationViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsState()
    val parsedId = remember(jobId) { runCatching { UUID.fromString(jobId) }.getOrNull() }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    LaunchedEffect(parsedId) {
        parsedId?.let { viewModel.loadDetail(it) }
    }

    LaunchedEffect(state.message, state.error, state.job) {
        val text = state.message ?: state.error?.takeIf { state.job != null }
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TopAppBar(
                title = { Text(state.job?.jobType ?: "Migration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )

            when {
                parsedId == null -> CenteredMigrationText("Invalid migration id")
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.job == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.loadDetail(parsedId) }) { Text("Retry") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.job?.let { job ->
                            item { MigrationProgressCard(job) }
                            item {
                                MigrationActions(
                                    status = job.status,
                                    isMutating = state.isMutating,
                                    onAssess = {
                                        pendingAction = "Run assessment for this job?" to {
                                            parsedId?.let { viewModel.assess(it) }
                                        }
                                    },
                                    onStart = {
                                        pendingAction = "Start this migration?" to {
                                            parsedId?.let { viewModel.start(it) }
                                        }
                                    },
                                    onPause = {
                                        pendingAction = "Pause this migration?" to {
                                            parsedId?.let { viewModel.pause(it) }
                                        }
                                    },
                                    onResume = {
                                        pendingAction = "Resume this migration?" to {
                                            parsedId?.let { viewModel.resume(it) }
                                        }
                                    },
                                    onCancel = {
                                        pendingAction = "Cancel this migration? This cannot be undone." to {
                                            parsedId?.let { viewModel.cancel(it) }
                                        }
                                    },
                                    onLoadAssessment = { parsedId?.let { viewModel.loadAssessment(it) } },
                                )
                            }
                        }

                        state.assessment?.let { assessment ->
                            item { AssessmentCard(assessment) }
                        }

                        item {
                            Text(
                                text = "Items (${state.items.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (state.items.isEmpty()) {
                            item {
                                Text(
                                    text = "No items recorded for this job.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(state.items, key = { it.id }) { item ->
                                MigrationItemCard(item)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingAction?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text("Confirm") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    action()
                    pendingAction = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MigrationProgressCard(job: MigrationJobResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = job.jobType, style = MaterialTheme.typography.titleMedium)
                MigrationStatusBadge(job.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (job.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${job.completedItems}/${job.totalItems} items - ${"%.0f".format(job.progressPercent)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatBytes(job.transferredBytes)} of ${formatBytes(job.totalBytes)} transferred",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (job.failedItems > 0 || job.skippedItems > 0) {
                Text(
                    text = "${job.failedItems} failed, ${job.skippedItems} skipped",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            job.errorSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MigrationActions(
    status: String,
    isMutating: Boolean,
    onAssess: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onLoadAssessment: () -> Unit,
) {
    val s = status.lowercase()
    val canStart = s in setOf("pending", "assessed", "ready", "created")
    val canPause = s in setOf("running", "in_progress", "started")
    val canResume = s == "paused"
    val canCancel = s in setOf("pending", "running", "in_progress", "started", "paused", "assessed", "ready")

    FlowRowActions {
        TextButton(onClick = onAssess, enabled = !isMutating) { Text("Assess") }
        TextButton(onClick = onLoadAssessment, enabled = !isMutating) { Text("View assessment") }
        if (canStart) TextButton(onClick = onStart, enabled = !isMutating) { Text("Start") }
        if (canPause) TextButton(onClick = onPause, enabled = !isMutating) { Text("Pause") }
        if (canResume) TextButton(onClick = onResume, enabled = !isMutating) { Text("Resume") }
        if (canCancel) {
            TextButton(
                onClick = onCancel,
                enabled = !isMutating,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Cancel") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowActions(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun AssessmentCard(assessment: AssessmentResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Assessment (${assessment.status})",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${assessment.totalArtifacts} artifacts, ${formatBytes(assessment.totalSizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${assessment.repositories.size} repositories, ${assessment.usersCount} users, " +
                    "${assessment.groupsCount} groups, ${assessment.permissionsCount} permissions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (assessment.blockers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Blockers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                assessment.blockers.forEach { blocker ->
                    Text(
                        text = "- $blocker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (assessment.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Warnings", style = MaterialTheme.typography.labelMedium)
                assessment.warnings.forEach { warning ->
                    Text(
                        text = "- $warning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MigrationItemCard(item: MigrationItemResponse) {
    val color = migrationStatusColor(item.status)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.sourcePath,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.itemType} - ${formatBytes(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.errorMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = item.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun CenteredMigrationText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
