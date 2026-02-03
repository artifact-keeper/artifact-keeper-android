package com.artifactkeeper.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.artifactkeeper.android.ui.screens.builds.BuildsScreen
import com.artifactkeeper.android.ui.screens.dashboard.DashboardScreen
import com.artifactkeeper.android.ui.screens.packages.PackagesScreen
import com.artifactkeeper.android.ui.screens.repositories.RepositoriesScreen
import com.artifactkeeper.android.ui.screens.repositories.RepositoryDetailScreen
import com.artifactkeeper.android.ui.screens.search.SearchScreen
import com.artifactkeeper.android.ui.screens.security.SecurityScreen
import com.artifactkeeper.android.ui.screens.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactKeeperNavHost() {
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
                )
            }
        }
    }
}
