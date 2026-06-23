package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
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
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.client.models.DtComponentFull
import com.artifactkeeper.client.models.DtFinding
import com.artifactkeeper.client.models.DtPolicyFull
import com.artifactkeeper.client.models.DtPolicyViolation
import com.artifactkeeper.client.models.DtProject
import com.artifactkeeper.client.models.DtProjectMetrics

private val ViolationColor = Color(0xFFFA8C16)

/**
 * Lists the Dependency-Track projects tracked by the server. Tapping a project
 * opens [DtProjectDetailScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependencyTrackScreen(
    onProjectClick: (uuid: String, name: String) -> Unit,
    viewModel: DependencyTrackViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val policiesState by viewModel.policiesState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
        viewModel.loadPolicies()
    }

    when {
        state.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.loadProjects(refresh = true) }) {
                        Text("Retry")
                    }
                }
            }
        }
        state.projects.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No Dependency-Track projects",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (policiesState.policies.isNotEmpty()) {
                    item { DtPoliciesCard(policiesState.policies) }
                }
                item {
                    Text(
                        text = "Projects (${state.projects.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.projects, key = { it.uuid }) { project ->
                    DtProjectCard(
                        project = project,
                        onClick = { onProjectClick(project.uuid, project.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DtPoliciesCard(policies: List<DtPolicyFull>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Policies (${policies.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            policies.forEach { policy ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = policy.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ViolationColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = policy.violationState.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ViolationColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DtProjectCard(project: DtProject, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    project.version?.takeIf { it.isNotBlank() }?.let { ver ->
                        Text(
                            text = ver,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Detail for a single Dependency-Track project: severity metrics, the list of
 * findings (most severe first), and any policy violations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DtProjectDetailScreen(
    projectUuid: String,
    projectName: String,
    onBack: () -> Unit,
    viewModel: DependencyTrackViewModel = hiltViewModel(),
) {
    val state by viewModel.projectDetailState.collectAsState()

    LaunchedEffect(projectUuid) { viewModel.loadProjectDetail(projectUuid) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(projectName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
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
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadProjectDetail(projectUuid) }) {
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
                    state.metrics?.let { metrics ->
                        item { DtMetricsCard(metrics, state.metricsHistory) }
                    }

                    if (state.violations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Policy Violations (${state.violations.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(state.violations, key = { it.uuid }) { violation ->
                            DtViolationCard(violation)
                        }
                    }

                    item {
                        Text(
                            text = "Findings (${state.findings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (state.findings.isEmpty()) {
                        item {
                            Text(
                                text = "No findings for this project",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.findings, key = { it.vulnerability.uuid }) { finding ->
                            DtFindingCard(
                                finding = finding,
                                isUpdating = state.isUpdating,
                                onSuppress = {
                                    viewModel.updateFindingAnalysis(
                                        projectUuid = projectUuid,
                                        componentUuid = finding.component.uuid,
                                        vulnerabilityUuid = finding.vulnerability.uuid,
                                        state = "NOT_AFFECTED",
                                        suppressed = true,
                                        justification = null,
                                    )
                                },
                                onUnsuppress = {
                                    viewModel.updateFindingAnalysis(
                                        projectUuid = projectUuid,
                                        componentUuid = finding.component.uuid,
                                        vulnerabilityUuid = finding.vulnerability.uuid,
                                        state = "EXPLOITABLE",
                                        suppressed = false,
                                        justification = null,
                                    )
                                },
                            )
                        }
                    }

                    if (state.components.isNotEmpty()) {
                        item {
                            Text(
                                text = "Components (${state.components.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(state.components, key = { it.uuid }) { component ->
                            DtComponentCard(component)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DtMetricsCard(metrics: DtProjectMetrics, history: List<DtProjectMetrics> = emptyList()) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Vulnerabilities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DtSeverityPill("C", metrics.critical ?: 0, Critical)
                DtSeverityPill("H", metrics.high ?: 0, High)
                DtSeverityPill("M", metrics.medium ?: 0, Medium)
                DtSeverityPill("L", metrics.low ?: 0, Low)
            }

            val total = metrics.findingsTotal ?: 0L
            val audited = metrics.findingsAudited ?: 0L
            if (total > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { audited.toFloat() / total.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Audited $audited / $total findings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Findings trend over the recorded history window.
            if (history.size >= 2) {
                val first = history.first().findingsTotal ?: 0L
                val last = history.last().findingsTotal ?: 0L
                val delta = last - first
                val trendColor = when {
                    delta > 0 -> Critical
                    delta < 0 -> Low
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val sign = if (delta > 0) "+" else ""
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Trend over ${history.size} snapshots: $sign$delta findings",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = trendColor,
                )
            }
        }
    }
}

@Composable
private fun DtViolationCard(violation: DtPolicyViolation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ViolationColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = violation.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ViolationColor,
                    )
                }
                Text(
                    text = violation.policyCondition.policy.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    violation.component.group?.takeIf { it.isNotBlank() }?.let { append("$it:") }
                    append(violation.component.name)
                    violation.component.version?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DtFindingCard(
    finding: DtFinding,
    isUpdating: Boolean = false,
    onSuppress: () -> Unit = {},
    onUnsuppress: () -> Unit = {},
) {
    val vuln = finding.vulnerability
    val sevColor = dtSeverityColor(vuln.severity)
    val isSuppressed = finding.analysis?.isSuppressed == true

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
                        text = vuln.severity.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = sevColor,
                    )
                }
                if (finding.analysis?.isSuppressed == true) {
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
                text = "${vuln.source} ${vuln.vulnId}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            vuln.title?.takeIf { it.isNotBlank() }?.let { title ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    finding.component.group?.takeIf { it.isNotBlank() }?.let { append("$it:") }
                    append(finding.component.name)
                    finding.component.version?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            vuln.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (isSuppressed) {
                    TextButton(onClick = onUnsuppress, enabled = !isUpdating) {
                        Text("Reactivate")
                    }
                } else {
                    TextButton(onClick = onSuppress, enabled = !isUpdating) {
                        Text("Mark not affected")
                    }
                }
            }
        }
    }
}

@Composable
private fun DtComponentCard(component: DtComponentFull) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        component.group?.takeIf { it.isNotBlank() }?.let { append("$it:") }
                        append(component.name)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                component.version?.takeIf { it.isNotBlank() }?.let { ver ->
                    Text(
                        text = ver,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            component.resolvedLicense?.let { license ->
                Text(
                    text = license.licenseId ?: license.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DtSeverityPill(label: String, count: Long, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

private fun dtSeverityColor(severity: String): Color = when (severity.uppercase()) {
    "CRITICAL" -> Critical
    "HIGH" -> High
    "MEDIUM" -> Medium
    "LOW" -> Low
    else -> Low
}
