package com.artifactkeeper.android.ui.screens.staging

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artifactkeeper.android.data.models.CveSummary
import com.artifactkeeper.android.data.models.LicenseSummary
import com.artifactkeeper.android.data.models.PolicyStatus
import com.artifactkeeper.android.data.models.PolicyViolation
import com.artifactkeeper.android.data.models.StagingArtifact
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.android.ui.util.formatBytes

// Policy status colors matching web UI
private val StatusPassing = Color(0xFF52C41A)
private val StatusFailing = Color(0xFFF5222D)
private val StatusWarning = Color(0xFFFAAD14)
private val StatusPending = Color(0xFF8C8C8C)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StagingDetailScreen(
    viewModel: StagingViewModel = viewModel(),
    onBack: () -> Unit,
    onShowHistory: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val repo = uiState.selectedRepo
    var isRefreshing by remember { mutableStateOf(false) }
    var showPromotionDialog by remember { mutableStateOf(false) }
    var promotingArtifactId by remember { mutableStateOf<String?>(null) }
    var showContextMenu by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val snackbarHostState = remember { SnackbarHostState() }

    // Show success/error messages
    LaunchedEffect(uiState.promotionSuccess, uiState.promotionError) {
        uiState.promotionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.promotionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.isLoadingArtifacts) {
        if (!uiState.isLoadingArtifacts) {
            isRefreshing = false
        }
    }

    if (repo == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("No repository selected")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = repo.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${uiState.artifacts.size} artifacts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShowHistory) {
                        Icon(Icons.Default.History, contentDescription = "Promotion History")
                    }
                    if (uiState.selectedArtifactIds.isNotEmpty()) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(uiState.selectedArtifactIds.size.toString())
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.selectedArtifactIds.isNotEmpty()) {
                BottomAppBar(
                    actions = {
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text("Clear")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.selectedArtifactIds.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = {
                                promotingArtifactId = null
                                showPromotionDialog = true
                            },
                            icon = { Icon(Icons.Default.Publish, contentDescription = null) },
                            text = { Text("Promote") },
                        )
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.filterStatus == null,
                    onClick = { viewModel.setFilterStatus(null) },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = uiState.filterStatus == PolicyStatus.PASSING,
                    onClick = { viewModel.setFilterStatus(PolicyStatus.PASSING) },
                    label = { Text("Passing") },
                    leadingIcon = if (uiState.filterStatus == PolicyStatus.PASSING) {
                        { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                )
                FilterChip(
                    selected = uiState.filterStatus == PolicyStatus.FAILING,
                    onClick = { viewModel.setFilterStatus(PolicyStatus.FAILING) },
                    label = { Text("Failing") },
                    leadingIcon = if (uiState.filterStatus == PolicyStatus.FAILING) {
                        { Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                )
                FilterChip(
                    selected = uiState.filterStatus == PolicyStatus.PENDING,
                    onClick = { viewModel.setFilterStatus(PolicyStatus.PENDING) },
                    label = { Text("Pending") },
                    leadingIcon = if (uiState.filterStatus == PolicyStatus.PENDING) {
                        { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                )
            }

            // Select all button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { viewModel.selectAllArtifacts() },
                    enabled = uiState.artifacts.any { it.canPromote },
                ) {
                    Text("Select All Promotable")
                }
            }

            when {
                uiState.isLoadingArtifacts && uiState.artifacts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.artifactsError != null && uiState.artifacts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.artifactsError ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.loadArtifacts(repo.key) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                uiState.artifacts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No artifacts in staging",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            viewModel.loadArtifacts(repo.key)
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.artifacts, key = { it.id }) { artifact ->
                                StagingArtifactCard(
                                    artifact = artifact,
                                    isSelected = artifact.id in uiState.selectedArtifactIds,
                                    showContextMenu = showContextMenu == artifact.id,
                                    onCheckedChange = { viewModel.toggleArtifactSelection(artifact.id) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showContextMenu = artifact.id
                                    },
                                    onDismissContextMenu = { showContextMenu = null },
                                    onPromoteClick = {
                                        showContextMenu = null
                                        promotingArtifactId = artifact.id
                                        showPromotionDialog = true
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Promotion Dialog
    if (showPromotionDialog) {
        PromotionDialog(
            viewModel = viewModel,
            artifactId = promotingArtifactId,
            onDismiss = {
                showPromotionDialog = false
                promotingArtifactId = null
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StagingArtifactCard(
    artifact: StagingArtifact,
    isSelected: Boolean,
    showContextMenu: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLongClick: () -> Unit,
    onDismissContextMenu: () -> Unit,
    onPromoteClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val policyStatus = PolicyStatus.fromString(artifact.policyStatus)
    val statusColor = policyStatusColor(policyStatus)
    val statusIcon = policyStatusIcon(policyStatus)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = onLongClick,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onCheckedChange,
                    enabled = artifact.canPromote,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artifact.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artifact.version != null) {
                        Text(
                            text = artifact.version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Policy status chip
                PolicyStatusChip(
                    icon = statusIcon,
                    label = policyStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = statusColor,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CVE and License summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                artifact.cveSummary?.let { cve ->
                    CveSummaryRow(cve)
                }
                artifact.licenseSummary?.let { license ->
                    LicenseSummaryChip(license)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatBytes(artifact.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = artifact.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp),
                )
            }

            // Expandable violations section
            AnimatedVisibility(
                visible = expanded && artifact.policyViolations.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Policy Violations",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    artifact.policyViolations.forEach { violation ->
                        PolicyViolationCard(violation)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (artifact.policyViolations.isNotEmpty() && !expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${artifact.policyViolations.size} violation${if (artifact.policyViolations.size != 1) "s" else ""} - tap to expand",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusFailing,
                )
            }
        }

        // Context menu dropdown
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = onDismissContextMenu,
        ) {
            DropdownMenuItem(
                text = { Text("Promote") },
                onClick = onPromoteClick,
                leadingIcon = { Icon(Icons.Default.Publish, contentDescription = null) },
                enabled = artifact.canPromote,
            )
            DropdownMenuItem(
                text = { Text(if (isSelected) "Deselect" else "Select") },
                onClick = {
                    onCheckedChange(!isSelected)
                    onDismissContextMenu()
                },
                leadingIcon = {
                    Icon(
                        if (isSelected) Icons.Default.CheckBoxOutlineBlank else Icons.Default.CheckBox,
                        contentDescription = null,
                    )
                },
                enabled = artifact.canPromote,
            )
        }
    }
}

@Composable
private fun PolicyStatusChip(
    icon: ImageVector,
    label: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun CveSummaryRow(cve: CveSummary) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = "CVE",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        if (cve.criticalCount > 0) {
            SeverityBadge("C", cve.criticalCount, Critical)
        }
        if (cve.highCount > 0) {
            SeverityBadge("H", cve.highCount, High)
        }
        if (cve.mediumCount > 0) {
            SeverityBadge("M", cve.mediumCount, Medium)
        }
        if (cve.lowCount > 0) {
            SeverityBadge("L", cve.lowCount, Low)
        }
        if (cve.total == 0) {
            Text(
                text = "No CVEs",
                style = MaterialTheme.typography.labelSmall,
                color = StatusPassing,
            )
        }
    }
}

@Composable
private fun SeverityBadge(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = "$label$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun LicenseSummaryChip(license: LicenseSummary) {
    val color = when {
        license.deniedCount > 0 -> StatusFailing
        license.unknownCount > 0 -> StatusWarning
        else -> StatusPassing
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Policy,
            contentDescription = "License",
            tint = color,
            modifier = Modifier.size(12.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "${license.total} licenses",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun PolicyViolationCard(violation: PolicyViolation) {
    val severityColor = when (violation.severity.lowercase()) {
        "critical" -> Critical
        "high" -> High
        "medium" -> Medium
        "low" -> Low
        else -> StatusWarning
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = violation.policyName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = violation.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = violation.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (violation.rule != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Rule: ${violation.rule}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun policyStatusColor(status: PolicyStatus): Color = when (status) {
    PolicyStatus.PASSING -> StatusPassing
    PolicyStatus.FAILING -> StatusFailing
    PolicyStatus.WARNING -> StatusWarning
    PolicyStatus.PENDING -> StatusPending
}

private fun policyStatusIcon(status: PolicyStatus): ImageVector = when (status) {
    PolicyStatus.PASSING -> Icons.Default.CheckCircle
    PolicyStatus.FAILING -> Icons.Default.Cancel
    PolicyStatus.WARNING -> Icons.Default.Warning
    PolicyStatus.PENDING -> Icons.Default.Schedule
}
