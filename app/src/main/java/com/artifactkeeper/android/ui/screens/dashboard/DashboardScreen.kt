package com.artifactkeeper.android.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Dashboard") })
        Text("Dashboard content", modifier = Modifier.padding())
    }
}
