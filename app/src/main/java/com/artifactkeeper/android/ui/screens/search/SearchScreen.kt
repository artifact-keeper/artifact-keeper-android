package com.artifactkeeper.android.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Search") })
        Text("Search content", modifier = Modifier.padding())
    }
}
