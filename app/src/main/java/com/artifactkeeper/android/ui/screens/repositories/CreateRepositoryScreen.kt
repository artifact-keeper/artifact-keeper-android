package com.artifactkeeper.android.ui.screens.repositories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.api.unwrap
import com.artifactkeeper.client.models.CreateRepositoryRequest
import kotlinx.coroutines.launch

// Supported package formats (matches the backend)
private val FORMATS = listOf(
    "maven", "npm", "pypi", "docker", "nuget", "cargo", "go", "helm",
    "rubygems", "composer", "cocoapods", "swift", "pub", "hex", "conan",
    "conda", "opkg", "rpm", "deb", "apk", "generic",
)

private val REPO_TYPES = listOf("local", "remote", "virtual")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRepositoryScreen(
    onBack: () -> Unit,
    onCreated: (repoKey: String) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("local") }
    var selectedFormat by remember { mutableStateOf("generic") }
    var upstreamUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var typeExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Auto-generate key from name
    LaunchedEffect(name) {
        if (key.isEmpty() || key == name.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-')) {
            key = name.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-')
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Repository") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Name") },
                placeholder = { Text("My Maven Repository") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSaving,
            )

            // Key
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '-' }
                    errorMessage = null
                },
                label = { Text("Key") },
                supportingText = { Text("URL-safe identifier, lowercase letters, digits, and hyphens") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSaving,
            )

            // Repository type dropdown
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded },
            ) {
                OutlinedTextField(
                    value = selectedType.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !isSaving,
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                ) {
                    REPO_TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                selectedType = type
                                typeExpanded = false
                            },
                        )
                    }
                }
            }

            // Format dropdown
            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = !formatExpanded },
            ) {
                OutlinedTextField(
                    value = selectedFormat.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Format") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !isSaving,
                )
                ExposedDropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false },
                ) {
                    FORMATS.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format.uppercase()) },
                            onClick = {
                                selectedFormat = format
                                formatExpanded = false
                            },
                        )
                    }
                }
            }

            // Upstream URL (only for remote type)
            if (selectedType == "remote") {
                OutlinedTextField(
                    value = upstreamUrl,
                    onValueChange = {
                        upstreamUrl = it
                        errorMessage = null
                    },
                    label = { Text("Upstream URL") },
                    placeholder = { Text("https://repo1.maven.org/maven2/") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                )
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    errorMessage = null
                },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !isSaving,
            )

            // Public toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Public", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Allow anonymous read access",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    enabled = !isSaving,
                )
            }

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Create button
            Button(
                onClick = {
                    errorMessage = null
                    if (name.isBlank()) {
                        errorMessage = "Name is required"
                        return@Button
                    }
                    if (key.isBlank()) {
                        errorMessage = "Key is required"
                        return@Button
                    }
                    if (selectedType == "remote" && upstreamUrl.isBlank()) {
                        errorMessage = "Upstream URL is required for remote repositories"
                        return@Button
                    }

                    coroutineScope.launch {
                        isSaving = true
                        try {
                            val request = CreateRepositoryRequest(
                                key = key,
                                name = name,
                                repoType = selectedType,
                                format = selectedFormat,
                                upstreamUrl = if (selectedType == "remote") upstreamUrl.ifBlank { null } else null,
                                description = description.ifBlank { null },
                                isPublic = isPublic,
                            )
                            val repo = ApiClient.reposApi.createRepository(request).unwrap()
                            onCreated(repo.key)
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to create repository"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create Repository")
            }
        }
    }
}
