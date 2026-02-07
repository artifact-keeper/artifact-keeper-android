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
import com.artifactkeeper.android.data.models.CreateLicensePolicyRequest
import com.artifactkeeper.android.data.models.LicensePolicy
import com.artifactkeeper.android.data.models.UpdateLicensePolicyRequest
import kotlinx.coroutines.launch

private val ACTION_OPTIONS = listOf("warn", "block", "allow")

private val ActionWarn = Color(0xFFFAAD14)
private val ActionBlock = Color(0xFFF5222D)
private val ActionAllow = Color(0xFF52C41A)

@Composable
fun LicensePoliciesScreen() {
    var policies by remember { mutableStateOf<List<LicensePolicy>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var policyToDelete by remember { mutableStateOf<LicensePolicy?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadPolicies(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                policies = ApiClient.api.listLicensePolicies()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load license policies"
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
                                        text = "No license policies defined",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        items(policies, key = { it.id }) { policy ->
                            LicensePolicyCard(
                                policy = policy,
                                onToggle = { enabled ->
                                    coroutineScope.launch {
                                        try {
                                            ApiClient.api.updateLicensePolicy(
                                                policy.id,
                                                UpdateLicensePolicyRequest(
                                                    name = policy.name,
                                                    description = policy.description,
                                                    allowedLicenses = policy.allowedLicenses,
                                                    deniedLicenses = policy.deniedLicenses,
                                                    action = policy.action,
                                                    allowUnknown = policy.allowUnknown,
                                                    isEnabled = enabled,
                                                )
                                            )
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
                                    Text("Add License Policy")
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
        AddLicensePolicyDialog(
            onDismiss = { showAddDialog = false },
            onCreate = { request ->
                coroutineScope.launch {
                    try {
                        ApiClient.api.createLicensePolicy(request)
                        showAddDialog = false
                        loadPolicies(refresh = true)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Failed to create license policy"
                    }
                }
            },
        )
    }

    if (policyToDelete != null) {
        AlertDialog(
            onDismissRequest = { policyToDelete = null },
            title = { Text("Delete License Policy") },
            text = {
                Text("Are you sure you want to delete license policy \"${policyToDelete?.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = policyToDelete?.id ?: return@TextButton
                        policyToDelete = null
                        coroutineScope.launch {
                            try {
                                ApiClient.api.deleteLicensePolicy(id)
                                loadPolicies(refresh = true)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to delete license policy"
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
private fun LicensePolicyCard(
    policy: LicensePolicy,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val actionColor = when (policy.action.lowercase()) {
        "block" -> ActionBlock
        "allow" -> ActionAllow
        else -> ActionWarn
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
                    if (!policy.description.isNullOrBlank()) {
                        Text(
                            text = policy.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Switch(
                    checked = policy.isEnabled,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action badge + license counts
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Action badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(actionColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = policy.action.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = actionColor,
                    )
                }

                // Allowed count
                if (policy.allowedLicenses.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ActionAllow.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${policy.allowedLicenses.size} Allowed",
                            style = MaterialTheme.typography.labelSmall,
                            color = ActionAllow,
                        )
                    }
                }

                // Denied count
                if (policy.deniedLicenses.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ActionBlock.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${policy.deniedLicenses.size} Denied",
                            style = MaterialTheme.typography.labelSmall,
                            color = ActionBlock,
                        )
                    }
                }

                // Allow unknown
                if (policy.allowUnknown) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Allow Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
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
private fun AddLicensePolicyDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateLicensePolicyRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var allowedLicenses by remember { mutableStateOf("") }
    var deniedLicenses by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("warn") }
    var allowUnknown by remember { mutableStateOf(true) }
    var actionExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add License Policy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = allowedLicenses,
                    onValueChange = { allowedLicenses = it },
                    label = { Text("Allowed Licenses (comma-separated)") },
                    placeholder = { Text("MIT, Apache-2.0, BSD-3-Clause") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = deniedLicenses,
                    onValueChange = { deniedLicenses = it },
                    label = { Text("Denied Licenses (comma-separated)") },
                    placeholder = { Text("GPL-3.0, AGPL-3.0") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Action picker
                ExposedDropdownMenuBox(
                    expanded = actionExpanded,
                    onExpandedChange = { actionExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedAction.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = actionExpanded,
                        onDismissRequest = { actionExpanded = false },
                    ) {
                        ACTION_OPTIONS.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedAction = action
                                    actionExpanded = false
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
                    Text("Allow Unknown Licenses")
                    Switch(checked = allowUnknown, onCheckedChange = { allowUnknown = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val allowed = allowedLicenses.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    val denied = deniedLicenses.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    onCreate(
                        CreateLicensePolicyRequest(
                            name = name,
                            description = description.ifBlank { null },
                            allowedLicenses = allowed,
                            deniedLicenses = denied,
                            action = selectedAction,
                            allowUnknown = allowUnknown,
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
