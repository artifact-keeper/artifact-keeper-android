package com.artifactkeeper.android.ui.util

import androidx.compose.ui.graphics.Color
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Medium

/**
 * Extracted display logic from various Composable screens.
 * By placing this logic in plain (non-Composable) functions, JaCoCo
 * can instrument and measure coverage for it via unit tests.
 */
object DisplayLogic {

    // =========================================================================
    // SecurityScreen: grade color mapping
    // =========================================================================

    private val GradeA = Color(0xFF52C41A)
    private val GradeB = Color(0xFF36CFC9)
    private val GradeC = Color(0xFFFAAD14)
    private val GradeD = Color(0xFFFA8C16)
    private val GradeF = Color(0xFFF5222D)

    fun gradeColor(grade: String): Color = when (grade.uppercase()) {
        "A" -> GradeA
        "B" -> GradeB
        "C" -> GradeC
        "D" -> GradeD
        else -> GradeF
    }

    // =========================================================================
    // SecurityScreen: DT status
    // =========================================================================

    private val DtConnected = Color(0xFF52C41A)
    private val DtDisconnected = Color(0xFFF5222D)

    fun dtStatusColor(isHealthy: Boolean): Color =
        if (isHealthy) DtConnected else DtDisconnected

    fun dtStatusText(isHealthy: Boolean): String =
        if (isHealthy) "Connected" else "Disconnected"

    // =========================================================================
    // SecurityScreen: audit progress
    // =========================================================================

    fun auditProgress(total: Long, audited: Long): Float =
        if (total > 0) audited.toFloat() / total.toFloat() else 0f

    // =========================================================================
    // SecurityScreen: risk score color
    // =========================================================================

    fun riskScoreColor(riskScore: Double): Color = when {
        riskScore >= 70 -> Critical
        riskScore >= 40 -> High
        riskScore >= 10 -> Medium
        else -> DtConnected
    }

    // =========================================================================
    // SecurityScreen: isEmpty logic
    // =========================================================================

    fun securityScreenIsEmpty(
        scoresEmpty: Boolean,
        cveTrendsNull: Boolean,
        dtEnabled: Boolean,
    ): Boolean = scoresEmpty && cveTrendsNull && !dtEnabled

    // =========================================================================
    // BuildsScreen: status color mapping
    // =========================================================================

    private val StatusSuccess = Color(0xFF52C41A)
    private val StatusFailed = Color(0xFFF5222D)
    private val StatusRunning = Color(0xFF1890FF)
    private val StatusPending = Color(0xFF8C8C8C)

    fun buildStatusColor(status: String): Color = when (status.lowercase()) {
        "success" -> StatusSuccess
        "failed", "error" -> StatusFailed
        "running", "in_progress" -> StatusRunning
        else -> StatusPending
    }

    // =========================================================================
    // BuildsScreen: title formatting
    // =========================================================================

    fun buildTitle(name: String, number: Long): String = "$name #$number"

    // =========================================================================
    // BuildsScreen: artifact count text
    // =========================================================================

    fun buildArtifactCountText(count: Int): String =
        "$count artifact${if (count > 1) "s" else ""}"

    // =========================================================================
    // MonitoringScreen: service health check
    // =========================================================================

    private val MonitoringOk = Color(0xFF52C41A)
    private val MonitoringFail = Color(0xFFF5222D)

    fun isServiceHealthy(status: String): Boolean =
        status.lowercase() == "ok" || status.lowercase() == "healthy"

    fun serviceStatusColor(status: String): Color =
        if (isServiceHealthy(status)) MonitoringOk else MonitoringFail

    // =========================================================================
    // MonitoringScreen: alert status check
    // =========================================================================

    fun isAlertHealthy(currentStatus: String): Boolean =
        currentStatus.lowercase() == "healthy"

    // =========================================================================
    // StagingListScreen: artifact count text (different pluralization)
    // =========================================================================

    fun stagingArtifactCountText(count: Int): String =
        "$count artifact${if (count != 1) "s" else ""}"

    // =========================================================================
    // Shared: description display check
    // =========================================================================

    fun shouldShowDescription(description: String?): Boolean =
        !description.isNullOrBlank()

    // =========================================================================
    // ServerManager: URL cleaning
    // =========================================================================

    fun cleanServerUrl(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    // =========================================================================
    // Shared: host extraction from URL
    // =========================================================================

    fun extractHost(url: String): String = try {
        java.net.URI(url).host ?: url
    } catch (_: Exception) {
        url
    }

    // =========================================================================
    // Shared: validation helpers
    // =========================================================================

    fun isLoginValid(username: String, password: String): Boolean =
        username.isNotBlank() && password.isNotBlank()

    // =========================================================================
    // NavHost: visible tabs logic
    // =========================================================================

    fun visibleTabRoutes(
        allRoutes: List<String>,
        isLoggedIn: Boolean,
        isAdmin: Boolean,
    ): List<String> = allRoutes.filter { route ->
        when (route) {
            "artifacts" -> true
            "admin" -> isAdmin
            else -> isLoggedIn
        }
    }

    // =========================================================================
    // SearchScreen: display branch
    // =========================================================================

    fun searchDisplayBranch(
        isSearching: Boolean,
        errorMessage: String?,
        hasSearched: Boolean,
        repoResultsEmpty: Boolean,
        artifactResultsEmpty: Boolean,
    ): String = when {
        isSearching -> "loading"
        errorMessage != null -> "error"
        !hasSearched -> "initial"
        repoResultsEmpty && artifactResultsEmpty -> "noResults"
        else -> "results"
    }

    // =========================================================================
    // SharedComposables: LoadingErrorContainer branch
    // =========================================================================

    fun containerBranch(
        isLoading: Boolean,
        error: String?,
        isEmpty: Boolean,
        hasCustomContent: Boolean,
    ): String = when {
        isLoading -> "loading"
        error != null -> "error"
        isEmpty -> if (hasCustomContent) "emptyCustom" else "emptyDefault"
        else -> "content"
    }
}
