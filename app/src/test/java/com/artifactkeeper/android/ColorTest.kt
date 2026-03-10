package com.artifactkeeper.android

import androidx.compose.ui.graphics.Color
import com.artifactkeeper.android.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ColorTest {

    // =========================================================================
    // Primary tonal range
    // =========================================================================

    @Test
    fun `Primary40 is the brand seed color`() {
        assertEquals(Color(0xFF3B55C6), Primary40)
    }

    @Test
    fun `Primary tonal values are ordered from dark to light`() {
        // Lower tonal values should have lower luminance (darker).
        // We verify the hex values match the documented palette.
        assertEquals(Color(0xFF001849), Primary10)
        assertEquals(Color(0xFF0C2C7A), Primary20)
        assertEquals(Color(0xFF2643A0), Primary30)
        assertEquals(Color(0xFF3B55C6), Primary40)
        assertEquals(Color(0xFF5570E0), Primary50)
        assertEquals(Color(0xFF7089F3), Primary60)
        assertEquals(Color(0xFF8DA3FF), Primary70)
        assertEquals(Color(0xFFB1C0FF), Primary80)
        assertEquals(Color(0xFFD9E0FF), Primary90)
        assertEquals(Color(0xFFEEF0FF), Primary95)
        assertEquals(Color(0xFFFBFBFF), Primary99)
    }

    @Test
    fun `all Primary tonal values are distinct`() {
        val primaries = listOf(
            Primary10, Primary20, Primary30, Primary40, Primary50,
            Primary60, Primary70, Primary80, Primary90, Primary95, Primary99,
        )
        assertEquals(primaries.size, primaries.toSet().size)
    }

    // =========================================================================
    // Secondary tonal range
    // =========================================================================

    @Test
    fun `Secondary tonal values match palette`() {
        assertEquals(Color(0xFF141B2C), Secondary10)
        assertEquals(Color(0xFF293042), Secondary20)
        assertEquals(Color(0xFF3F4759), Secondary30)
        assertEquals(Color(0xFF575E71), Secondary40)
        assertEquals(Color(0xFF70778B), Secondary50)
        assertEquals(Color(0xFF8991A5), Secondary60)
        assertEquals(Color(0xFFA4ABC0), Secondary70)
        assertEquals(Color(0xFFBFC6DC), Secondary80)
        assertEquals(Color(0xFFDBE2F9), Secondary90)
        assertEquals(Color(0xFFEEF0FF), Secondary95)
    }

    @Test
    fun `all Secondary tonal values are distinct`() {
        val secondaries = listOf(
            Secondary10, Secondary20, Secondary30, Secondary40, Secondary50,
            Secondary60, Secondary70, Secondary80, Secondary90, Secondary95,
        )
        assertEquals(secondaries.size, secondaries.toSet().size)
    }

    // =========================================================================
    // Tertiary tonal range
    // =========================================================================

    @Test
    fun `Tertiary tonal values match palette`() {
        assertEquals(Color(0xFF241530), Tertiary10)
        assertEquals(Color(0xFF3A2A46), Tertiary20)
        assertEquals(Color(0xFF52415E), Tertiary30)
        assertEquals(Color(0xFF6A5877), Tertiary40)
        assertEquals(Color(0xFF847091), Tertiary50)
        assertEquals(Color(0xFF9E8AAC), Tertiary60)
        assertEquals(Color(0xFFBAA4C7), Tertiary70)
        assertEquals(Color(0xFFD6BFE4), Tertiary80)
        assertEquals(Color(0xFFF2DAFF), Tertiary90)
        assertEquals(Color(0xFFFBECFF), Tertiary95)
    }

    // =========================================================================
    // Error tonal range
    // =========================================================================

    @Test
    fun `Error tonal values match palette`() {
        assertEquals(Color(0xFF410002), Error10)
        assertEquals(Color(0xFF690005), Error20)
        assertEquals(Color(0xFF93000A), Error30)
        assertEquals(Color(0xFFBA1A1A), Error40)
        assertEquals(Color(0xFFFF5449), Error60)
        assertEquals(Color(0xFFFFB4AB), Error80)
        assertEquals(Color(0xFFFFDAD6), Error90)
    }

    // =========================================================================
    // Neutral (surface) values
    // =========================================================================

    @Test
    fun `Neutral surface values match palette`() {
        assertEquals(Color(0xFF1B1B1F), Neutral10)
        assertEquals(Color(0xFF303034), Neutral20)
        assertEquals(Color(0xFFE3E1E6), Neutral90)
        assertEquals(Color(0xFFF2EFF4), Neutral95)
        assertEquals(Color(0xFFFDFBFF), Neutral99)
    }

    // =========================================================================
    // Neutral Variant (outlines / surface-variant)
    // =========================================================================

    @Test
    fun `NeutralVariant values match palette`() {
        assertEquals(Color(0xFF44464E), NeutralVariant30)
        assertEquals(Color(0xFF757780), NeutralVariant50)
        assertEquals(Color(0xFF8F919A), NeutralVariant60)
        assertEquals(Color(0xFFC6C6D0), NeutralVariant80)
        assertEquals(Color(0xFFE2E1EC), NeutralVariant90)
    }

    // =========================================================================
    // Semantic severity colors
    // =========================================================================

    @Test
    fun `severity colors have correct values`() {
        assertEquals(Color(0xFFF5222D), Critical)
        assertEquals(Color(0xFFFA8C16), High)
        assertEquals(Color(0xFFFAAD14), Medium)
        assertEquals(Color(0xFF1890FF), Low)
    }

    @Test
    fun `all severity colors are distinct`() {
        val severities = listOf(Critical, High, Medium, Low)
        assertEquals(severities.size, severities.toSet().size)
    }

    // =========================================================================
    // Semantic policy status colors
    // =========================================================================

    @Test
    fun `policy status colors have correct values`() {
        assertEquals(Color(0xFF52C41A), PolicyPassing)
        assertEquals(Color(0xFFF5222D), PolicyFailing)
        assertEquals(Color(0xFFFAAD14), PolicyWarning)
        assertEquals(Color(0xFF8C8C8C), PolicyPending)
    }

    @Test
    fun `all policy status colors are distinct`() {
        val statuses = listOf(PolicyPassing, PolicyFailing, PolicyWarning, PolicyPending)
        assertEquals(statuses.size, statuses.toSet().size)
    }

    @Test
    fun `PolicyFailing matches Critical since both represent the same severity`() {
        assertEquals(Critical, PolicyFailing)
    }

    @Test
    fun `PolicyWarning matches Medium since both are amber`() {
        assertEquals(Medium, PolicyWarning)
    }

    // =========================================================================
    // Color channel sanity checks
    // =========================================================================

    @Test
    fun `Primary10 is very dark with low RGB values`() {
        // Primary10 = 0xFF001849
        assertEquals(0f, Primary10.red, 0.01f)
        assertEquals(0x18 / 255f, Primary10.green, 0.01f)
        assertEquals(0x49 / 255f, Primary10.blue, 0.01f)
        assertEquals(1f, Primary10.alpha, 0f)
    }

    @Test
    fun `Primary99 is nearly white`() {
        // Primary99 = 0xFFFBFBFF
        assertEquals(0xFB / 255f, Primary99.red, 0.01f)
        assertEquals(0xFB / 255f, Primary99.green, 0.01f)
        assertEquals(1f, Primary99.blue, 0.01f)
        assertEquals(1f, Primary99.alpha, 0f)
    }

    @Test
    fun `all palette colors are fully opaque`() {
        val allColors = listOf(
            Primary10, Primary20, Primary30, Primary40, Primary50,
            Primary60, Primary70, Primary80, Primary90, Primary95, Primary99,
            Secondary10, Secondary20, Secondary30, Secondary40, Secondary50,
            Secondary60, Secondary70, Secondary80, Secondary90, Secondary95,
            Tertiary10, Tertiary20, Tertiary30, Tertiary40, Tertiary50,
            Tertiary60, Tertiary70, Tertiary80, Tertiary90, Tertiary95,
            Error10, Error20, Error30, Error40, Error60, Error80, Error90,
            Neutral10, Neutral20, Neutral90, Neutral95, Neutral99,
            NeutralVariant30, NeutralVariant50, NeutralVariant60, NeutralVariant80, NeutralVariant90,
            Critical, High, Medium, Low,
            PolicyPassing, PolicyFailing, PolicyWarning, PolicyPending,
        )
        for (color in allColors) {
            assertEquals("Color $color should be fully opaque", 1f, color.alpha, 0f)
        }
    }
}
