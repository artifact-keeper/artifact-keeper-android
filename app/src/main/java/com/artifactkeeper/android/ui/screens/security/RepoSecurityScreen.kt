package com.artifactkeeper.android.ui.screens.security

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.client.models.DashboardResponse
import com.artifactkeeper.client.models.ScanConfigResponse
import com.artifactkeeper.client.models.ScanResponse
import com.artifactkeeper.client.models.ScoreResponse
import com.artifactkeeper.client.models.SigningConfigResponse

private val GradeA = Color(0xFF52C41A)
private val GradeB = Color(0xFF36CFC9)
private val GradeC = Color(0xFFFAAD14)
private val GradeD = Color(0xFFFA8C16)
private val GradeF = Color(0xFFF5222D)
private val ScanCompleted = Color(0xFF52C41A)
private val ScanRunning = Color(0xFF1890FF)
private val ScanFailed = Color(0xFFF5222D)
private val ScanPending = Color(0xFFFAAD14)

/**
 * Per-repository security view: the portfolio dashboard summary, this repo's
 * security score and scan configuration, and the scans run against it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSecurityScreen(
    repoKey: String,
    repoName: String,
    onBack: () -> Unit,
    viewModel: RepoSecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()

    LaunchedEffect(repoKey) {
        viewModel.loadRepoSecurity(repoKey)
        viewModel.loadDashboard()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Repository Security")
                    Text(
                        text = repoName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                        TextButton(onClick = { viewModel.loadRepoSecurity(repoKey, refresh = true) }) {
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
                    dashboardState.dashboard?.let { dashboard ->
                        item { SecurityDashboardCard(dashboard) }
                    }

                    item {
                        Text(
                            text = "This Repository",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    state.score?.let { score ->
                        item { RepoScoreCard(score) }
                    }
                    state.config?.let { config ->
                        item { ScanConfigCard(config) }
                    }
                    state.signingConfig?.let { signing ->
                        item { SigningConfigCard(signing, state.repoPublicKey) }
                    }
                    if (state.score == null && state.config == null) {
                        item {
                            Text(
                                text = "No security data configured for this repository",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Scans (${state.scans.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (state.scans.isEmpty()) {
                        item {
                            Text(
                                text = "No scans run against this repository",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.scans, key = { it.id }) { scan ->
                            RepoScanRow(scan)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Portfolio-level security summary. Reusable from the Security dashboard tab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityDashboardCard(dashboard: DashboardResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Security Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DashboardMetric("Critical", dashboard.criticalFindings, Critical)
                DashboardMetric("High", dashboard.highFindings, High)
                DashboardMetric("Findings", dashboard.totalFindings, Medium)
                DashboardMetric("Scans", dashboard.totalScans, ScanRunning)
                DashboardMetric("Blocked", dashboard.policyViolationsBlocked, GradeD)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${dashboard.reposGradeA} grade A",
                    style = MaterialTheme.typography.bodySmall,
                    color = GradeA,
                )
                Text(
                    text = "${dashboard.reposGradeF} grade F",
                    style = MaterialTheme.typography.bodySmall,
                    color = GradeF,
                )
                Text(
                    text = "${dashboard.reposWithScanning} scanning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: Long, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
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
private fun RepoScoreCard(score: ScoreResponse) {
    val gradeColor = when (score.grade.uppercase()) {
        "A" -> GradeA
        "B" -> GradeB
        "C" -> GradeC
        "D" -> GradeD
        else -> GradeF
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Security Score",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${score.score}/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(gradeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = score.grade.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScorePill("C", score.criticalCount, Critical)
                ScorePill("H", score.highCount, High)
                ScorePill("M", score.mediumCount, Medium)
                ScorePill("L", score.lowCount, Low)
            }
        }
    }
}

@Composable
private fun ScanConfigCard(config: ScanConfigResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Scan Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConfigRow("Scanning enabled", config.scanEnabled)
            ConfigRow("Scan on upload", config.scanOnUpload)
            ConfigRow("Scan on proxy", config.scanOnProxy)
            ConfigRow("Block on policy violation", config.blockOnPolicyViolation)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Severity threshold: ${config.severityThreshold.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SigningConfigCard(config: SigningConfigResponse, publicKeyPem: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Signing",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConfigRow("Require signatures", config.requireSignatures)
            ConfigRow("Sign packages", config.signPackages)
            ConfigRow("Sign metadata", config.signMetadata)
            publicKeyPem?.takeIf { it.isNotBlank() }?.let { pem ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Repository public key",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = pem,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ConfigRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = if (enabled) "On" else "Off",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) ScanCompleted else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RepoScanRow(scan: ScanResponse) {
    val statusColor = when (scan.status.lowercase()) {
        "completed", "complete" -> ScanCompleted
        "running", "in_progress" -> ScanRunning
        "failed", "error" -> ScanFailed
        else -> ScanPending
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = scan.scanType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = scan.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScorePill("C", scan.criticalCount, Critical)
                ScorePill("H", scan.highCount, High)
                ScorePill("M", scan.mediumCount, Medium)
                ScorePill("L", scan.lowCount, Low)
            }
        }
    }
}

@Composable
private fun ScorePill(label: String, count: Int, color: Color) {
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
