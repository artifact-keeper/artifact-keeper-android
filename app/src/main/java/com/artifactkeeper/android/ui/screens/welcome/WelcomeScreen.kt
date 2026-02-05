package com.artifactkeeper.android.ui.screens.welcome

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artifactkeeper.android.R
import com.artifactkeeper.android.data.ServerManager
import com.artifactkeeper.android.data.api.ApiClient
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(onConnected: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("artifact_keeper_prefs", android.content.Context.MODE_PRIVATE)
    }

    var serverUrl by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // App logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Artifact Keeper",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome to Artifact Keeper",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Connect to your Artifact Keeper server to get started.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Server URL") },
                placeholder = { Text("https://artifacts.example.com") },
                singleLine = true,
                isError = errorMessage != null,
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val url = serverUrl.trim()

                    // Validate URL format
                    if (url.isBlank()) {
                        errorMessage = "Please enter a server URL"
                        return@Button
                    }
                    if (!Patterns.WEB_URL.matcher(url).matches()) {
                        errorMessage = "Please enter a valid URL"
                        return@Button
                    }

                    coroutineScope.launch {
                        isTesting = true
                        errorMessage = null
                        try {
                            ApiClient.configure(url)
                            ApiClient.api.getHealth()
                            // Connection succeeded -- save and register with ServerManager
                            prefs.edit().putString("server_url", url).apply()
                            val host = try {
                                java.net.URI(url).host ?: url
                            } catch (_: Exception) {
                                url
                            }
                            ServerManager.addServer(name = host, url = url)
                            onConnected()
                        } catch (e: Exception) {
                            val detail = e.message ?: "Unknown error"
                            errorMessage = "Could not connect to server: $detail"
                            ApiClient.clearConfig()
                        } finally {
                            isTesting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting,
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isTesting) "Connecting..." else "Connect")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Learn more at artifactkeeper.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://artifactkeeper.com"))
                        context.startActivity(intent)
                    }
                    .padding(vertical = 16.dp),
            )
        }
    }
}
