@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.artifactkeeper.android.data.models.AlertState
import com.artifactkeeper.android.data.models.HealthCheck
import com.artifactkeeper.android.data.models.HealthLogEntry
import com.artifactkeeper.android.data.models.HealthResponse
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import com.artifactkeeper.android.ui.util.formatRelativeTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private val StatusOk = Color(0xFF52C41A)
private val StatusFail = Color(0xFFF5222D)

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun MonitoringScreen() {
    var health by remember { mutableStateOf<HealthResponse?>(null) }
    var alerts by remember { mutableStateOf<List<AlertState>>(emptyList()) }
    var healthLog by remember { mutableStateOf<List<HealthLogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadData(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                // Fetch health via OkHttp directly (not under /api/v1)
                val healthDeferred = async {
                    withContext(Dispatchers.IO) {
                        val healthUrl = ApiClient.baseUrl + "health"
                        val client = OkHttpClient()
                        val request = Request.Builder().url(healthUrl).build()
                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: "{}"
                        json.decodeFromString<HealthResponse>(body)
                    }
                }
                val alertsDeferred = async { ApiClient.api.getAlerts() }
                val logDeferred = async { ApiClient.api.getHealthLog() }

                health = healthDeferred.await()
                alerts = alertsDeferred.await()
                healthLog = logDeferred.await()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load monitoring data"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Monitoring") })

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
                        TextButton(onClick = { loadData() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { loadData(refresh = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Service health section
                        item {
                            Text(
                                text = "Service Health",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        val healthChecks = health?.checks?.entries?.toList() ?: emptyList()
                        if (healthChecks.isEmpty()) {
                            item {
                                Text(
                                    text = "No health checks available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        items(healthChecks, key = { it.key }) { (name, check) ->
                            ServiceHealthCard(name, check)
                        }

                        // Alerts section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Alerts",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        if (alerts.isEmpty()) {
                            item {
                                Text(
                                    text = "No active alerts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        items(alerts, key = { it.id }) { alert ->
                            AlertCard(alert)
                        }

                        // Health log section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Health Log",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        if (healthLog.isEmpty()) {
                            item {
                                Text(
                                    text = "No health log entries",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        items(healthLog, key = { it.id }) { entry ->
                            HealthLogCard(entry)
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceHealthCard(name: String, check: HealthCheck) {
    val isOk = check.status.lowercase() == "ok" || check.status.lowercase() == "healthy"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isOk) StatusOk else StatusFail),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = check.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOk) StatusOk else StatusFail,
                )
                if (check.responseTimeMs != null) {
                    Text(
                        text = "${check.responseTimeMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: AlertState) {
    val severityColor = when (alert.severity.lowercase()) {
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
                Text(
                    text = alert.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = alert.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = severityColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Status: ${alert.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatRelativeTime(alert.triggeredAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HealthLogCard(entry: HealthLogEntry) {
    val isOk = entry.status.lowercase() == "ok" || entry.status.lowercase() == "healthy"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOk) StatusOk else StatusFail),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = entry.service,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )

            if (entry.responseTimeMs != null) {
                Text(
                    text = "${entry.responseTimeMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = formatRelativeTime(entry.checkedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
