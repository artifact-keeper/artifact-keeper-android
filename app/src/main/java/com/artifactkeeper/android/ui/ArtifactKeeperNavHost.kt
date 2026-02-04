package com.artifactkeeper.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.artifactkeeper.android.data.ServerManager
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.UserInfo
import com.artifactkeeper.android.ui.components.AccountMenu
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
import com.artifactkeeper.android.ui.screens.security.ScanFindingsScreen
import com.artifactkeeper.android.ui.screens.security.ScansScreen
import com.artifactkeeper.android.ui.screens.security.SecurityScreen
import com.artifactkeeper.android.ui.screens.settings.SettingsScreen
import com.artifactkeeper.android.ui.screens.auth.ChangePasswordScreen
import com.artifactkeeper.android.ui.screens.welcome.WelcomeScreen

private data class BottomTab(
    val route: String,
    val label: String,
    val compactLabel: String,
    val icon: ImageVector,
)

private val allBottomTabs = listOf(
    BottomTab("artifacts", "Artifacts", "Artifacts", Icons.Default.Inventory2),
    BottomTab("integration", "Integration", "Integr.", Icons.Default.Link),
    BottomTab("security", "Security", "Security", Icons.Default.Shield),
    BottomTab("operations", "Operations", "Ops", Icons.Default.BarChart),
    BottomTab("admin", "Admin", "Admin", Icons.Default.AdminPanelSettings),
)

private fun visibleTabs(isLoggedIn: Boolean, isAdmin: Boolean): List<BottomTab> {
    return allBottomTabs.filter { tab ->
        when (tab.route) {
            "artifacts" -> true
            "admin" -> isAdmin
            else -> isLoggedIn // integration, security, operations
        }
    }
}

private val allSectionRoutes = allBottomTabs.map { it.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactKeeperNavHost(
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Medium,
) {
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
            widthSizeClass = widthSizeClass,
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
private fun MainAppScaffold(
    widthSizeClass: WindowWidthSizeClass,
    onDisconnect: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("artifact_keeper_prefs", android.content.Context.MODE_PRIVATE)
    }
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }
    val useRail = widthSizeClass == WindowWidthSizeClass.Expanded
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    // Shared auth state
    var currentUser by remember { mutableStateOf<UserInfo?>(null) }
    var mustChangePassword by remember { mutableStateOf(false) }
    var changePasswordUserId by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val savedToken = prefs.getString("auth_token", null)
        val savedUsername = prefs.getString("user_username", null)
        val savedUserId = prefs.getString("user_id", null)
        val savedEmail = prefs.getString("user_email", null)
        val savedIsAdmin = prefs.getBoolean("user_is_admin", false)
        if (savedToken != null && savedUsername != null && savedUserId != null) {
            currentUser = UserInfo(
                id = savedUserId,
                username = savedUsername,
                email = savedEmail,
                isAdmin = savedIsAdmin,
            )
        }
    }

    val accountActions: @Composable () -> Unit = {
        AccountMenu(
            currentUser = currentUser,
            onLoggedIn = { user, token, forceChangePassword ->
                currentUser = user
                prefs.edit()
                    .putString("auth_token", token)
                    .putString("user_id", user.id)
                    .putString("user_username", user.username)
                    .putString("user_email", user.email)
                    .putBoolean("user_is_admin", user.isAdmin)
                    .apply()
                if (forceChangePassword) {
                    changePasswordUserId = user.id
                    mustChangePassword = true
                }
            },
            onLoggedOut = {
                currentUser = null
                ApiClient.setToken(null)
                prefs.edit()
                    .remove("auth_token")
                    .remove("user_id")
                    .remove("user_username")
                    .remove("user_email")
                    .remove("user_is_admin")
                    .apply()
            },
        )
    }

    val isLoggedIn = currentUser != null
    val isAdmin = currentUser?.isAdmin == true
    val bottomTabs = visibleTabs(isLoggedIn, isAdmin)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Sync selectedTab when route changes (e.g. back navigation)
    LaunchedEffect(currentRoute, bottomTabs) {
        val idx = bottomTabs.indexOfFirst { it.route == currentRoute }
        if (idx >= 0) selectedTab = idx
    }

    // Reset to first tab if current tab becomes hidden after logout
    LaunchedEffect(bottomTabs) {
        if (selectedTab >= bottomTabs.size) {
            selectedTab = 0
            navController.navigate("artifacts") {
                popUpTo("artifacts") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val showNav = currentRoute in allSectionRoutes || currentRoute == null

    if (mustChangePassword) {
        ChangePasswordScreen(
            userId = changePasswordUserId,
            onPasswordChanged = {
                mustChangePassword = false
            },
            onLogout = {
                mustChangePassword = false
                currentUser = null
                ApiClient.setToken(null)
                prefs.edit()
                    .remove("auth_token")
                    .remove("user_id")
                    .remove("user_username")
                    .remove("user_email")
                    .remove("user_is_admin")
                    .apply()
            },
        )
        return
    }

    if (useRail && showNav) {
        // Expanded: NavigationRail on the left
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                bottomTabs.forEachIndexed { index, tab ->
                    NavigationRailItem(
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
            NavHost(
                navController = navController,
                startDestination = "artifacts",
                modifier = Modifier.fillMaxHeight().weight(1f),
            ) {
                composable("artifacts") {
                    ArtifactsSection(
                        isCompact = false,
                        onRepoClick = { key -> navController.navigate("repos/$key") },
                        accountActions = accountActions,
                    )
                }
                composable("integration") { IntegrationSection(isCompact = false, accountActions = accountActions) }
                composable("security") { SecuritySection(isCompact = false, accountActions = accountActions) }
                composable("operations") { OperationsSection(isCompact = false, accountActions = accountActions) }
                composable("admin") { AdminSection(isCompact = false, onDisconnect = onDisconnect, accountActions = accountActions) }
                composable("repos/{key}") { backStackEntry ->
                    val key = backStackEntry.arguments?.getString("key") ?: return@composable
                    RepositoryDetailScreen(
                        repoKey = key,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    } else {
        // Compact / Medium: Bottom NavigationBar
        Scaffold(
            bottomBar = {
                if (showNav) {
                    NavigationBar {
                        bottomTabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = {
                                    Text(
                                        text = if (isCompact) tab.compactLabel else tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
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
                        isCompact = isCompact,
                        onRepoClick = { key -> navController.navigate("repos/$key") },
                        accountActions = accountActions,
                    )
                }
                composable("integration") { IntegrationSection(isCompact = isCompact, accountActions = accountActions) }
                composable("security") { SecuritySection(isCompact = isCompact, accountActions = accountActions) }
                composable("operations") { OperationsSection(isCompact = isCompact, accountActions = accountActions) }
                composable("admin") { AdminSection(isCompact = isCompact, onDisconnect = onDisconnect, accountActions = accountActions) }
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
}

// ---------------------------------------------------------------------------
// Section composables with adaptive sub-tab rows
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtifactsSection(
    isCompact: Boolean,
    onRepoClick: (String) -> Unit,
    accountActions: @Composable () -> Unit,
) {
    val subTabs = if (isCompact) listOf("Repos", "Pkgs", "Builds", "Search")
                  else listOf("Repositories", "Packages", "Builds", "Search")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Artifacts") }, actions = { accountActions() })
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = if (isCompact) 4.dp else 16.dp,
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) },
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
private fun IntegrationSection(isCompact: Boolean, accountActions: @Composable () -> Unit) {
    val subTabs = listOf("Peers", "Replication", "Webhooks")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Integration") }, actions = { accountActions() })
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = if (isCompact) 4.dp else 16.dp,
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) },
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
private fun SecuritySection(isCompact: Boolean, accountActions: @Composable () -> Unit) {
    val subTabs = listOf("Dashboard", "Scans", "Policies")
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedScanId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedScanId != null) {
            ScanFindingsScreen(
                scanId = selectedScanId!!,
                onBack = { selectedScanId = null },
            )
        } else {
            TopAppBar(title = { Text("Security") }, actions = { accountActions() })
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = if (isCompact) 4.dp else 16.dp,
            ) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) },
                    )
                }
            }
            when (selectedTab) {
                0 -> SecurityScreen()
                1 -> ScansScreen(onScanClick = { selectedScanId = it })
                2 -> PoliciesScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationsSection(isCompact: Boolean, accountActions: @Composable () -> Unit) {
    val subTabs = if (isCompact) listOf("Stats", "Health", "Metrics")
                  else listOf("Analytics", "Monitoring", "Telemetry")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Operations") }, actions = { accountActions() })
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = if (isCompact) 4.dp else 16.dp,
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) },
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
private fun AdminSection(isCompact: Boolean, onDisconnect: () -> Unit, accountActions: @Composable () -> Unit) {
    val subTabs = listOf("Users", "Groups", "SSO", "Settings")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Admin") }, actions = { accountActions() })
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = if (isCompact) 4.dp else 16.dp,
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) },
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
