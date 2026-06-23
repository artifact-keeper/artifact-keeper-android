package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.client.models.FindingResponse
import com.artifactkeeper.client.models.ScanResponse
import java.util.UUID

private val FixedVersionGreen = Color(0xFF52C41A)

/**
 * Detail for a single scan: a summary header plus the list of vulnerabilities
 * and CVEs found, ordered most-severe first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    scanId: String,
    onBack: () -> Unit,
    viewModel: ArtifactSecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.scanDetailState.collectAsState()
    val parsedId = remember(scanId) { runCatching { UUID.fromString(scanId) }.getOrNull() }

    LaunchedEffect(parsedId) {
        parsedId?.let { viewModel.loadScanDetail(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Scan Detail") },
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
            parsedId == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Invalid scan id",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
                        TextButton(onClick = { viewModel.loadScanDetail(parsedId) }) {
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
                    state.scan?.let { scan ->
                        item { ScanSummaryCard(scan) }
                    }

                    item {
                        Text(
                            text = "Findings (${state.findings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    if (state.findings.isEmpty()) {
                        item {
                            Text(
                                text = "No findings for this scan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.findings, key = { it.id }) { finding ->
                            FindingDetailCard(finding)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanSummaryCard(scan: ScanResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = artifactScanTypeIcon(scan.scanType),
                        contentDescription = scan.scanType,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = scan.scanType.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                ArtifactStatusBadge(scan.status)
            }

            scan.artifactName?.takeIf { it.isNotBlank() }?.let { name ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append(name)
                        scan.artifactVersion?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ArtifactSeverityRow(
                critical = scan.criticalCount,
                high = scan.highCount,
                medium = scan.mediumCount,
                low = scan.lowCount,
            )

            scan.errorMessage?.takeIf { it.isNotBlank() }?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FindingDetailCard(finding: FindingResponse) {
    val uriHandler = LocalUriHandler.current
    val sevColor = severityColor(finding.severity)

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
                        text = finding.severity.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = sevColor,
                    )
                }

                if (finding.isAcknowledged) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Acknowledged",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Acknowledged",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = finding.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            finding.cveId?.takeIf { it.isNotBlank() }?.let { cve ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cve,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://nvd.nist.gov/vuln/detail/$cve")
                    },
                )
            }

            val hasComponentInfo = !finding.affectedComponent.isNullOrBlank() ||
                !finding.affectedVersion.isNullOrBlank() ||
                !finding.fixedVersion.isNullOrBlank()
            if (hasComponentInfo) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    finding.affectedComponent?.takeIf { it.isNotBlank() }?.let { comp ->
                        Text(
                            text = comp,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    finding.affectedVersion?.takeIf { it.isNotBlank() }?.let { ver ->
                        Text(
                            text = ver,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Critical,
                        )
                    }
                    finding.fixedVersion?.takeIf { it.isNotBlank() }?.let { fixed ->
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "fixed in",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = fixed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = FixedVersionGreen,
                        )
                    }
                }
            }

            finding.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun severityColor(severity: String): Color = when (severity.lowercase()) {
    "critical" -> Critical
    "high" -> High
    "medium" -> Medium
    "low" -> Low
    else -> Low
}
