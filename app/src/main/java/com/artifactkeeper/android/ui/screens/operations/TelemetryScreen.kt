@file:OptIn(ExperimentalMaterial3Api::class)

package com.artifactkeeper.android.ui.screens.operations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

data class MetricEntry(
    val name: String,
    val value: String,
    val help: String? = null,
)

@Composable
fun TelemetryScreen() {
    var metrics by remember { mutableStateOf<List<MetricEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadMetrics(refresh: Boolean = false) {
        coroutineScope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            errorMessage = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val metricsUrl = ApiClient.baseUrl + "metrics"
                    val client = ApiClient.httpClient
                    val request = Request.Builder().url(metricsUrl).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    parsePrometheusMetrics(body)
                }
                metrics = result
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load metrics"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { loadMetrics() }

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
                        TextButton(onClick = { loadMetrics() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            metrics.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No metrics available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { loadMetrics(refresh = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text(
                                text = "${metrics.size} metrics",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        items(metrics, key = { it.name }) { metric ->
                            MetricCard(metric)
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(metric: MetricEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = metric.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (metric.help != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = metric.help,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

private fun parsePrometheusMetrics(text: String): List<MetricEntry> {
    val entries = mutableListOf<MetricEntry>()
    val helpMap = mutableMapOf<String, String>()

    val lines = text.lines()
    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("# HELP ") -> {
                val rest = trimmed.removePrefix("# HELP ")
                val spaceIndex = rest.indexOf(' ')
                if (spaceIndex > 0) {
                    val metricName = rest.substring(0, spaceIndex)
                    val helpText = rest.substring(spaceIndex + 1)
                    helpMap[metricName] = helpText
                }
            }
            trimmed.startsWith("#") || trimmed.isBlank() -> {
                // Skip TYPE lines and empty lines
            }
            else -> {
                // Parse metric line: metric_name{labels} value
                val parts = trimmed.split(" ")
                if (parts.size >= 2) {
                    val fullName = parts[0]
                    val value = parts[1]
                    // Extract base metric name (without labels)
                    val baseName = fullName.substringBefore("{")
                    entries.add(
                        MetricEntry(
                            name = fullName,
                            value = value,
                            help = helpMap[baseName],
                        )
                    )
                }
            }
        }
    }

    return entries
}
