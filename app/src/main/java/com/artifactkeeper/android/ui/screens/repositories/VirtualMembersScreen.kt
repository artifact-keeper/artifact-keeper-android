package com.artifactkeeper.android.ui.screens.repositories

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artifactkeeper.android.data.models.Repository
import com.artifactkeeper.android.data.models.VirtualMember
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualMembersScreen(
    repoKey: String,
    repoName: String,
    repoFormat: String,
    onBack: () -> Unit,
    viewModel: VirtualMembersViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var memberToDelete by remember { mutableStateOf<VirtualMember?>(null) }

    // Track dragging state
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(repoKey) {
        viewModel.loadMembers(repoKey)
    }

    // Show error/success messages
    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$repoName Members") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.loadEligibleRepos(repoKey, repoFormat)
                    showAddDialog = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.members.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No members",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add local or remote repositories as members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> {
                val listState = rememberLazyListState()
                val members = uiState.members

                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.loadMembers(repoKey) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text(
                                text = "Drag to reorder. Lower priority = higher precedence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        itemsIndexed(
                            items = members,
                            key = { _, member -> member.id },
                        ) { index, member ->
                            val isDragging = draggedItemIndex == index
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 8.dp else 1.dp,
                                label = "elevation",
                            )

                            MemberCard(
                                member = member,
                                index = index,
                                isDragging = isDragging,
                                elevation = elevation,
                                dragOffset = if (isDragging) dragOffset else 0f,
                                onDelete = { memberToDelete = member },
                                onDragStart = { draggedItemIndex = index },
                                onDrag = { delta ->
                                    dragOffset += delta
                                    // Calculate new index based on drag offset
                                    val itemHeight = 80f // Approximate item height in dp
                                    val indexOffset = (dragOffset / itemHeight).toInt()
                                    val newIndex = (index + indexOffset).coerceIn(0, members.lastIndex)
                                    if (newIndex != index && newIndex != draggedItemIndex) {
                                        // Reorder the list
                                        val mutableList = members.toMutableList()
                                        val draggedItem = mutableList.removeAt(draggedItemIndex)
                                        mutableList.add(newIndex, draggedItem)
                                        draggedItemIndex = newIndex
                                        dragOffset = 0f
                                    }
                                },
                                onDragEnd = {
                                    if (draggedItemIndex != index) {
                                        // Create reordered list with new priorities
                                        val reorderedMembers = members.toMutableList()
                                        val draggedItem = reorderedMembers.removeAt(index)
                                        reorderedMembers.add(draggedItemIndex.coerceIn(0, reorderedMembers.size), draggedItem)
                                        viewModel.reorderMembers(reorderedMembers)
                                    }
                                    draggedItemIndex = -1
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedItemIndex = -1
                                    dragOffset = 0f
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Member Dialog
    if (showAddDialog) {
        AddMemberDialog(
            eligibleRepos = uiState.eligibleRepos,
            isLoading = uiState.isLoadingEligible,
            onDismiss = { showAddDialog = false },
            onAdd = { repo ->
                viewModel.addMember(repo.key)
                showAddDialog = false
            },
        )
    }

    // Delete Confirmation Dialog
    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Remove Member") },
            text = { Text("Remove ${member.memberRepoName} from this virtual repository?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(member.memberRepoKey)
                        memberToDelete = null
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun MemberCard(
    member: VirtualMember,
    index: Int,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragOffset: Float,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else 0f
            }
            .shadow(elevation)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Drag handle
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragCancel() },
                        )
                    },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Priority badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = "${member.priority}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Member info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.memberRepoName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                member.memberRepoType.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                    Text(
                        text = member.memberRepoKey,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberDialog(
    eligibleRepos: List<Repository>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAdd: (Repository) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member Repository") },
        text = {
            Column {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    eligibleRepos.isEmpty() -> {
                        Text(
                            text = "No eligible repositories found. Only local and remote repositories with the same format can be added as members.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        Text(
                            text = "Select a repository to add as a member:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                count = eligibleRepos.size,
                                key = { eligibleRepos[it].id },
                            ) { index ->
                                val repo = eligibleRepos[index]
                                ElevatedCard(
                                    onClick = { onAdd(repo) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = repo.name,
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Text(
                                                text = repo.key,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    repo.repoType.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
