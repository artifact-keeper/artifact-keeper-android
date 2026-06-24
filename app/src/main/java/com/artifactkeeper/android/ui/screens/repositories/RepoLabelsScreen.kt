@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.repositories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.client.models.LabelResponse

/**
 * Labels for a single repository: list, add (key + optional value), and remove
 * a label by key with a confirm dialog.
 */
@Composable
fun RepoLabelsScreen(
    repoKey: String,
    onBack: () -> Unit,
    viewModel: RepoLabelsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var labelToDelete by remember { mutableStateOf<LabelResponse?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(repoKey) { viewModel.load(repoKey) }

    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Labels - $repoKey", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add label")
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
                state.error != null && state.labels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.load(repoKey, refresh = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.labels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No labels. Use + to add one.",
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
                        items(state.labels, key = { it.key }) { label ->
                            RepoLabelCard(
                                label = label,
                                isMutating = state.isMutating,
                                onDelete = { labelToDelete = label },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddRepoLabelDialog(
            isMutating = state.isMutating,
            onDismiss = { showAdd = false },
            onConfirm = { key, value ->
                viewModel.addLabel(repoKey, key, value)
                showAdd = false
            },
        )
    }

    labelToDelete?.let { label ->
        AlertDialog(
            onDismissRequest = { labelToDelete = null },
            title = { Text("Remove label") },
            text = { Text("Remove label \"${label.key}\" from this repository?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLabel(repoKey, label.key)
                    labelToDelete = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { labelToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun RepoLabelCard(
    label: LabelResponse,
    isMutating: Boolean,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.key,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (label.`value`.isNotBlank()) {
                    Text(
                        text = label.`value`,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onDelete, enabled = !isMutating) {
                Icon(Icons.Default.Close, contentDescription = "Remove label ${label.key}")
            }
        }
    }
}

@Composable
private fun AddRepoLabelDialog(
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (key: String, value: String) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add label") },
        text = {
            Column {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(key.trim(), value.trim()) },
                enabled = !isMutating && key.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
