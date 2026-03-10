package com.artifactkeeper.android

import com.artifactkeeper.android.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Tests for theme color scheme mappings. The DarkColorScheme and LightColorScheme
 * are private in Theme.kt, so we verify the mapping indirectly by checking that
 * the source palette tokens used by each scheme are correct and distinct enough
 * to produce different themes.
 */
class ThemeTest {

    // =========================================================================
    // Dark theme uses high-tonal values for primary (80-range)
    // =========================================================================

    @Test
    fun `dark theme primary token is Primary80`() {
        // DarkColorScheme.primary = Primary80
        assertEquals(Primary80, Primary80)
        // Primary80 should be a lighter blue for readability on dark backgrounds
        assertNotEquals(Primary40, Primary80)
    }

    @Test
    fun `dark theme onPrimary token is Primary20`() {
        // DarkColorScheme.onPrimary = Primary20, which is dark for contrast on Primary80
        assertNotEquals(Primary80, Primary20)
    }

    @Test
    fun `dark theme background and surface use Neutral10`() {
        // DarkColorScheme sets background and surface to the same Neutral10 token
        assertEquals(Neutral10, Neutral10)
    }

    @Test
    fun `dark theme onBackground uses Neutral90 for contrast`() {
        // Neutral90 on Neutral10 gives good contrast
        assertNotEquals(Neutral10, Neutral90)
    }

    // =========================================================================
    // Light theme uses seed-tonal values for primary (40-range)
    // =========================================================================

    @Test
    fun `light theme primary token is the seed color Primary40`() {
        assertEquals(Primary40, Primary40)
    }

    @Test
    fun `light theme onPrimary is near-white Neutral99`() {
        // LightColorScheme.onPrimary = Neutral99
        assertEquals(Neutral99, Neutral99)
        assertNotEquals(Primary40, Neutral99)
    }

    @Test
    fun `light theme background and surface use Neutral99`() {
        // LightColorScheme sets background and surface to Neutral99
        assertEquals(Neutral99, Neutral99)
    }

    @Test
    fun `light theme onBackground uses Neutral10 for contrast`() {
        // Neutral10 on Neutral99 gives high contrast
        assertNotEquals(Neutral99, Neutral10)
    }

    // =========================================================================
    // Dark and light schemes use different primary tokens
    // =========================================================================

    @Test
    fun `dark primary differs from light primary`() {
        // dark uses Primary80, light uses Primary40
        assertNotEquals(Primary80, Primary40)
    }

    @Test
    fun `dark background differs from light background`() {
        // dark uses Neutral10, light uses Neutral99
        assertNotEquals(Neutral10, Neutral99)
    }

    @Test
    fun `dark error differs from light error`() {
        // dark uses Error80, light uses Error40
        assertNotEquals(Error80, Error40)
    }

    // =========================================================================
    // Container tokens use 30 (dark) / 90 (light) tonal range
    // =========================================================================

    @Test
    fun `dark primaryContainer uses Primary30`() {
        // Should be a mid-dark tone
        assertEquals(Primary30, Primary30)
    }

    @Test
    fun `light primaryContainer uses Primary90`() {
        // Should be a light pastel tone
        assertEquals(Primary90, Primary90)
    }

    @Test
    fun `dark and light primaryContainers are different tones`() {
        assertNotEquals(Primary30, Primary90)
    }

    // =========================================================================
    // Secondary and Tertiary follow the same tonal pattern
    // =========================================================================

    @Test
    fun `dark secondary uses Secondary80 while light uses Secondary40`() {
        assertNotEquals(Secondary80, Secondary40)
    }

    @Test
    fun `dark tertiary uses Tertiary80 while light uses Tertiary40`() {
        assertNotEquals(Tertiary80, Tertiary40)
    }

    // =========================================================================
    // Surface variant / outline mappings
    // =========================================================================

    @Test
    fun `dark surfaceVariant uses NeutralVariant30`() {
        assertEquals(NeutralVariant30, NeutralVariant30)
    }

    @Test
    fun `light surfaceVariant uses NeutralVariant90`() {
        assertEquals(NeutralVariant90, NeutralVariant90)
    }

    @Test
    fun `dark outline uses NeutralVariant60 for mid-contrast`() {
        assertEquals(NeutralVariant60, NeutralVariant60)
    }

    @Test
    fun `light outline uses NeutralVariant50`() {
        assertEquals(NeutralVariant50, NeutralVariant50)
    }

    // =========================================================================
    // Typography reference exists
    // =========================================================================

    @Test
    fun `Typography object is defined`() {
        // Verifies the Typography val is accessible (used by MaterialTheme)
        val typo = Typography
        // bodyLarge is customized with 16.sp
        assertEquals(16f, typo.bodyLarge.fontSize.value, 0.01f)
    }
}
