package com.artifactkeeper.android.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artifactkeeper.android.ui.components.ItemTitleWithChip
import com.artifactkeeper.android.ui.util.formatBytes
import com.artifactkeeper.client.models.ChecksumArtifact
import com.artifactkeeper.client.models.SearchResultItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadInitial() }

    // Debounce text search and suggestions; checksum search is explicit.
    LaunchedEffect(query, state.mode) {
        if (state.mode != SearchMode.TEXT) return@LaunchedEffect
        if (query.isBlank()) {
            viewModel.clear()
            return@LaunchedEffect
        }
        viewModel.loadSuggestions(query)
        delay(300)
        viewModel.quickSearch(query)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = state.mode == SearchMode.TEXT,
                    onClick = {
                        viewModel.setMode(SearchMode.TEXT)
                        query = ""
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Text") }
                SegmentedButton(
                    selected = state.mode == SearchMode.CHECKSUM,
                    onClick = {
                        viewModel.setMode(SearchMode.CHECKSUM)
                        query = ""
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Checksum") }
            }
            if (state.mode == SearchMode.TEXT) {
                IconButton(onClick = { showFilters = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Advanced filters")
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    if (state.mode == SearchMode.TEXT) "Search artifacts and repositories"
                    else "Paste a checksum (SHA-256)",
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
        )

        if (state.mode == SearchMode.CHECKSUM) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.checksumSearch(query) },
                    label = { Text("Search by checksum") },
                )
            }
        } else if (state.suggestions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { query = suggestion },
                        label = { Text(suggestion) },
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isSearching -> CenteredProgress()
                state.error != null -> CenteredMessage(state.error!!, isError = true)
                state.mode == SearchMode.CHECKSUM -> ChecksumContent(state.checksumResults, state.hasSearched)
                !state.hasSearched -> InitialContent(state.recent, state.trending)
                state.results.isEmpty() -> CenteredMessage("No results for \"$query\"")
                else -> ResultList(state.results)
            }
        }
    }

    if (showFilters) {
        SearchFiltersSheet(
            initialQuery = query,
            onDismiss = { showFilters = false },
            onApply = { q, format, repositoryKey, minSize, maxSize ->
                query = q
                viewModel.advancedSearch(
                    query = q.ifBlank { null },
                    format = format,
                    repositoryKey = repositoryKey,
                    minSize = minSize,
                    maxSize = maxSize,
                )
                showFilters = false
            },
        )
    }
}

/** Formats offered in the advanced filter sheet. */
private val SearchFilterFormats = listOf("maven", "npm", "pypi", "docker", "nuget", "gem", "cargo", "go")

/**
 * Parse a human-entered size into bytes. Accepts a bare number (bytes) or a
 * number with a KB/MB/GB suffix (case-insensitive). Returns null for blank or
 * unparseable input so the filter is simply omitted.
 */
internal fun parseSizeToBytes(input: String): Long? {
    val trimmed = input.trim().lowercase()
    if (trimmed.isBlank()) return null
    val match = Regex("""^([0-9]*\.?[0-9]+)\s*(b|kb|mb|gb)?$""").find(trimmed) ?: return null
    val value = match.groupValues[1].toDoubleOrNull() ?: return null
    val multiplier = when (match.groupValues[2]) {
        "kb" -> 1_024.0
        "mb" -> 1_024.0 * 1_024
        "gb" -> 1_024.0 * 1_024 * 1_024
        else -> 1.0
    }
    return (value * multiplier).toLong()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFiltersSheet(
    initialQuery: String,
    onDismiss: () -> Unit,
    onApply: (query: String, format: String?, repositoryKey: String?, minSize: Long?, maxSize: Long?) -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var format by remember { mutableStateOf<String?>(null) }
    var repositoryKey by remember { mutableStateOf("") }
    var minSizeText by remember { mutableStateOf("") }
    var maxSizeText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Advanced filters", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Query (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Format", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = format == null,
                    onClick = { format = null },
                    label = { Text("Any") },
                )
                SearchFilterFormats.forEach { f ->
                    FilterChip(
                        selected = format == f,
                        onClick = { format = if (format == f) null else f },
                        label = { Text(f) },
                    )
                }
            }

            OutlinedTextField(
                value = repositoryKey,
                onValueChange = { repositoryKey = it },
                label = { Text("Repository key (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minSizeText,
                    onValueChange = { minSizeText = it },
                    label = { Text("Min size") },
                    placeholder = { Text("e.g. 10kb") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = maxSizeText,
                    onValueChange = { maxSizeText = it },
                    label = { Text("Max size") },
                    placeholder = { Text("e.g. 5mb") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        query = ""
                        format = null
                        repositoryKey = ""
                        minSizeText = ""
                        maxSizeText = ""
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset") }
                Button(
                    onClick = {
                        onApply(
                            query.trim(),
                            format,
                            repositoryKey.trim().ifBlank { null },
                            parseSizeToBytes(minSizeText),
                            parseSizeToBytes(maxSizeText),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun InitialContent(
    recent: List<SearchResultItem>,
    trending: List<SearchResultItem>,
) {
    if (recent.isEmpty() && trending.isEmpty()) {
        CenteredMessage("Search artifacts and repositories by name, format, or checksum")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (trending.isNotEmpty()) {
            item(key = "trending-header") { SectionHeader("Trending", icon = true) }
            items(trending, key = { "trending-${it.id}" }) { ResultRow(it) }
        }
        if (recent.isNotEmpty()) {
            item(key = "recent-header") { SectionHeader("Recent") }
            items(recent, key = { "recent-${it.id}" }) { ResultRow(it) }
        }
    }
}

@Composable
private fun ResultList(results: List<SearchResultItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(results, key = { it.id }) { ResultRow(it) }
    }
}

@Composable
private fun ChecksumContent(results: List<ChecksumArtifact>, hasSearched: Boolean) {
    when {
        !hasSearched -> CenteredMessage("Paste a checksum and tap Search by checksum")
        results.isEmpty() -> CenteredMessage("No artifacts match that checksum")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(results, key = { it.id }) { ChecksumRow(it) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon) {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ResultRow(item: SearchResultItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            ItemTitleWithChip(title = item.name, chipLabel = (item.format ?: item.type).uppercase())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.repositoryKey,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.version?.let {
                Text(
                    text = "v$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.sizeBytes?.let {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = "Size",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatBytes(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecksumRow(item: ChecksumArtifact) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            ItemTitleWithChip(title = item.name, chipLabel = item.contentType)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.repositoryKey} / ${item.path}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatBytes(item.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String, isError: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            if (!isError) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
