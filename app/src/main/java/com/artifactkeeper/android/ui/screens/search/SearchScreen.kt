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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
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
