@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.client.models.PluginResponse

private val EnabledColor = Color(0xFF52C41A)
private val DisabledColor = Color(0xFFFAAD14)
private val ErrorColor = Color(0xFFF5222D)

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "enabled", "active", "running" -> EnabledColor
    "disabled", "inactive", "stopped" -> DisabledColor
    "error", "failed" -> ErrorColor
    else -> DisabledColor
}

/**
 * Plugins management: lists installed plugins with enable/disable/reload/
 * uninstall actions and an install-from-git flow. Tapping a plugin opens its
 * detail.
 */
@Composable
fun PluginsScreen(
    onPluginClick: (String) -> Unit = {},
    viewModel: PluginsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showInstallDialog by remember { mutableStateOf(false) }
    var pluginToUninstall by remember { mutableStateOf<PluginResponse?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadPlugins() }

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showInstallDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Install plugin from Git")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.plugins.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.loadPlugins(refresh = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.plugins.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No plugins installed. Use + to install from Git.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.plugins, key = { it.id }) { plugin ->
                            PluginCard(
                                plugin = plugin,
                                isMutating = state.isMutating,
                                onClick = { onPluginClick(plugin.id.toString()) },
                                onEnable = { viewModel.enablePlugin(plugin.id) },
                                onDisable = { viewModel.disablePlugin(plugin.id) },
                                onReload = { viewModel.reloadPlugin(plugin.id) },
                                onUninstall = { pluginToUninstall = plugin },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInstallDialog) {
        InstallFromGitDialog(
            isMutating = state.isMutating,
            onDismiss = { showInstallDialog = false },
            onConfirm = { url, ref ->
                viewModel.installFromGit(url, ref)
                showInstallDialog = false
            },
        )
    }

    pluginToUninstall?.let { plugin ->
        AlertDialog(
            onDismissRequest = { pluginToUninstall = null },
            title = { Text("Uninstall plugin") },
            text = { Text("Uninstall \"${plugin.displayName}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.uninstallPlugin(plugin.id)
                        pluginToUninstall = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Uninstall") }
            },
            dismissButton = {
                TextButton(onClick = { pluginToUninstall = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PluginCard(
    plugin: PluginResponse,
    isMutating: Boolean,
    onClick: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onReload: () -> Unit,
    onUninstall: () -> Unit,
) {
    val enabled = plugin.status.equals("enabled", ignoreCase = true) ||
        plugin.status.equals("active", ignoreCase = true)
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
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plugin.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${plugin.pluginType}  -  v${plugin.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusBadge(plugin.status)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (enabled) {
                    TextButton(onClick = onDisable, enabled = !isMutating) { Text("Disable") }
                } else {
                    TextButton(onClick = onEnable, enabled = !isMutating) { Text("Enable") }
                }
                TextButton(onClick = onReload, enabled = !isMutating) { Text("Reload") }
                TextButton(
                    onClick = onUninstall,
                    enabled = !isMutating,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Uninstall") }
            }
        }
    }
}

@Composable
internal fun StatusBadge(status: String) {
    val color = statusColor(status)
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
private fun InstallFromGitDialog(
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (url: String, ref: String?) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install plugin from Git") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Git URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("Ref (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url.trim(), ref.trim().ifBlank { null }) },
                enabled = !isMutating && url.isNotBlank(),
            ) { Text("Install") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
