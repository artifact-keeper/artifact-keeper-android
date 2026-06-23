package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.client.models.ArtifactHealthResponse
import com.artifactkeeper.client.models.IssueResponse
import java.util.UUID

/**
 * Per-artifact quality detail: a health summary plus the issues found by quality
 * checks, each of which can be suppressed (with a reason) or unsuppressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityArtifactScreen(
    artifactId: String,
    onBack: () -> Unit,
    viewModel: QualityViewModel = hiltViewModel(),
) {
    val state by viewModel.artifactState.collectAsState()
    val parsedId = remember(artifactId) { runCatching { UUID.fromString(artifactId) }.getOrNull() }
    var issueToSuppress by remember { mutableStateOf<IssueResponse?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(parsedId) {
        parsedId?.let { viewModel.loadArtifactQuality(it) }
    }

    // Surface trigger/evaluate results without blanking the screen.
    LaunchedEffect(state.message, state.error, state.health) {
        val text = state.message ?: state.error?.takeIf { state.health != null }
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearArtifactMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        TopAppBar(
            title = { Text("Artifact Quality") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { parsedId?.let { viewModel.triggerChecks(it) } },
                    enabled = parsedId != null && !state.isMutating,
                ) { Text("Run checks") }
                TextButton(
                    onClick = { parsedId?.let { viewModel.evaluateGate(it) } },
                    enabled = parsedId != null && !state.isMutating,
                ) { Text("Evaluate") }
            },
        )

        when {
            parsedId == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Invalid artifact id",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.health == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadArtifactQuality(parsedId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.health?.let { health ->
                        item { ArtifactHealthCard(health) }
                    }

                    state.gateEvaluation?.let { eval ->
                        item { GateEvaluationCard(eval, onDismiss = { viewModel.clearGateEvaluation() }) }
                    }

                    if (state.checks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Checks (${state.checks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(state.checks, key = { it.id }) { check ->
                            QualityCheckCard(check, onClick = { viewModel.loadCheckDetail(check.id) })
                        }
                    }

                    item {
                        Text(
                            text = "Issues (${state.issues.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (state.issues.isEmpty()) {
                        item {
                            Text(
                                text = "No quality issues found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.issues, key = { it.id }) { issue ->
                            QualityIssueCard(
                                issue = issue,
                                isMutating = state.isMutating,
                                onSuppress = { issueToSuppress = issue },
                                onUnsuppress = { viewModel.unsuppressIssue(parsedId, issue.id) },
                            )
                        }
                    }
                }
            }
        }
    }
    }

    issueToSuppress?.let { issue ->
        SuppressIssueDialog(
            issueTitle = issue.title,
            isMutating = state.isMutating,
            onConfirm = { reason ->
                parsedId?.let { viewModel.suppressIssue(it, issue.id, reason) }
                issueToSuppress = null
            },
            onDismiss = { issueToSuppress = null },
        )
    }

    state.selectedCheck?.let { check ->
        CheckDetailDialog(check = check, onDismiss = { viewModel.clearSelectedCheck() })
    }
}

@Composable
private fun GateEvaluationCard(eval: com.artifactkeeper.client.models.GateEvaluationResponse, onDismiss: () -> Unit) {
    val color = if (eval.passed) gradeColor("A") else gradeColor("F")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Gate: ${eval.gateName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (eval.passed) "Passed" else "Failed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Action: ${eval.action.replaceFirstChar { it.uppercase() }}  -  Health ${eval.healthGrade} (${eval.healthScore})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            eval.violations.forEach { v ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${v.rule}: ${v.message} (expected ${v.expected}, actual ${v.actual})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun QualityCheckCard(check: com.artifactkeeper.client.models.CheckResponse, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = check.checkType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${check.issuesCount} issue${if (check.issuesCount != 1) "s" else ""}  -  ${check.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            check.score?.let { s ->
                Text(
                    text = "$s",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        s >= 80 -> gradeColor("A")
                        s >= 60 -> gradeColor("C")
                        else -> gradeColor("F")
                    },
                )
            }
        }
    }
}

@Composable
private fun CheckDetailDialog(check: com.artifactkeeper.client.models.CheckResponse, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(check.checkType.replaceFirstChar { it.uppercase() } + " check") },
        text = {
            Column {
                Text(
                    text = "Status: ${check.status}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                check.score?.let {
                    Text("Score: $it", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "Issues: ${check.issuesCount} (C ${check.criticalCount} / H ${check.highCount} / M ${check.mediumCount} / L ${check.lowCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                check.errorMessage?.takeIf { it.isNotBlank() }?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ArtifactHealthCard(health: ArtifactHealthResponse) {
    val color = gradeColor(health.healthGrade)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Health Score",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${health.healthScore}/100  -  ${health.checksPassed}/${health.checksTotal} checks passed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = health.healthGrade.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                health.qualityScore?.let { SubScorePill("Quality", it) }
                health.securityScore?.let { SubScorePill("Security", it) }
                health.licenseScore?.let { SubScorePill("License", it) }
                health.metadataScore?.let { SubScorePill("Metadata", it) }
            }
        }
    }
}

@Composable
private fun SubScorePill(label: String, score: Int) {
    val color = when {
        score >= 80 -> GradeAColor
        score >= 60 -> GradeCColor
        else -> GradeFColor
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun QualityIssueCard(
    issue: IssueResponse,
    isMutating: Boolean,
    onSuppress: () -> Unit,
    onUnsuppress: () -> Unit,
) {
    val sevColor = when (issue.severity.lowercase()) {
        "critical" -> Critical
        "high" -> High
        "medium" -> Medium
        "low" -> Low
        else -> Low
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(sevColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = issue.severity.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = sevColor,
                    )
                }
                Text(
                    text = issue.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (issue.isSuppressed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Suppressed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            issue.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            issue.location?.takeIf { it.isNotBlank() }?.let { loc ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = loc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (issue.isSuppressed) {
                    TextButton(onClick = onUnsuppress, enabled = !isMutating) { Text("Unsuppress") }
                } else {
                    TextButton(onClick = onSuppress, enabled = !isMutating) { Text("Suppress") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuppressIssueDialog(
    issueTitle: String,
    isMutating: Boolean,
    onConfirm: (reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suppress issue") },
        text = {
            Column {
                Text(
                    text = issueTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason.trim()) },
                enabled = !isMutating && reason.isNotBlank(),
            ) {
                Text("Suppress")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
