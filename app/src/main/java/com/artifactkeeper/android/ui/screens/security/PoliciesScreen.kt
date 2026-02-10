@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.android.data.models.CreatePolicyRequest
import com.artifactkeeper.android.data.models.SecurityPolicy
import com.artifactkeeper.android.data.models.UpdatePolicyRequest
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import kotlinx.coroutines.launch

private val SEVERITY_OPTIONS = listOf("critical", "high", "medium", "low")

@Composable
fun PoliciesScreen() {
    var policies by remember { mutableStateOf<List<SecurityPolicy>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var policyToDelete by remember { mutableStateOf<SecurityPolicy?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadPolicies(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                policies = ApiClient.securityApi.listPolicies().unwrap()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load policies"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadPolicies() }

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
                        TextButton(onClick = { loadPolicies() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { loadPolicies(refresh = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (policies.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No security policies defined",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        items(policies, key = { it.id }) { policy ->
                            PolicyCard(
                                policy = policy,
                                onToggle = { enabled ->
                                    coroutineScope.launch {
                                        try {
                                            ApiClient.securityApi.updatePolicy(
                                                policy.id,
                                                UpdatePolicyRequest(
                                                    name = policy.name,
                                                    maxSeverity = policy.maxSeverity,
                                                    blockUnscanned = policy.blockUnscanned,
                                                    blockOnFail = policy.blockOnFail,
                                                    isEnabled = enabled,
                                                )
                                            ).unwrap()
                                            loadPolicies(refresh = true)
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Failed to update policy"
                                        }
                                    }
                                },
                                onDelete = { policyToDelete = policy },
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Button(onClick = { showAddDialog = true }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Policy")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPolicyDialog(
            onDismiss = { showAddDialog = false },
            onCreate = { request ->
                coroutineScope.launch {
                    try {
                        ApiClient.securityApi.createPolicy(request).unwrap()
                        showAddDialog = false
                        loadPolicies(refresh = true)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Failed to create policy"
                    }
                }
            },
        )
    }

    if (policyToDelete != null) {
        AlertDialog(
            onDismissRequest = { policyToDelete = null },
            title = { Text("Delete Policy") },
            text = {
                Text("Are you sure you want to delete policy \"${policyToDelete?.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = policyToDelete?.id ?: return@TextButton
                        policyToDelete = null
                        coroutineScope.launch {
                            try {
                                ApiClient.securityApi.deletePolicy(id).unwrap()
                                loadPolicies(refresh = true)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to delete policy"
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { policyToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun PolicyCard(
    policy: SecurityPolicy,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val severityColor = when (policy.maxSeverity.lowercase()) {
        "critical" -> Critical
        "high" -> High
        "medium" -> Medium
        "low" -> Low
        else -> MaterialTheme.colorScheme.outline
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = policy.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Switch(
                    checked = policy.isEnabled,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Severity badge + chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Severity threshold
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = policy.maxSeverity.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = severityColor,
                    )
                }

                if (policy.blockUnscanned) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Block Unscanned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                if (policy.blockOnFail) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Block on Fail",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun AddPolicyDialog(
    onDismiss: () -> Unit,
    onCreate: (CreatePolicyRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf("high") }
    var blockUnscanned by remember { mutableStateOf(false) }
    var blockOnFail by remember { mutableStateOf(false) }
    var severityExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Policy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Severity picker
                ExposedDropdownMenuBox(
                    expanded = severityExpanded,
                    onExpandedChange = { severityExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedSeverity.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Max Severity") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = severityExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = severityExpanded,
                        onDismissRequest = { severityExpanded = false },
                    ) {
                        SEVERITY_OPTIONS.forEach { severity ->
                            DropdownMenuItem(
                                text = { Text(severity.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedSeverity = severity
                                    severityExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Block Unscanned")
                    Switch(checked = blockUnscanned, onCheckedChange = { blockUnscanned = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Block on Fail")
                    Switch(checked = blockOnFail, onCheckedChange = { blockOnFail = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        CreatePolicyRequest(
                            name = name,
                            maxSeverity = selectedSeverity,
                            blockUnscanned = blockUnscanned,
                            blockOnFail = blockOnFail,
                        )
                    )
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
