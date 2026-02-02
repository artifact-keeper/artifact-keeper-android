package com.artifactkeeper.android.ui.screens.repositories

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoriesScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Repositories") })
        Text("Repositories content", modifier = Modifier.padding())
    }
}
