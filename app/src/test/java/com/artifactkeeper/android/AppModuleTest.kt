package com.artifactkeeper.android

import com.artifactkeeper.android.data.ServerManager
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.di.AppModule
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Tests for the Hilt AppModule provider methods.
 * These verify that providers return the expected singleton objects.
 */
class AppModuleTest {

    // =========================================================================
    // provideApiClient
    // =========================================================================

    @Test
    fun `provideApiClient returns the ApiClient singleton`() {
        val client = AppModule.provideApiClient()
        assertSame(ApiClient, client)
    }

    @Test
    fun `provideApiClient returns same instance on repeated calls`() {
        val first = AppModule.provideApiClient()
        val second = AppModule.provideApiClient()
        assertSame(first, second)
    }

    @Test
    fun `provideApiClient returns non-null`() {
        assertNotNull(AppModule.provideApiClient())
    }

    // =========================================================================
    // provideServerManager
    // =========================================================================

    @Test
    fun `provideServerManager returns the ServerManager singleton`() {
        val manager = AppModule.provideServerManager()
        assertSame(ServerManager, manager)
    }

    @Test
    fun `provideServerManager returns same instance on repeated calls`() {
        val first = AppModule.provideServerManager()
        val second = AppModule.provideServerManager()
        assertSame(first, second)
    }

    @Test
    fun `provideServerManager returns non-null`() {
        assertNotNull(AppModule.provideServerManager())
    }

    // =========================================================================
    // provideIoDispatcher
    // =========================================================================

    @Test
    fun `provideIoDispatcher returns Dispatchers IO`() {
        val dispatcher = AppModule.provideIoDispatcher()
        assertSame(Dispatchers.IO, dispatcher)
    }

    @Test
    fun `provideIoDispatcher returns non-null`() {
        assertNotNull(AppModule.provideIoDispatcher())
    }
}
