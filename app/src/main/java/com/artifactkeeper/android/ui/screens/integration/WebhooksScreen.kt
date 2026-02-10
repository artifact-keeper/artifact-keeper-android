@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.artifactkeeper.android.ui.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.artifactkeeper.android.data.models.CreateWebhookRequest
import com.artifactkeeper.android.data.models.Webhook
import kotlinx.coroutines.launch

private val ALL_EVENTS = listOf(
    "artifact_uploaded",
    "artifact_deleted",
    "repository_created",
    "repository_deleted",
    "user_created",
    "user_deleted",
    "build_started",
    "build_completed",
    "build_failed",
)

@Composable
fun WebhooksScreen() {
    var webhooks by remember { mutableStateOf<List<Webhook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var webhookToDelete by remember { mutableStateOf<Webhook?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadWebhooks(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                webhooks = ApiClient.webhooksApi.listWebhooks().unwrap().items
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load webhooks"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadWebhooks() }

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
                        TextButton(onClick = { loadWebhooks() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { loadWebhooks(refresh = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (webhooks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No webhooks configured",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        items(webhooks, key = { it.id }) { webhook ->
                            WebhookCard(
                                webhook = webhook,
                                onToggle = { enabled ->
                                    coroutineScope.launch {
                                        try {
                                            if (enabled) {
                                                ApiClient.webhooksApi.enableWebhook(webhook.id).unwrap()
                                            } else {
                                                ApiClient.webhooksApi.disableWebhook(webhook.id).unwrap()
                                            }
                                            loadWebhooks(refresh = true)
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Failed to toggle webhook"
                                        }
                                    }
                                },
                                onTest = {
                                    coroutineScope.launch {
                                        try {
                                            val result = ApiClient.webhooksApi.testWebhook(webhook.id).unwrap()
                                            testResult = if (result.success) {
                                                "Test succeeded (status ${result.statusCode})"
                                            } else {
                                                "Test failed: ${result.error ?: "Unknown error"}"
                                            }
                                        } catch (e: Exception) {
                                            testResult = "Test failed: ${e.message}"
                                        }
                                    }
                                },
                                onDelete = { webhookToDelete = webhook },
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
                                    Text("Add Webhook")
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
        AddWebhookDialog(
            onDismiss = { showAddDialog = false },
            onCreate = { request ->
                coroutineScope.launch {
                    try {
                        ApiClient.webhooksApi.createWebhook(request).unwrap()
                        showAddDialog = false
                        loadWebhooks(refresh = true)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Failed to create webhook"
                    }
                }
            },
        )
    }

    if (webhookToDelete != null) {
        AlertDialog(
            onDismissRequest = { webhookToDelete = null },
            title = { Text("Delete Webhook") },
            text = {
                Text("Are you sure you want to delete webhook \"${webhookToDelete?.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = webhookToDelete?.id ?: return@TextButton
                        webhookToDelete = null
                        coroutineScope.launch {
                            try {
                                ApiClient.webhooksApi.deleteWebhook(id).unwrap()
                                loadWebhooks(refresh = true)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to delete webhook"
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
                TextButton(onClick = { webhookToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (testResult != null) {
        AlertDialog(
            onDismissRequest = { testResult = null },
            title = { Text("Webhook Test") },
            text = { Text(testResult ?: "") },
            confirmButton = {
                TextButton(onClick = { testResult = null }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun WebhookCard(
    webhook: Webhook,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = webhook.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = webhook.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }

                Switch(
                    checked = webhook.isEnabled,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Event chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                webhook.events.forEach { event ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = event.replace("_", " "),
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onTest) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test")
                }
                Spacer(modifier = Modifier.width(4.dp))
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
private fun AddWebhookDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateWebhookRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var selectedEvents by remember { mutableStateOf<Set<String>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Webhook") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Secret (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Events",
                    style = MaterialTheme.typography.labelLarge,
                )

                Column {
                    ALL_EVENTS.forEach { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = event in selectedEvents,
                                onCheckedChange = { checked ->
                                    selectedEvents = if (checked) {
                                        selectedEvents + event
                                    } else {
                                        selectedEvents - event
                                    }
                                },
                            )
                            Text(
                                text = event.replace("_", " "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        CreateWebhookRequest(
                            name = name,
                            url = url,
                            events = selectedEvents.toList(),
                            secret = secret.ifBlank { null },
                        )
                    )
                },
                enabled = name.isNotBlank() && url.isNotBlank() && selectedEvents.isNotEmpty(),
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
