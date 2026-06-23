@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.artifactkeeper.android.ui.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.client.models.DeliveryResponse
import com.artifactkeeper.client.models.WebhookResponse
import java.util.UUID

private val SuccessColor = Color(0xFF52C41A)
private val FailureColor = Color(0xFFF5222D)

/**
 * Detail for a single webhook: its configuration, a Rotate secret action, and
 * the recent delivery attempts (each re-sendable).
 */
@Composable
fun WebhookDetailScreen(
    webhookId: String,
    onBack: () -> Unit,
    viewModel: WebhookDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val parsedId = remember(webhookId) { runCatching { UUID.fromString(webhookId) }.getOrNull() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(parsedId) {
        parsedId?.let { viewModel.load(it) }
    }

    LaunchedEffect(state.message, state.error, state.webhook) {
        val text = state.message ?: state.error?.takeIf { state.webhook != null }
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TopAppBar(
                title = { Text("Webhook") },
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
                        onClick = { parsedId?.let { viewModel.rotateSecret(it) } },
                        enabled = parsedId != null && !state.isMutating,
                    ) { Text("Rotate secret") }
                },
            )

            when {
                parsedId == null -> CenteredText("Invalid webhook id")
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.webhook == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.load(parsedId) }) { Text("Retry") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.webhook?.let { webhook ->
                            item { WebhookConfigCard(webhook) }
                        }

                        item {
                            Text(
                                text = "Deliveries (${state.deliveries.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (state.deliveries.isEmpty()) {
                            item {
                                Text(
                                    text = "No deliveries recorded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(state.deliveries, key = { it.id }) { delivery ->
                                DeliveryCard(
                                    delivery = delivery,
                                    isMutating = state.isMutating,
                                    onRedeliver = {
                                        parsedId?.let { viewModel.redeliver(it, delivery.id) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.newSecret?.let { secret ->
        AlertDialog(
            onDismissRequest = { viewModel.clearNewSecret() },
            title = { Text("New webhook secret") },
            text = {
                Column {
                    Text(
                        text = "Copy this secret now. It will not be shown again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = secret,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearNewSecret() }) { Text("Done") }
            },
        )
    }
}

@Composable
private fun WebhookConfigCard(webhook: WebhookResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = webhook.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                val color = if (webhook.isEnabled) SuccessColor else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (webhook.isEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = webhook.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            webhook.lastTriggeredAt?.let { ts ->
                Text(
                    text = "Last triggered ${ts.toLocalDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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
        }
    }
}

@Composable
private fun DeliveryCard(
    delivery: DeliveryResponse,
    isMutating: Boolean,
    onRedeliver: () -> Unit,
) {
    val color = if (delivery.success) SuccessColor else FailureColor
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = delivery.event.replace("_", " "),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = delivery.responseStatus?.toString() ?: if (delivery.success) "OK" else "Failed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${delivery.attempts} attempt${if (delivery.attempts != 1) "s" else ""}  -  ${delivery.createdAt.toLocalDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            delivery.responseBody?.takeIf { it.isNotBlank() }?.let { body ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onRedeliver, enabled = !isMutating) { Text("Redeliver") }
            }
        }
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
