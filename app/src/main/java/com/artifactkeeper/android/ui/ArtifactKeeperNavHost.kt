package com.artifactkeeper.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.builds.BuildsScreen
import com.artifactkeeper.android.ui.screens.dashboard.DashboardScreen
import com.artifactkeeper.android.ui.screens.packages.PackagesScreen
import com.artifactkeeper.android.ui.screens.repositories.RepositoriesScreen
import com.artifactkeeper.android.ui.screens.repositories.RepositoryDetailScreen
import com.artifactkeeper.android.ui.screens.search.SearchScreen
import com.artifactkeeper.android.ui.screens.security.SecurityScreen
import com.artifactkeeper.android.ui.screens.settings.SettingsScreen
import com.artifactkeeper.android.ui.screens.welcome.WelcomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactKeeperNavHost() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("artifact_keeper_prefs", android.content.Context.MODE_PRIVATE)
    }

    // Restore saved server URL and auth token on app start
    var isConfigured by remember {
        val savedUrl = prefs.getString("server_url", null)
        val savedToken = prefs.getString("auth_token", null)
        if (!savedUrl.isNullOrBlank()) {
            ApiClient.configure(savedUrl, savedToken)
        }
        mutableStateOf(savedUrl?.isNotBlank() == true)
    }

    if (!isConfigured) {
        WelcomeScreen(
            onConnected = {
                isConfigured = true
            },
        )
    } else {
        MainAppScaffold(
            onDisconnect = {
                prefs.edit()
                    .remove("server_url")
                    .remove("auth_token")
                    .remove("user_id")
                    .remove("user_username")
                    .remove("user_email")
                    .remove("user_is_admin")
                    .apply()
                ApiClient.clearConfig()
                isConfigured = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScaffold(onDisconnect: () -> Unit) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple("dashboard", "Dashboard", Icons.Default.Home),
        Triple("repos", "Repos", Icons.Default.Folder),
        Triple("packages", "Packages", Icons.Default.Inventory2),
        Triple("builds", "Builds", Icons.Default.Build),
        Triple("search", "Search", Icons.Default.Search),
        Triple("security", "Security", Icons.Default.Shield),
    )

    val tabRoutes = tabs.map { it.first }.toSet()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabRoutes || currentRoute == null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEachIndexed { index, (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                navController.navigate(route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                )
            }
            composable("repos") {
                RepositoriesScreen(
                    onRepoClick = { key ->
                        navController.navigate("repos/$key")
                    },
                )
            }
            composable("packages") { PackagesScreen() }
            composable("builds") { BuildsScreen() }
            composable("search") { SearchScreen() }
            composable("security") { SecurityScreen() }
            composable("repos/{key}") { backStackEntry ->
                val key = backStackEntry.arguments?.getString("key") ?: return@composable
                RepositoryDetailScreen(
                    repoKey = key,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onDisconnect = onDisconnect,
                )
            }
        }
    }
}
