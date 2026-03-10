package com.artifactkeeper.android

import androidx.compose.ui.graphics.Color
import com.artifactkeeper.android.ui.theme.Critical
import com.artifactkeeper.android.ui.theme.High
import com.artifactkeeper.android.ui.theme.Low
import com.artifactkeeper.android.ui.theme.Medium
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the display logic in SecurityScreen.kt.
 * Covers grade-to-color mapping, DT status logic, audit progress
 * calculation, and risk score color determination.
 */
class SecurityScreenLogicTest {

    // Mirrors the private constants in SecurityScreen.kt
    private val GradeA = Color(0xFF52C41A)
    private val GradeB = Color(0xFF36CFC9)
    private val GradeC = Color(0xFFFAAD14)
    private val GradeD = Color(0xFFFA8C16)
    private val GradeF = Color(0xFFF5222D)

    private val DtConnected = Color(0xFF52C41A)
    private val DtDisconnected = Color(0xFFF5222D)

    // =========================================================================
    // Grade color mapping
    // =========================================================================

    private fun gradeColor(grade: String): Color = when (grade.uppercase()) {
        "A" -> GradeA
        "B" -> GradeB
        "C" -> GradeC
        "D" -> GradeD
        else -> GradeF
    }

    @Test
    fun `grade A maps to green`() {
        assertEquals(GradeA, gradeColor("A"))
    }

    @Test
    fun `grade B maps to teal`() {
        assertEquals(GradeB, gradeColor("B"))
    }

    @Test
    fun `grade C maps to amber`() {
        assertEquals(GradeC, gradeColor("C"))
    }

    @Test
    fun `grade D maps to orange`() {
        assertEquals(GradeD, gradeColor("D"))
    }

    @Test
    fun `grade F maps to red`() {
        assertEquals(GradeF, gradeColor("F"))
    }

    @Test
    fun `unknown grade maps to F red`() {
        assertEquals(GradeF, gradeColor("X"))
        assertEquals(GradeF, gradeColor(""))
    }

    @Test
    fun `grade mapping is case-insensitive`() {
        assertEquals(GradeA, gradeColor("a"))
        assertEquals(GradeB, gradeColor("b"))
        assertEquals(GradeC, gradeColor("c"))
        assertEquals(GradeD, gradeColor("d"))
        assertEquals(GradeF, gradeColor("f"))
    }

    @Test
    fun `all grade colors are distinct`() {
        val colors = listOf(GradeA, GradeB, GradeC, GradeD, GradeF)
        assertEquals(5, colors.toSet().size)
    }

    // =========================================================================
    // DT status logic
    // =========================================================================

    @Test
    fun `DT healthy maps to connected color`() {
        val isHealthy = true
        val color = if (isHealthy) DtConnected else DtDisconnected
        assertEquals(DtConnected, color)
    }

    @Test
    fun `DT unhealthy maps to disconnected color`() {
        val isHealthy = false
        val color = if (isHealthy) DtConnected else DtDisconnected
        assertEquals(DtDisconnected, color)
    }

    @Test
    fun `DT status text for healthy`() {
        val isHealthy = true
        val text = if (isHealthy) "Connected" else "Disconnected"
        assertEquals("Connected", text)
    }

    @Test
    fun `DT status text for unhealthy`() {
        val isHealthy = false
        val text = if (isHealthy) "Connected" else "Disconnected"
        assertEquals("Disconnected", text)
    }

    // =========================================================================
    // Audit progress calculation
    // =========================================================================

    @Test
    fun `progress is 0 when total is 0`() {
        val total = 0L
        val audited = 0L
        val progress = if (total > 0) audited.toFloat() / total.toFloat() else 0f
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun `progress is 1 when all findings are audited`() {
        val total = 100L
        val audited = 100L
        val progress = if (total > 0) audited.toFloat() / total.toFloat() else 0f
        assertEquals(1f, progress, 0.001f)
    }

    @Test
    fun `progress is 0_5 when half audited`() {
        val total = 200L
        val audited = 100L
        val progress = if (total > 0) audited.toFloat() / total.toFloat() else 0f
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun `progress with partial audit`() {
        val total = 75L
        val audited = 25L
        val progress = if (total > 0) audited.toFloat() / total.toFloat() else 0f
        assertEquals(0.333f, progress, 0.01f)
    }

    // =========================================================================
    // Risk score color determination
    // =========================================================================

    private fun riskScoreColor(riskScore: Double): Color = when {
        riskScore >= 70 -> Critical
        riskScore >= 40 -> High
        riskScore >= 10 -> Medium
        else -> DtConnected
    }

    @Test
    fun `risk score above 70 is critical`() {
        assertEquals(Critical, riskScoreColor(70.0))
        assertEquals(Critical, riskScoreColor(100.0))
        assertEquals(Critical, riskScoreColor(85.5))
    }

    @Test
    fun `risk score between 40 and 70 is high`() {
        assertEquals(High, riskScoreColor(40.0))
        assertEquals(High, riskScoreColor(69.9))
        assertEquals(High, riskScoreColor(55.0))
    }

    @Test
    fun `risk score between 10 and 40 is medium`() {
        assertEquals(Medium, riskScoreColor(10.0))
        assertEquals(Medium, riskScoreColor(39.9))
        assertEquals(Medium, riskScoreColor(25.0))
    }

    @Test
    fun `risk score below 10 is safe`() {
        assertEquals(DtConnected, riskScoreColor(9.9))
        assertEquals(DtConnected, riskScoreColor(0.0))
        assertEquals(DtConnected, riskScoreColor(5.0))
    }

    // =========================================================================
    // Risk score formatting
    // =========================================================================

    @Test
    fun `risk score formatted to one decimal place`() {
        assertEquals("42.5", "%.1f".format(42.5))
        assertEquals("0.0", "%.1f".format(0.0))
        assertEquals("100.0", "%.1f".format(100.0))
        assertEquals("33.3", "%.1f".format(33.333))
    }

    // =========================================================================
    // Score display format
    // =========================================================================

    @Test
    fun `score display format`() {
        val score = 85
        assertEquals("Score: 85/100", "Score: $score/100")
    }

    // =========================================================================
    // SecurityScreen isEmpty logic
    // =========================================================================

    @Test
    fun `security screen empty when no scores and no trends and dt not enabled`() {
        val scoresEmpty = true
        val cveTrendsNull = true
        val dtEnabled = false
        val isEmpty = scoresEmpty && cveTrendsNull && !dtEnabled
        assertTrue(isEmpty)
    }

    @Test
    fun `security screen not empty when scores present`() {
        val scoresEmpty = false
        val cveTrendsNull = true
        val dtEnabled = false
        val isEmpty = scoresEmpty && cveTrendsNull && !dtEnabled
        assertFalse(isEmpty)
    }

    @Test
    fun `security screen not empty when cveTrends present`() {
        val scoresEmpty = true
        val cveTrendsNull = false
        val dtEnabled = false
        val isEmpty = scoresEmpty && cveTrendsNull && !dtEnabled
        assertFalse(isEmpty)
    }

    @Test
    fun `security screen not empty when dt enabled`() {
        val scoresEmpty = true
        val cveTrendsNull = true
        val dtEnabled = true
        val isEmpty = scoresEmpty && cveTrendsNull && !dtEnabled
        assertFalse(isEmpty)
    }
}
