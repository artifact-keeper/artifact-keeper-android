package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Security") })
        Text("Security content", modifier = Modifier.padding())
    }
}
