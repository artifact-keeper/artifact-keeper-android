package com.artifactkeeper.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.artifactkeeper.android.data.ServerManager
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.ui.screens.admin.GroupsScreen
import com.artifactkeeper.android.ui.screens.admin.SSOScreen
import com.artifactkeeper.android.ui.screens.admin.UsersScreen
import com.artifactkeeper.android.ui.screens.builds.BuildsScreen
import com.artifactkeeper.android.ui.screens.integration.PeersScreen
import com.artifactkeeper.android.ui.screens.integration.ReplicationScreen
import com.artifactkeeper.android.ui.screens.integration.WebhooksScreen
import com.artifactkeeper.android.ui.screens.operations.AnalyticsScreen
import com.artifactkeeper.android.ui.screens.operations.MonitoringScreen
import com.artifactkeeper.android.ui.screens.operations.TelemetryScreen
import com.artifactkeeper.android.ui.screens.packages.PackagesScreen
import com.artifactkeeper.android.ui.screens.repositories.RepositoriesScreen
import com.artifactkeeper.android.ui.screens.repositories.RepositoryDetailScreen
import com.artifactkeeper.android.ui.screens.search.SearchScreen
import com.artifactkeeper.android.ui.screens.security.PoliciesScreen
import com.artifactkeeper.android.ui.screens.security.ScansScreen
import com.artifactkeeper.android.ui.screens.security.SecurityScreen
import com.artifactkeeper.android.ui.screens.settings.SettingsScreen
import com.artifactkeeper.android.ui.screens.welcome.WelcomeScreen

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab("artifacts", "Artifacts", Icons.Default.Inventory2),
    BottomTab("integration", "Integration", Icons.Default.Link),
    BottomTab("security", "Security", Icons.Default.Shield),
    BottomTab("operations", "Operations", Icons.Default.BarChart),
    BottomTab("admin", "Admin", Icons.Default.AdminPanelSettings),
)

private val sectionRoutes = bottomTabs.map { it.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactKeeperNavHost() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("artifact_keeper_prefs", android.content.Context.MODE_PRIVATE)
    }

    // Initialize ServerManager
    LaunchedEffect(Unit) {
        ServerManager.init(context)
        ServerManager.migrateIfNeeded(context)
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Sync selectedTab when route changes (e.g. back navigation)
    LaunchedEffect(currentRoute) {
        val idx = bottomTabs.indexOfFirst { it.route == currentRoute }
        if (idx >= 0) selectedTab = idx
    }

    val showBottomBar = currentRoute in sectionRoutes || currentRoute == null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                navController.navigate(tab.route) {
                                    popUpTo("artifacts") { saveState = true }
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
            startDestination = "artifacts",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("artifacts") {
                ArtifactsSection(
                    onRepoClick = { key -> navController.navigate("repos/$key") },
                )
            }
            composable("integration") {
                IntegrationSection()
            }
            composable("security") {
                SecuritySection()
            }
            composable("operations") {
                OperationsSection()
            }
            composable("admin") {
                AdminSection(onDisconnect = onDisconnect)
            }
            composable("repos/{key}") { backStackEntry ->
                val key = backStackEntry.arguments?.getString("key") ?: return@composable
                RepositoryDetailScreen(
                    repoKey = key,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section composables with sub-tab rows
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtifactsSection(onRepoClick: (String) -> Unit) {
    val subTabs = listOf("Repositories", "Packages", "Builds", "Search")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Artifacts") })
        TabRow(selectedTabIndex = selectedTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        when (selectedTab) {
            0 -> RepositoriesScreen(onRepoClick = onRepoClick)
            1 -> PackagesScreen()
            2 -> BuildsScreen()
            3 -> SearchScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegrationSection() {
    val subTabs = listOf("Peers", "Replication", "Webhooks")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Integration") })
        TabRow(selectedTabIndex = selectedTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        when (selectedTab) {
            0 -> PeersScreen()
            1 -> ReplicationScreen()
            2 -> WebhooksScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecuritySection() {
    val subTabs = listOf("Dashboard", "Scans", "Policies")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Security") })
        TabRow(selectedTabIndex = selectedTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        when (selectedTab) {
            0 -> SecurityScreen()
            1 -> ScansScreen()
            2 -> PoliciesScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationsSection() {
    val subTabs = listOf("Analytics", "Monitoring", "Telemetry")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Operations") })
        TabRow(selectedTabIndex = selectedTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        when (selectedTab) {
            0 -> AnalyticsScreen()
            1 -> MonitoringScreen()
            2 -> TelemetryScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminSection(onDisconnect: () -> Unit) {
    val subTabs = listOf("Users", "Groups", "SSO", "Settings")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Admin") })
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        when (selectedTab) {
            0 -> UsersScreen()
            1 -> GroupsScreen()
            2 -> SSOScreen()
            3 -> SettingsScreen(
                onBack = { selectedTab = 0 },
                onDisconnect = onDisconnect,
            )
        }
    }
}
