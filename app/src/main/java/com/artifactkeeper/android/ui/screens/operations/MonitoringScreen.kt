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
import com.artifactkeeper.android.data.models.DtStatus
import com.artifactkeeper.android.data.models.HealthCheck
import com.artifactkeeper.android.data.models.HealthLogEntry
import com.artifactkeeper.android.data.models.HealthResponse
import com.artifactkeeper.android.ui.util.formatRelativeTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request

private val StatusOk = Color(0xFF52C41A)
private val StatusFail = Color(0xFFF5222D)

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun MonitoringScreen() {
    var health by remember { mutableStateOf<HealthResponse?>(null) }
    var dtStatus by remember { mutableStateOf<DtStatus?>(null) }
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
                health = withContext(Dispatchers.IO) {
                    val healthUrl = ApiClient.baseUrl + "health"
                    val client = ApiClient.httpClient
                    val request = Request.Builder().url(healthUrl).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: "{}"
                    json.decodeFromString<HealthResponse>(body)
                }
                // Fetch Dependency-Track status
                try {
                    dtStatus = ApiClient.api.getDtStatus()
                } catch (_: Exception) {
                    dtStatus = null
                }
                alerts = ApiClient.api.getAlerts()
                healthLog = ApiClient.api.getHealthLog()
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

                        items(healthChecks, key = { "check-${it.key}" }) { (name, check) ->
                            ServiceHealthCard(name, check)
                        }

                        // Dependency-Track health (from separate endpoint)
                        if (dtStatus?.enabled == true) {
                            item(key = "check-dependency-track") {
                                ServiceHealthCard(
                                    name = "Dependency-Track",
                                    check = HealthCheck(
                                        status = if (dtStatus!!.healthy) "healthy" else "unhealthy",
                                        responseTimeMs = null,
                                    ),
                                )
                            }
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

                        items(alerts, key = { "alert-${it.serviceName}" }) { alert ->
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

                        items(healthLog.size) { index ->
                            val entry = healthLog[index]
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
    val isHealthy = alert.currentStatus.lowercase() == "healthy"
    val statusColor = if (isHealthy) StatusOk else StatusFail

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = alert.serviceName.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = alert.currentStatus.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            if (alert.consecutiveFailures > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Consecutive failures: ${alert.consecutiveFailures}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatRelativeTime(alert.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                text = entry.serviceName.replaceFirstChar { it.uppercase() },
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
