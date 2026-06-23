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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.client.models.CveHistoryEntry
import java.util.UUID

private val StatusOpen = Color(0xFFF5222D)
private val StatusAck = Color(0xFFFAAD14)
private val StatusFixed = Color(0xFF52C41A)

/** CVE workflow statuses offered in the status update dialog. */
private val CveStatuses = listOf("open", "acknowledged", "false_positive", "fixed")

/**
 * Lists the CVE history entries detected for an artifact, ordered most severe
 * first. Tapping a CVE opens [CveDetailScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CveTrackingScreen(
    artifactId: String,
    onCveClick: (artifactId: String, cveId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CveTrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val parsedId = remember(artifactId) { runCatching { UUID.fromString(artifactId) }.getOrNull() }

    LaunchedEffect(parsedId) {
        parsedId?.let { viewModel.loadArtifactCves(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("CVE Tracking") },
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
            parsedId == null -> CenteredText("Invalid artifact id")
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
                        TextButton(onClick = { viewModel.loadArtifactCves(parsedId, refresh = true) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            state.entries.isEmpty() -> CenteredText("No CVEs detected for this artifact")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        CveEntryCard(
                            entry = entry,
                            onClick = { onCveClick(artifactId, entry.cveId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CveEntryCard(entry: CveHistoryEntry, onClick: () -> Unit) {
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
                    entry.severity?.let { SeverityBadge(it) }
                    Text(
                        text = entry.cveId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                CveStatusBadge(entry.status)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val details = buildList {
                entry.cvssScore?.let { add("CVSS ${"%.1f".format(it)}") }
                entry.affectedComponent?.takeIf { it.isNotBlank() }?.let { comp ->
                    add(comp + (entry.affectedVersion?.let { " $it" } ?: ""))
                }
                entry.fixedVersion?.takeIf { it.isNotBlank() }?.let { add("fixed in $it") }
            }
            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = details.joinToString("  -  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Detail for a single CVE: its detection history for the artifact plus a status
 * update action (acknowledge, mark fixed, mark false positive).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CveDetailScreen(
    artifactId: String,
    cveId: String,
    onBack: () -> Unit,
    viewModel: CveTrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.cveDetailState.collectAsState()
    val parsedId = remember(artifactId) { runCatching { UUID.fromString(artifactId) }.getOrNull() }
    val uriHandler = LocalUriHandler.current
    var showStatusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(parsedId, cveId) {
        parsedId?.let { viewModel.loadCveDetail(it, cveId) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(cveId) },
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
                    onClick = { showStatusDialog = true },
                    enabled = parsedId != null && !state.isUpdating,
                ) {
                    Text("Update status")
                }
            },
        )

        when {
            parsedId == null -> CenteredText("Invalid artifact id")
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                state.error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = cveId,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.Underline,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://nvd.nist.gov/vuln/detail/$cveId")
                            },
                        )
                    }
                    if (state.history.isEmpty()) {
                        item { CenteredText("No history for this CVE") }
                    } else {
                        items(state.history, key = { it.id }) { entry ->
                            CveHistoryCard(entry)
                        }
                    }
                }
            }
        }
    }

    if (showStatusDialog && parsedId != null) {
        CveStatusDialog(
            isUpdating = state.isUpdating,
            onDismiss = { showStatusDialog = false },
            onConfirm = { status, reason ->
                viewModel.updateCveStatus(parsedId, cveId, status, reason)
                showStatusDialog = false
            },
        )
    }
}

@Composable
private fun CveHistoryCard(entry: CveHistoryEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                entry.severity?.let { SeverityBadge(it) } ?: Spacer(Modifier.width(0.dp))
                CveStatusBadge(entry.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            entry.cvssScore?.let {
                Text(
                    text = "CVSS ${"%.1f".format(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "First detected ${entry.firstDetectedAt.toLocalDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Last detected ${entry.lastDetectedAt.toLocalDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entry.acknowledgedReason?.takeIf { it.isNotBlank() }?.let { reason ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reason: $reason",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CveStatusDialog(
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (status: String, reason: String?) -> Unit,
) {
    var selectedStatus by remember { mutableStateOf(CveStatuses.first()) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update CVE status") },
        text = {
            Column {
                CveStatuses.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                        )
                        Text(status.replace('_', ' ').replaceFirstChar { it.uppercase() })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedStatus, reason.ifBlank { null }) },
                enabled = !isUpdating,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SeverityBadge(severity: String) {
    val color = when (severity.lowercase()) {
        "critical" -> Critical
        "high" -> High
        "medium" -> Medium
        "low" -> Low
        else -> Low
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = severity.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun CveStatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "open" -> StatusOpen
        "acknowledged", "false_positive" -> StatusAck
        "fixed", "resolved" -> StatusFixed
        else -> StatusAck
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = status.replace('_', ' ').replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
