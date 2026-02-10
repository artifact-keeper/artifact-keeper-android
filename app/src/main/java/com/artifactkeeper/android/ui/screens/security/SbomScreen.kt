@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.artifactkeeper.android.ui.screens.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.CveHistoryEntry
import com.artifactkeeper.android.data.models.GenerateSbomRequest
import com.artifactkeeper.android.data.models.SbomComponent
import com.artifactkeeper.android.data.models.SbomContentResponse
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.android.ui.util.formatRelativeTime
import kotlinx.coroutines.launch

private val FORMAT_OPTIONS = listOf("cyclonedx", "spdx")

private val StatusResolved = Color(0xFF52C41A)
private val StatusOpen = Color(0xFFF5222D)
private val LicenseBadge = Color(0xFF1890FF)

private fun severityColor(severity: String): Color = when (severity.lowercase()) {
    "critical" -> Critical
    "high" -> High
    "medium" -> Medium
    "low" -> Low
    else -> Low
}

@Composable
fun SbomScreen(
    artifactId: String,
    artifactName: String,
    onBack: () -> Unit,
) {
    var sbom by remember { mutableStateOf<SbomContentResponse?>(null) }
    var components by remember { mutableStateOf<List<SbomComponent>>(emptyList()) }
    var cveHistory by remember { mutableStateOf<List<CveHistoryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFormat by remember { mutableStateOf("cyclonedx") }
    var showRawJson by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun loadSbomData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Try to find existing SBOM for this artifact
                val sbomList = ApiClient.sbomApi.listSboms(artifactId = java.util.UUID.fromString(artifactId)).unwrap()
                if (sbomList.isNotEmpty()) {
                    val existingSbom = sbomList.first()
                    val sbomContent = ApiClient.sbomApi.getSbom(existingSbom.id).unwrap()
                    sbom = sbomContent
                    selectedFormat = sbomContent.format

                    // Load components
                    try {
                        components = ApiClient.sbomApi.getSbomComponents(existingSbom.id).unwrap()
                    } catch (_: Exception) {
                        components = emptyList()
                    }
                } else {
                    sbom = null
                    components = emptyList()
                }

                // Load CVE history
                try {
                    cveHistory = ApiClient.sbomApi.getCveHistory(java.util.UUID.fromString(artifactId)).unwrap()
                } catch (_: Exception) {
                    cveHistory = emptyList()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load SBOM data"
            } finally {
                isLoading = false
            }
        }
    }

    fun generateSbom() {
        coroutineScope.launch {
            isGenerating = true
            errorMessage = null
            try {
                ApiClient.sbomApi.generateSbom(
                    GenerateSbomRequest(
                        artifactId = java.util.UUID.fromString(artifactId),
                        format = selectedFormat,
                    )
                ).unwrap()
                // Reload to get full content
                loadSbomData()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to generate SBOM"
            } finally {
                isGenerating = false
            }
        }
    }

    LaunchedEffect(artifactId) { loadSbomData() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "SBOM",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = artifactName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                        TextButton(onClick = { loadSbomData() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Format selector + Generate button
                    item(key = "controls") {
                        SbomControlsCard(
                            selectedFormat = selectedFormat,
                            onFormatChange = { selectedFormat = it },
                            hasSbom = sbom != null,
                            isGenerating = isGenerating,
                            onGenerate = { generateSbom() },
                        )
                    }

                    // Stats cards
                    if (sbom != null) {
                        item(key = "stats") {
                            SbomStatsCard(sbom!!)
                        }
                    }

                    // License badges
                    if (sbom != null && sbom!!.licenses.isNotEmpty()) {
                        item(key = "licenses") {
                            SbomLicensesCard(sbom!!.licenses)
                        }
                    }

                    // Components list
                    if (components.isNotEmpty()) {
                        item(key = "components-header") {
                            Text(
                                text = "Components (${components.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        items(components.take(20), key = { "${it.name}-${it.version}" }) { component ->
                            SbomComponentCard(component)
                        }

                        if (components.size > 20) {
                            item(key = "components-more") {
                                Text(
                                    text = "... and ${components.size - 20} more components",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        }
                    }

                    // CVE History
                    if (cveHistory.isNotEmpty()) {
                        item(key = "cve-header") {
                            Text(
                                text = "CVE History (${cveHistory.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }

                        items(cveHistory, key = { it.id }) { entry ->
                            CveHistoryCard(entry)
                        }
                    }

                    // Raw JSON viewer
                    if (sbom != null) {
                        item(key = "raw-json") {
                            RawJsonCard(
                                content = sbom!!.content.toString(),
                                expanded = showRawJson,
                                onToggle = { showRawJson = !showRawJson },
                            )
                        }
                    }

                    // Empty state
                    if (sbom == null) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No SBOM generated yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Select a format and click Generate to create an SBOM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SbomControlsCard(
    selectedFormat: String,
    onFormatChange: (String) -> Unit,
    hasSbom: Boolean,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
) {
    var formatExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Format picker
                ExposedDropdownMenuBox(
                    expanded = formatExpanded,
                    onExpandedChange = { formatExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = selectedFormat.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Format") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = formatExpanded,
                        onDismissRequest = { formatExpanded = false },
                    ) {
                        FORMAT_OPTIONS.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.uppercase()) },
                                onClick = {
                                    onFormatChange(format)
                                    formatExpanded = false
                                },
                            )
                        }
                    }
                }

                // Generate button
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasSbom) "Regenerate" else "Generate")
                }
            }
        }
    }
}

@Composable
private fun SbomStatsCard(sbom: SbomContentResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SBOM Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SbomStatItem(label = "Format", value = sbom.format.uppercase())
                SbomStatItem(label = "Components", value = sbom.componentCount.toString())
                SbomStatItem(label = "Dependencies", value = sbom.dependencyCount.toString())
                SbomStatItem(label = "Licenses", value = sbom.licenseCount.toString())
            }

            if (sbom.specVersion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Spec Version: ${sbom.specVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Generated: ${formatRelativeTime(sbom.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SbomStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SbomLicensesCard(licenses: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Licenses",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                licenses.forEach { license ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LicenseBadge.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = license,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = LicenseBadge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SbomComponentCard(component: SbomComponent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                component.version?.let { ver ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = ver,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            if (component.licenses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    component.licenses.forEach { license ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LicenseBadge.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = license,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = LicenseBadge,
                            )
                        }
                    }
                }
            }

            component.componentType?.let { compType ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Type: $compType",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CveHistoryCard(entry: CveHistoryEntry) {
    val uriHandler = LocalUriHandler.current
    val severity = entry.severity ?: "unknown"
    val sevColor = severityColor(severity)
    val statusColor = if (entry.status.lowercase() == "resolved") StatusResolved else StatusOpen

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Severity badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(sevColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = severity.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = sevColor,
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = entry.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // CVE ID as clickable link
                Text(
                    text = entry.cveId,
                    style = MaterialTheme.typography.labelSmall.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://nvd.nist.gov/vuln/detail/${entry.cveId}")
                    },
                )
            }

            // Show affected component as title-like info
            entry.affectedComponent?.takeIf { it.isNotBlank() }?.let { comp ->
                Spacer(modifier = Modifier.height(6.dp))
                val label = buildString {
                    append(comp)
                    entry.affectedVersion?.takeIf { it.isNotBlank() }?.let { ver ->
                        append(" @ $ver")
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Detected: ${formatRelativeTime(entry.firstDetectedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Show updatedAt as resolved time when status is resolved
                if (entry.status.lowercase() == "resolved") {
                    Text(
                        text = "Resolved: ${formatRelativeTime(entry.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusResolved,
                    )
                }
            }
        }
    }
}

@Composable
private fun RawJsonCard(
    content: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Raw JSON",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp),
                    ) {
                        SelectionContainer {
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
