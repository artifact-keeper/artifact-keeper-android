package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.client.models.ComponentResponse
import com.artifactkeeper.client.models.ScanResponse
import com.artifactkeeper.client.models.SbomContentResponse
import java.util.UUID

private val StatusCompleted = Color(0xFF52C41A)
private val StatusRunning = Color(0xFF1890FF)
private val StatusFailed = Color(0xFFF5222D)
private val StatusPending = Color(0xFFFAAD14)

/**
 * Per-artifact security overview: the scans that ran against the artifact and
 * its generated SBOM. Tapping a scan opens [ScanDetailScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactSecurityScreen(
    artifactId: String,
    onScanClick: (String) -> Unit,
    onBack: () -> Unit,
    onViewCves: () -> Unit = {},
    onViewQuality: () -> Unit = {},
    viewModel: ArtifactSecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val parsedId = remember(artifactId) { runCatching { UUID.fromString(artifactId) }.getOrNull() }

    LaunchedEffect(parsedId) {
        parsedId?.let { viewModel.loadArtifactSecurity(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Artifact Security") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                TextButton(onClick = onViewQuality) {
                    Text("Quality")
                }
                TextButton(onClick = onViewCves) {
                    Text("CVEs")
                }
            },
        )

        when {
            parsedId == null -> {
                CenteredMessage("Invalid artifact id")
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
                        TextButton(onClick = { viewModel.loadArtifactSecurity(parsedId, refresh = true) }) {
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
                    item {
                        SectionHeader(icon = Icons.Default.Scanner, label = "Scans")
                    }
                    if (state.scans.isEmpty()) {
                        item { EmptyHint("No scans have run against this artifact") }
                    } else {
                        items(state.scans, key = { it.id }) { scan ->
                            ArtifactScanCard(scan, onClick = { onScanClick(scan.id.toString()) })
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(icon = Icons.Default.Inventory2, label = "SBOM")
                    }
                    val sbom = state.sbom
                    if (sbom == null) {
                        item { EmptyHint("No SBOM generated for this artifact") }
                    } else {
                        item { SbomSummaryCard(sbom) }
                        if (state.components.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Components (${state.components.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            items(state.components, key = { it.id }) { component ->
                                ComponentRow(component)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ArtifactScanCard(scan: ScanResponse, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${scan.findingsCount} finding${if (scan.findingsCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
            ArtifactSeverityRow(
                critical = scan.criticalCount,
                high = scan.highCount,
                medium = scan.mediumCount,
                low = scan.lowCount,
            )
        }
    }
}

@Composable
private fun SbomSummaryCard(sbom: SbomContentResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "${sbom.format.uppercase()} ${sbom.formatVersion}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricColumn(label = "Components", value = sbom.componentCount.toString())
                MetricColumn(label = "Dependencies", value = sbom.dependencyCount.toString())
                MetricColumn(label = "Licenses", value = sbom.licenseCount.toString())
            }
            if (sbom.licenses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = sbom.licenses.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComponentRow(component: ComponentResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.name,
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
            if (component.licenses.isNotEmpty()) {
                Text(
                    text = component.licenses.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ArtifactStatusBadge(status: String) {
    val color = artifactStatusColor(status)
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

@Composable
internal fun ArtifactSeverityRow(critical: Int, high: Int, medium: Int, low: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SeverityPill(label = "C", count = critical, color = Critical)
        SeverityPill(label = "H", count = high, color = High)
        SeverityPill(label = "M", count = medium, color = Medium)
        SeverityPill(label = "L", count = low, color = Low)
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

@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

internal fun artifactScanTypeIcon(scanType: String): ImageVector = when (scanType.lowercase()) {
    "vulnerability" -> Icons.Default.BugReport
    "license" -> Icons.Default.Policy
    "malware" -> Icons.Default.Security
    "sast", "static" -> Icons.Default.FindInPage
    else -> Icons.Default.Scanner
}

internal fun artifactStatusColor(status: String): Color = when (status.lowercase()) {
    "completed", "complete" -> StatusCompleted
    "running", "in_progress" -> StatusRunning
    "failed", "error" -> StatusFailed
    "pending", "queued" -> StatusPending
    else -> StatusPending
}
