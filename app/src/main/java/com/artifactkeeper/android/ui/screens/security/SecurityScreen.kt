package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.CveTrendDataPoint
import com.artifactkeeper.android.data.models.CveTrends
import com.artifactkeeper.android.data.models.DtPortfolioMetrics
import com.artifactkeeper.android.data.models.DtStatus
import com.artifactkeeper.android.data.models.RepoSecurityScore
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import kotlinx.coroutines.launch

private val GradeA = Color(0xFF52C41A)
private val GradeB = Color(0xFF36CFC9)
private val GradeC = Color(0xFFFAAD14)
private val GradeD = Color(0xFFFA8C16)
private val GradeF = Color(0xFFF5222D)

private val DtConnected = Color(0xFF52C41A)
private val DtDisconnected = Color(0xFFF5222D)
private val DtViolations = Color(0xFFFA8C16)
private val DtProjects = Color(0xFF1890FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen() {
    var scores by remember { mutableStateOf<List<RepoSecurityScore>>(emptyList()) }
    var repoMap by remember { mutableStateOf<Map<String, Repository>>(emptyMap()) }
    var cveTrends by remember { mutableStateOf<CveTrends?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var dtStatus by remember { mutableStateOf<DtStatus?>(null) }
    var dtPortfolioMetrics by remember { mutableStateOf<DtPortfolioMetrics?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadData(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                scores = ApiClient.api.getSecurityScores()
                val repos = ApiClient.api.listRepositories(perPage = 100).items
                repoMap = repos.associateBy { it.id }
                try {
                    cveTrends = ApiClient.api.getCveTrends()
                } catch (_: Exception) {
                    // CVE trends are optional
                }

                // Load Dependency-Track status (non-blocking)
                try {
                    val status = ApiClient.api.getDtStatus()
                    dtStatus = status
                    if (status.enabled && status.healthy) {
                        dtPortfolioMetrics = ApiClient.api.getDtPortfolioMetrics()
                    } else {
                        dtPortfolioMetrics = null
                    }
                } catch (_: Exception) {
                    dtStatus = null
                    dtPortfolioMetrics = null
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load security data"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize()) {
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
            scores.isEmpty() && cveTrends == null && dtStatus?.enabled != true -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No security data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                        cveTrends?.let { trends ->
                            item(key = "cve-trends") {
                                CveTrendsSummaryCard(trends)
                            }
                        }

                        // Dependency-Track section (only when enabled)
                        if (dtStatus?.enabled == true) {
                            item(key = "dt-status") {
                                DtStatusCard(dtStatus!!)
                            }

                            if (dtPortfolioMetrics != null) {
                                item(key = "dt-metrics") {
                                    DtPortfolioMetricsCard(dtPortfolioMetrics!!)
                                }

                                item(key = "dt-audit") {
                                    DtAuditProgressCard(dtPortfolioMetrics!!)
                                }
                            }
                        }

                        if (scores.isNotEmpty()) {
                            item(key = "scores-header") {
                                Text(
                                    text = "Repository Scores",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            items(scores, key = { it.id }) { score ->
                                SecurityScoreCard(score, repoMap[score.repositoryId])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DtStatusCard(status: DtStatus) {
    val isHealthy = status.healthy
    val statusColor = if (isHealthy) DtConnected else DtDisconnected
    val statusText = if (isHealthy) "Connected" else "Disconnected"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Dependency-Track",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (status.url != null) {
                    Text(
                        text = status.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DtPortfolioMetricsCard(metrics: DtPortfolioMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Portfolio Overview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DtMetricChip(
                    label = "Critical",
                    value = metrics.critical.toString(),
                    color = Critical,
                )
                DtMetricChip(
                    label = "High",
                    value = metrics.high.toString(),
                    color = High,
                )
                DtMetricChip(
                    label = "Findings",
                    value = metrics.findingsTotal.toString(),
                    color = Medium,
                )
                DtMetricChip(
                    label = "Violations",
                    value = metrics.policyViolationsTotal.toString(),
                    color = DtViolations,
                )
                DtMetricChip(
                    label = "Projects",
                    value = metrics.projects.toString(),
                    color = DtProjects,
                )
            }
        }
    }
}

@Composable
private fun DtMetricChip(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
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
private fun DtAuditProgressCard(metrics: DtPortfolioMetrics) {
    val total = metrics.findingsTotal
    val audited = metrics.findingsAudited
    val progress = if (total > 0) audited.toFloat() / total.toFloat() else 0f

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Audit Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = DtConnected,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Audited $audited / $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Risk Score: ${"%.1f".format(metrics.inheritedRiskScore)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        metrics.inheritedRiskScore >= 70 -> Critical
                        metrics.inheritedRiskScore >= 40 -> High
                        metrics.inheritedRiskScore >= 10 -> Medium
                        else -> DtConnected
                    },
                )
            }
        }
    }
}

@Composable
private fun SecurityScoreCard(score: RepoSecurityScore, repo: Repository?) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repo?.name ?: score.repositoryId,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (repo != null) {
                        Text(
                            text = repo.key,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Grade badge
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Score: ${score.score}/100",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Severity pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SeverityPill(label = "C", count = score.criticalCount, color = Critical)
                SeverityPill(label = "H", count = score.highCount, color = High)
                SeverityPill(label = "M", count = score.mediumCount, color = Medium)
                SeverityPill(label = "L", count = score.lowCount, color = Low)
            }
        }
    }
}

@Composable
private fun SeverityPill(label: String, count: Int, color: Color) {
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

// MARK: - CVE Trends Summary

@Composable
private fun CveTrendsSummaryCard(trends: CveTrends) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CVE Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Severity counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SeverityPill(label = "Critical", count = trends.criticalCount, color = Critical)
                SeverityPill(label = "High", count = trends.highCount, color = High)
                SeverityPill(label = "Medium", count = trends.mediumCount, color = Medium)
                SeverityPill(label = "Low", count = trends.lowCount, color = Low)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detected vs Resolved
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${trends.totalDetected} detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Critical,
                )
                Text(
                    text = "${trends.totalResolved} resolved",
                    style = MaterialTheme.typography.bodySmall,
                    color = GradeA,
                )
                Text(
                    text = trends.period,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Sparkline
            if (trends.trendData.size >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
                CveTrendSparkline(
                    data = trends.trendData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }
        }
    }
}

@Composable
private fun CveTrendSparkline(data: List<CveTrendDataPoint>, modifier: Modifier = Modifier) {
    val detectedColor = Critical
    val resolvedColor = GradeA

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val maxVal = data.maxOf { maxOf(it.detected, it.resolved) }.coerceAtLeast(1).toFloat()
        val stepX = size.width / (data.size - 1).toFloat()

        fun buildPath(values: List<Int>): Path {
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - (v.toFloat() / maxVal) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        drawPath(
            path = buildPath(data.map { it.detected }),
            color = detectedColor,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawPath(
            path = buildPath(data.map { it.resolved }),
            color = resolvedColor,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
