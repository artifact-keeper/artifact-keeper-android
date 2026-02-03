package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FindInPage
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
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.ScanResult
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import kotlinx.coroutines.launch

private val StatusCompleted = Color(0xFF52C41A)
private val StatusRunning = Color(0xFF1890FF)
private val StatusFailed = Color(0xFFF5222D)
private val StatusPending = Color(0xFFFAAD14)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScansScreen() {
    var scans by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var triggerMessage by remember { mutableStateOf<String?>(null) }
    var isTriggering by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun loadScans() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.listScans()
                scans = response.items
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load scans"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadScans() }

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
                        TextButton(onClick = { loadScans() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (scans.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No scans found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(scans, key = { it.id }) { scan ->
                            ScanCard(scan)
                        }
                    }
                }

                // Trigger scan footer
                HorizontalDivider()

                if (triggerMessage != null) {
                    Text(
                        text = triggerMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTriggering = true
                                triggerMessage = null
                                try {
                                    val response = ApiClient.api.triggerScan()
                                    triggerMessage = "${response.message} (${response.artifactsQueued} queued)"
                                    loadScans()
                                } catch (e: Exception) {
                                    triggerMessage = "Scan trigger failed: ${e.message}"
                                } finally {
                                    isTriggering = false
                                }
                            }
                        },
                        enabled = !isTriggering,
                    ) {
                        if (isTriggering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Trigger Scan")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanCard(scan: ScanResult) {
    val scanIcon = scanTypeIcon(scan.scanType)
    val statusColor = statusColor(scan.status)

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
                        imageVector = scanIcon,
                        contentDescription = scan.scanType,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = scan.scanType.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                // Status badge
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

            Text(
                text = "${scan.findingsCount} finding${if (scan.findingsCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Severity pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeverityPill(label = "C", count = scan.criticalCount, color = Critical)
                SeverityPill(label = "H", count = scan.highCount, color = High)
                SeverityPill(label = "M", count = scan.mediumCount, color = Medium)
                SeverityPill(label = "L", count = scan.lowCount, color = Low)
            }

            if (scan.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = scan.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
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

private fun scanTypeIcon(scanType: String): ImageVector = when (scanType.lowercase()) {
    "vulnerability" -> Icons.Default.BugReport
    "license" -> Icons.Default.Policy
    "malware" -> Icons.Default.Security
    "sast", "static" -> Icons.Default.FindInPage
    else -> Icons.Default.Scanner
}

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "completed", "complete" -> StatusCompleted
    "running", "in_progress" -> StatusRunning
    "failed", "error" -> StatusFailed
    "pending", "queued" -> StatusPending
    else -> StatusPending
}
