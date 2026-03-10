package com.artifactkeeper.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.artifactkeeper.android.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.artifactkeeper.android.data.EncryptedPrefsManager
import com.artifactkeeper.android.data.ServerManager
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.UserInfo
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import com.artifactkeeper.android.ui.components.AccountMenu
import com.artifactkeeper.android.ui.screens.admin.GroupsScreen
import com.artifactkeeper.android.ui.screens.admin.SSOScreen
import com.artifactkeeper.android.ui.screens.admin.UsersScreen
import com.artifactkeeper.android.ui.screens.builds.BuildDetailScreen
import com.artifactkeeper.android.ui.screens.builds.BuildsScreen
import com.artifactkeeper.android.ui.screens.integration.PeersScreen
import com.artifactkeeper.android.ui.screens.integration.ReplicationScreen
import com.artifactkeeper.android.ui.screens.integration.WebhooksScreen
import com.artifactkeeper.android.ui.screens.operations.AnalyticsScreen
import com.artifactkeeper.android.ui.screens.operations.MonitoringScreen
import com.artifactkeeper.android.ui.screens.operations.TelemetryScreen
import com.artifactkeeper.android.ui.screens.packages.PackageDetailScreen
import com.artifactkeeper.android.ui.screens.packages.PackagesScreen
import com.artifactkeeper.android.ui.screens.repositories.CreateRepositoryScreen
import com.artifactkeeper.android.ui.screens.repositories.RepositoriesScreen
import com.artifactkeeper.android.ui.screens.repositories.RepositoryDetailScreen
import com.artifactkeeper.android.ui.screens.repositories.VirtualMembersScreen
import com.artifactkeeper.android.ui.screens.search.SearchScreen
import com.artifactkeeper.android.ui.screens.security.LicensePoliciesScreen
import com.artifactkeeper.android.ui.screens.security.PoliciesScreen
import com.artifactkeeper.android.ui.screens.security.SbomScreen
import com.artifactkeeper.android.ui.screens.security.ScanFindingsScreen
import com.artifactkeeper.android.ui.screens.security.ScansScreen
import com.artifactkeeper.android.ui.screens.security.SecurityScreen
import com.artifactkeeper.android.ui.screens.settings.SettingsScreen
import com.artifactkeeper.android.ui.screens.auth.ChangePasswordScreen
import com.artifactkeeper.android.ui.screens.profile.ApiTokensScreen
import com.artifactkeeper.android.ui.screens.profile.ProfileScreen
import com.artifactkeeper.android.ui.screens.welcome.WelcomeScreen
import com.artifactkeeper.android.ui.screens.staging.StagingListScreen
import com.artifactkeeper.android.ui.screens.staging.StagingDetailScreen
import com.artifactkeeper.android.ui.screens.staging.PromotionHistoryScreen
import com.artifactkeeper.android.ui.screens.staging.StagingViewModel
import androidx.hilt.navigation.compose.hiltViewModel

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
    val prefs = remember { EncryptedPrefsManager.getPrefs(context) }

    // Initialize ServerManager
    LaunchedEffect(Unit) {
        ServerManager.init(context)
        ServerManager.migrateIfNeeded(context)
    }

    // Restore saved server URL and auth token on app start
    var isConfigured by remember {
        val savedUrl = prefs.getString(EncryptedPrefsManager.KEY_SERVER_URL, null)
        val savedToken = prefs.getString(EncryptedPrefsManager.KEY_AUTH_TOKEN, null)
        if (!savedUrl.isNullOrBlank()) {
            ApiClient.configure(savedUrl, savedToken)
        }
        mutableStateOf(savedUrl?.isNotBlank() == true)
    }

    val outerActiveServerId by ServerManager.activeServerId.collectAsState()

    if (!isConfigured) {
        WelcomeScreen(
            onConnected = {
                isConfigured = true
            },
        )
    } else {
        key(outerActiveServerId) {
        MainAppScaffold(
            widthSizeClass = widthSizeClass,
            onDisconnect = {
                EncryptedPrefsManager.clearAll(context)
                ApiClient.clearConfig()
                isConfigured = false
            },
        )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScaffold(
    widthSizeClass: WindowWidthSizeClass,
    onDisconnect: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { EncryptedPrefsManager.getPrefs(context) }
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }
    val useRail = widthSizeClass == WindowWidthSizeClass.Expanded
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    // Shared auth state
    var currentUser by remember { mutableStateOf<UserInfo?>(null) }
    var mustChangePassword by remember { mutableStateOf(false) }
    var changePasswordUserId by remember { mutableStateOf("") }
    var showProfile by remember { mutableStateOf(false) }
    var showApiTokens by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val savedToken = EncryptedPrefsManager.getString(context, EncryptedPrefsManager.KEY_AUTH_TOKEN)
        val savedUsername = EncryptedPrefsManager.getString(context, EncryptedPrefsManager.KEY_USER_USERNAME)
        val savedUserId = EncryptedPrefsManager.getString(context, EncryptedPrefsManager.KEY_USER_ID)
        val savedEmail = EncryptedPrefsManager.getString(context, EncryptedPrefsManager.KEY_USER_EMAIL)
        val savedIsAdmin = EncryptedPrefsManager.getBoolean(context, EncryptedPrefsManager.KEY_USER_IS_ADMIN)
        if (savedToken != null && savedUsername != null && savedUserId != null) {
            currentUser = UserInfo(
                id = java.util.UUID.fromString(savedUserId),
                username = savedUsername,
                email = savedEmail ?: "",
                isAdmin = savedIsAdmin,
                totpEnabled = false,
            )
        }
    }

    val servers by ServerManager.servers.collectAsState()
    val activeServerId by ServerManager.activeServerId.collectAsState()
    val serverStatuses by ServerManager.serverStatuses.collectAsState()
    val statusCoroutineScope = rememberCoroutineScope()

    val accountActions: @Composable () -> Unit = {
        AccountMenu(
            currentUser = currentUser,
            servers = servers,
            activeServerId = activeServerId,
            serverStatuses = serverStatuses,
            onLoggedIn = { user, token, forceChangePassword ->
                currentUser = user
                EncryptedPrefsManager.saveLoginData(
                    context = context,
                    token = token,
                    userId = user.id.toString(),
                    username = user.username,
                    email = user.email,
                    isAdmin = user.isAdmin,
                )
                if (forceChangePassword) {
                    changePasswordUserId = user.id.toString()
                    mustChangePassword = true
                }
            },
            onLoggedOut = {
                currentUser = null
                ApiClient.setToken(null)
                EncryptedPrefsManager.clearAuthData(context)
            },
            onProfileClick = {
                if (currentUser != null) {
                    showProfile = true
                }
            },
            onSwitchServer = { serverId ->
                currentUser = null
                ApiClient.setToken(null)
                EncryptedPrefsManager.clearAuthData(context)
                ServerManager.switchTo(serverId)
                val server = ServerManager.getActiveServer()
                if (server != null) {
                    EncryptedPrefsManager.putString(context, EncryptedPrefsManager.KEY_SERVER_URL, server.url)
                }
            },
            onAddServer = { name, url ->
                ServerManager.addServer(name = name, url = url)
                val server = ServerManager.getActiveServer()
                if (server != null) {
                    EncryptedPrefsManager.putString(context, EncryptedPrefsManager.KEY_SERVER_URL, server.url)
                    currentUser = null
                    ApiClient.setToken(null)
                    EncryptedPrefsManager.clearAuthData(context)
                }
            },
            onRemoveServer = { serverId ->
                val wasActive = serverId == activeServerId
                ServerManager.removeServer(serverId)
                if (wasActive) {
                    val remaining = ServerManager.getActiveServer()
                    if (remaining != null) {
                        EncryptedPrefsManager.putString(context, EncryptedPrefsManager.KEY_SERVER_URL, remaining.url)
                    } else {
                        onDisconnect()
                    }
                    currentUser = null
                    ApiClient.setToken(null)
                    EncryptedPrefsManager.clearAuthData(context)
                }
            },
            onRefreshStatuses = {
                statusCoroutineScope.launch {
                    ServerManager.refreshStatuses()
                }
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

    if (showApiTokens) {
        ApiTokensScreen(
            onBack = { showApiTokens = false },
        )
        return
    }

    if (showProfile && currentUser != null) {
        ProfileScreen(
            user = currentUser!!,
            onDismiss = { showProfile = false },
            onUserUpdated = { updatedUser ->
                currentUser = updatedUser
                EncryptedPrefsManager.saveLoginData(
                    context = context,
                    token = EncryptedPrefsManager.getString(context, EncryptedPrefsManager.KEY_AUTH_TOKEN) ?: "",
                    userId = updatedUser.id.toString(),
                    username = updatedUser.username,
                    email = updatedUser.email,
                    isAdmin = updatedUser.isAdmin,
                )
            },
            onNavigateToTokens = {
                showProfile = false
                showApiTokens = true
            },
        )
        return
    }

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
                EncryptedPrefsManager.clearAuthData(context)
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
                        onCreateRepo = { navController.navigate("create-repo") },
                        onPackageClick = { id -> navController.navigate("packages/$id") },
                        onBuildClick = { id -> navController.navigate("builds/$id") },
                        accountActions = accountActions,
                    )
                }
                composable("integration") { IntegrationSection(isCompact = false, accountActions = accountActions) }
                composable("security") { SecuritySection(isCompact = false, accountActions = accountActions) }
                composable("operations") { OperationsSection(isCompact = false, accountActions = accountActions) }
                composable("admin") { AdminSection(isCompact = false, onDisconnect = onDisconnect, accountActions = accountActions) }
                composable("create-repo") {
                    CreateRepositoryScreen(
                        onBack = { navController.popBackStack() },
                        onCreated = { repoKey ->
                            navController.popBackStack()
                            navController.navigate("repos/$repoKey")
                        },
                    )
                }
                composable("repos/{key}") { backStackEntry ->
                    val key = backStackEntry.arguments?.getString("key") ?: return@composable
                    RepositoryDetailScreen(
                        repoKey = key,
                        onBack = { navController.popBackStack() },
                        onArtifactSecurityClick = { id, name ->
                            navController.navigate("artifacts/$id/security?name=$name")
                        },
                        onNavigateToMembers = { repoKey, repoName, repoFormat ->
                            navController.navigate("repos/$repoKey/members?name=$repoName&format=$repoFormat")
                        },
                    )
                }
                composable("packages/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable
                    PackageDetailScreen(
                        packageId = id,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("builds/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable
                    BuildDetailScreen(
                        buildId = id,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("artifacts/{id}/security?name={name}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable
                    val name = backStackEntry.arguments?.getString("name") ?: "Artifact"
                    SbomScreen(
                        artifactId = id,
                        artifactName = name,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("repos/{key}/members?name={name}&format={format}") { backStackEntry ->
                    val key = backStackEntry.arguments?.getString("key") ?: return@composable
                    val name = backStackEntry.arguments?.getString("name") ?: key
                    val format = backStackEntry.arguments?.getString("format") ?: ""
                    VirtualMembersScreen(
                        repoKey = key,
                        repoName = name,
                        repoFormat = format,
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
                        onCreateRepo = { navController.navigate("create-repo") },
                        onPackageClick = { id -> navController.navigate("packages/$id") },
                        onBuildClick = { id -> navController.navigate("builds/$id") },
                        accountActions = accountActions,
                    )
                }
                composable("integration") { IntegrationSection(isCompact = isCompact, accountActions = accountActions) }
                composable("security") { SecuritySection(isCompact = isCompact, accountActions = accountActions) }
                composable("operations") { OperationsSection(isCompact = isCompact, accountActions = accountActions) }
                composable("admin") { AdminSection(isCompact = isCompact, onDisconnect = onDisconnect, accountActions = accountActions) }
                composable("create-repo") {
                    CreateRepositoryScreen(
                        onBack = { navController.popBackStack() },
                        onCreated = { repoKey ->
                            navController.popBackStack()
                            navController.navigate("repos/$repoKey")
                        },
                    )
                }
                composable("repos/{key}") { backStackEntry ->
                    val key = backStackEntry.arguments?.getString("key") ?: return@composable
                    RepositoryDetailScreen(
                        repoKey = key,
                        onBack = { navController.popBackStack() },
                        onArtifactSecurityClick = { id, name ->
                            navController.navigate("artifacts/$id/security?name=$name")
                        },
                        onNavigateToMembers = { repoKey, repoName, repoFormat ->
                            navController.navigate("repos/$repoKey/members?name=$repoName&format=$repoFormat")
                        },
                    )
                }
                composable("packages/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable
                    PackageDetailScreen(
                        packageId = id,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("builds/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable
                    BuildDetailScreen(
                        buildId = id,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("artifacts/{id}/security?name={name}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable
                    val name = backStackEntry.arguments?.getString("name") ?: "Artifact"
                    SbomScreen(
                        artifactId = id,
                        artifactName = name,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("repos/{key}/members?name={name}&format={format}") { backStackEntry ->
                    val key = backStackEntry.arguments?.getString("key") ?: return@composable
                    val name = backStackEntry.arguments?.getString("name") ?: key
                    val format = backStackEntry.arguments?.getString("format") ?: ""
                    VirtualMembersScreen(
                        repoKey = key,
                        repoName = name,
                        repoFormat = format,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared TopAppBar logo
// ---------------------------------------------------------------------------

@Composable
private fun AppLogo() {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "Artifact Keeper",
        modifier = Modifier
            .padding(start = 8.dp)
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp)),
    )
}

// ---------------------------------------------------------------------------
// Section composables with adaptive sub-tab rows
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtifactsSection(
    isCompact: Boolean,
    onRepoClick: (String) -> Unit,
    onCreateRepo: () -> Unit,
    onPackageClick: (String) -> Unit,
    onBuildClick: (String) -> Unit,
    accountActions: @Composable () -> Unit,
) {
    val subTabs = if (isCompact) listOf("Repos", "Staging", "Pkgs", "Builds", "Search")
                  else listOf("Repositories", "Staging", "Packages", "Builds", "Search")
    var selectedTab by remember { mutableIntStateOf(0) }

    // Staging navigation state
    val stagingViewModel: StagingViewModel = hiltViewModel()
    val stagingUiState by stagingViewModel.uiState.collectAsState()
    var showStagingHistory by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Handle staging sub-navigation
        if (selectedTab == 1 && stagingUiState.selectedRepo != null) {
            if (showStagingHistory) {
                PromotionHistoryScreen(
                    viewModel = stagingViewModel,
                    onBack = { showStagingHistory = false },
                )
            } else {
                StagingDetailScreen(
                    viewModel = stagingViewModel,
                    onBack = {
                        stagingViewModel.clearSelectedRepo()
                    },
                    onShowHistory = { showStagingHistory = true },
                )
            }
        } else {
            TopAppBar(
                title = { Text("Artifacts") },
                navigationIcon = { AppLogo() },
                actions = { accountActions() },
            )
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = if (isCompact) 4.dp else 16.dp,
            ) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index != 1) {
                                // Reset staging state when navigating away
                                stagingViewModel.clearSelectedRepo()
                                showStagingHistory = false
                            }
                        },
                        text = { Text(title, maxLines = 1) },
                    )
                }
            }
            when (selectedTab) {
                0 -> RepositoriesScreen(onRepoClick = onRepoClick, onCreateRepo = onCreateRepo)
                1 -> StagingListScreen(
                    viewModel = stagingViewModel,
                    onRepoClick = { repo ->
                        stagingViewModel.selectRepo(repo)
                    },
                )
                2 -> PackagesScreen(onPackageClick = onPackageClick)
                3 -> BuildsScreen(onBuildClick = onBuildClick)
                4 -> SearchScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegrationSection(isCompact: Boolean, accountActions: @Composable () -> Unit) {
    val subTabs = listOf("Peers", "Replication", "Webhooks")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Integration") },
            navigationIcon = { AppLogo() },
            actions = { accountActions() },
        )
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
    val subTabs = listOf("Dashboard", "Scans", "Policies", "Licenses")
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedScanId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedScanId != null) {
            ScanFindingsScreen(
                scanId = selectedScanId!!,
                onBack = { selectedScanId = null },
            )
        } else {
            TopAppBar(
                title = { Text("Security") },
                navigationIcon = { AppLogo() },
                actions = { accountActions() },
            )
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
                3 -> LicensePoliciesScreen()
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
        TopAppBar(
            title = { Text("Operations") },
            navigationIcon = { AppLogo() },
            actions = { accountActions() },
        )
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
        TopAppBar(
            title = { Text("Admin") },
            navigationIcon = { AppLogo() },
            actions = { accountActions() },
        )
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
