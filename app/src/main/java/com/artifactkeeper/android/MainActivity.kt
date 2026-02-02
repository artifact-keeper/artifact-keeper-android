package com.artifactkeeper.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.artifactkeeper.android.ui.ArtifactKeeperNavHost
import com.artifactkeeper.android.ui.theme.ArtifactKeeperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArtifactKeeperTheme {
                ArtifactKeeperNavHost()
            }
        }
    }
}
