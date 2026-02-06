package com.artifactkeeper.android.ui.screens.staging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionDialog(
    viewModel: StagingViewModel = viewModel(),
    artifactId: String?, // null means bulk promotion
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTargetRepo by remember { mutableStateOf<String?>(uiState.selectedRepo?.targetRepositoryKey) }
    var forcePromotion by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val isBulk = artifactId == null
    val selectedCount = uiState.selectedArtifactIds.size
    val hasViolations = if (isBulk) {
        uiState.artifacts
            .filter { it.id in uiState.selectedArtifactIds }
            .any { it.policyViolations.isNotEmpty() }
    } else {
        uiState.artifacts.find { it.id == artifactId }?.policyViolations?.isNotEmpty() == true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBulk) "Promote $selectedCount Artifacts" else "Promote Artifact",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Target repository dropdown
                Text(
                    text = "Target Repository",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedTargetRepo ?: "Select target repository",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        uiState.targetRepositories.forEach { repo ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(repo.name)
                                        Text(
                                            text = "${repo.format} - ${repo.key}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTargetRepo = repo.key
                                    expanded = false
                                },
                            )
                        }
                        if (uiState.targetRepositories.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "No target repositories available",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = { expanded = false },
                                enabled = false,
                            )
                        }
                    }
                }

                // Comment field
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )

                // Force promotion checkbox (with warning)
                if (hasViolations) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = "Policy Violations Detected",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBulk) {
                                    "Some selected artifacts have policy violations. Enable force promotion to proceed anyway."
                                } else {
                                    "This artifact has policy violations. Enable force promotion to proceed anyway."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = forcePromotion,
                                    onCheckedChange = { forcePromotion = it },
                                )
                                Text(
                                    text = "Force promotion (override policy)",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                // Summary
                if (isBulk) {
                    Text(
                        text = "Promoting $selectedCount artifact${if (selectedCount != 1) "s" else ""} to ${selectedTargetRepo ?: "..."}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetKey = selectedTargetRepo ?: return@Button
                    if (isBulk) {
                        viewModel.promoteBulk(
                            targetRepoKey = targetKey,
                            force = forcePromotion,
                            comment = comment.ifBlank { null },
                            onSuccess = { onDismiss() },
                        )
                    } else {
                        viewModel.promoteArtifact(
                            artifactId = artifactId,
                            targetRepoKey = targetKey,
                            force = forcePromotion,
                            comment = comment.ifBlank { null },
                            onSuccess = { onDismiss() },
                        )
                    }
                },
                enabled = selectedTargetRepo != null && !uiState.isPromoting &&
                    (!hasViolations || forcePromotion),
            ) {
                if (uiState.isPromoting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Promote")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isPromoting,
            ) {
                Text("Cancel")
            }
        },
    )
}
