package com.artifactkeeper.android

import com.artifactkeeper.android.data.api.ApiClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientTest {

    @After
    fun tearDown() {
        // Reset ApiClient state after each test since it is a singleton
        ApiClient.clearConfig()
    }

    // =========================================================================
    // Initial / cleared state
    // =========================================================================

    @Test
    fun `clearConfig resets to unconfigured state`() {
        ApiClient.configure("https://example.com", "tok123")
        ApiClient.clearConfig()

        assertFalse(ApiClient.isConfigured)
        assertEquals("", ApiClient.baseUrl)
        assertNull(ApiClient.token)
    }

    // =========================================================================
    // configure
    // =========================================================================

    @Test
    fun `configure sets baseUrl and token`() {
        ApiClient.configure("https://registry.example.com/", "my-token")

        assertTrue(ApiClient.isConfigured)
        assertEquals("https://registry.example.com/", ApiClient.baseUrl)
        assertEquals("my-token", ApiClient.token)
    }

    @Test
    fun `configure appends trailing slash if missing`() {
        ApiClient.configure("https://registry.example.com")

        assertEquals("https://registry.example.com/", ApiClient.baseUrl)
    }

    @Test
    fun `configure does not double trailing slash`() {
        ApiClient.configure("https://registry.example.com/")

        assertEquals("https://registry.example.com/", ApiClient.baseUrl)
    }

    @Test
    fun `configure with null token`() {
        ApiClient.configure("https://registry.example.com/", null)

        assertTrue(ApiClient.isConfigured)
        assertNull(ApiClient.token)
    }

    @Test
    fun `configure with default token parameter`() {
        ApiClient.configure("https://registry.example.com/")

        assertTrue(ApiClient.isConfigured)
        assertNull(ApiClient.token)
    }

    // =========================================================================
    // isConfigured
    // =========================================================================

    @Test
    fun `isConfigured returns false when baseUrl is blank`() {
        ApiClient.clearConfig()
        assertFalse(ApiClient.isConfigured)
    }

    @Test
    fun `isConfigured returns true when baseUrl is set`() {
        ApiClient.configure("http://localhost:8080")
        assertTrue(ApiClient.isConfigured)
    }

    // =========================================================================
    // setToken
    // =========================================================================

    @Test
    fun `setToken updates token`() {
        ApiClient.configure("https://example.com")
        assertNull(ApiClient.token)

        ApiClient.setToken("new-token")
        assertEquals("new-token", ApiClient.token)
    }

    @Test
    fun `setToken with null clears token`() {
        ApiClient.configure("https://example.com", "existing-token")
        ApiClient.setToken(null)
        assertNull(ApiClient.token)
    }

    // =========================================================================
    // reconfigure overwrites previous values
    // =========================================================================

    @Test
    fun `reconfigure overwrites previous baseUrl and token`() {
        ApiClient.configure("https://server1.example.com", "token1")
        assertEquals("https://server1.example.com/", ApiClient.baseUrl)
        assertEquals("token1", ApiClient.token)

        ApiClient.configure("https://server2.example.com", "token2")
        assertEquals("https://server2.example.com/", ApiClient.baseUrl)
        assertEquals("token2", ApiClient.token)
    }

    // =========================================================================
    // URL edge cases
    // =========================================================================

    @Test
    fun `configure with localhost URL`() {
        ApiClient.configure("http://localhost:8080")
        assertEquals("http://localhost:8080/", ApiClient.baseUrl)
    }

    @Test
    fun `configure with IP address URL`() {
        ApiClient.configure("http://192.168.1.100:8080")
        assertEquals("http://192.168.1.100:8080/", ApiClient.baseUrl)
    }

    @Test
    fun `configure with path in URL`() {
        ApiClient.configure("https://example.com/api/v1")
        assertEquals("https://example.com/api/v1/", ApiClient.baseUrl)
    }
}
