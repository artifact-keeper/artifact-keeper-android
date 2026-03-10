package com.artifactkeeper.android

import android.content.Context
import android.content.SharedPreferences
import com.artifactkeeper.android.data.EncryptedPrefsManager
import com.artifactkeeper.android.data.ServerManager
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.SavedServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the real ServerManager methods by injecting a mock SharedPreferences
 * via [ServerManager.initForTesting].
 */
class ServerManagerTest {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        mockEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { putString(any(), any()) } returns this
            every { remove(any()) } returns this
            every { clear() } returns this
            every { apply() } returns Unit
        }
        mockPrefs = mockk {
            every { getString("saved_servers", null) } returns null
            every { getString("active_server_id", null) } returns null
            every { edit() } returns mockEditor
        }

        // Mock ApiClient.configure since switchTo calls it
        mockkObject(ApiClient)
        every { ApiClient.configure(any(), any()) } returns Unit

        ServerManager.initForTesting(mockPrefs)
    }

    @After
    fun tearDown() {
        unmockkObject(ApiClient)
    }

    // =========================================================================
    // initForTesting / loadFromPrefs
    // =========================================================================

    @Test
    fun `initForTesting resets all state`() {
        assertTrue(ServerManager.getServers().isEmpty())
        assertNull(ServerManager.activeServerId.value)
    }

    @Test
    fun `loadFromPrefs loads servers from prefs JSON`() {
        val servers = listOf(
            SavedServer(id = "s1", name = "Dev", url = "https://dev.example.com/", addedAt = 1000),
            SavedServer(id = "s2", name = "Prod", url = "https://prod.example.com/", addedAt = 2000),
        )
        val serversJson = json.encodeToString(servers)

        every { mockPrefs.getString("saved_servers", null) } returns serversJson
        every { mockPrefs.getString("active_server_id", null) } returns "s2"

        ServerManager.loadFromPrefs()

        assertEquals(2, ServerManager.getServers().size)
        assertEquals("Dev", ServerManager.getServers()[0].name)
        assertEquals("Prod", ServerManager.getServers()[1].name)
        assertEquals("s2", ServerManager.activeServerId.value)
    }

    @Test
    fun `loadFromPrefs handles malformed JSON gracefully`() {
        every { mockPrefs.getString("saved_servers", null) } returns "not valid json"
        every { mockPrefs.getString("active_server_id", null) } returns null

        ServerManager.loadFromPrefs()

        assertTrue(ServerManager.getServers().isEmpty())
    }

    @Test
    fun `loadFromPrefs handles null servers JSON`() {
        every { mockPrefs.getString("saved_servers", null) } returns null
        every { mockPrefs.getString("active_server_id", null) } returns null

        ServerManager.loadFromPrefs()

        assertTrue(ServerManager.getServers().isEmpty())
        assertNull(ServerManager.activeServerId.value)
    }

    // =========================================================================
    // addServer
    // =========================================================================

    @Test
    fun `addServer creates new server and saves to prefs`() {
        val server = ServerManager.addServer("My Server", "https://example.com")

        assertEquals("https://example.com/", server.url)
        assertEquals("My Server", server.name)
        assertNotNull(server.id)
        assertEquals(1, ServerManager.getServers().size)
        assertEquals(server.id, ServerManager.activeServerId.value)
        verify { mockEditor.putString("saved_servers", any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `addServer appends trailing slash when missing`() {
        val server = ServerManager.addServer("Test", "https://test.example.com")
        assertEquals("https://test.example.com/", server.url)
    }

    @Test
    fun `addServer keeps trailing slash when present`() {
        val server = ServerManager.addServer("Test", "https://test.example.com/")
        assertEquals("https://test.example.com/", server.url)
    }

    @Test
    fun `addServer returns existing server for duplicate URL`() {
        val first = ServerManager.addServer("First", "https://dup.example.com")
        val second = ServerManager.addServer("Second", "https://dup.example.com")

        assertEquals(first.id, second.id)
        assertEquals(1, ServerManager.getServers().size)
    }

    @Test
    fun `addServer detects duplicate after URL cleaning`() {
        val first = ServerManager.addServer("First", "https://dup.example.com/")
        val second = ServerManager.addServer("Second", "https://dup.example.com")

        assertEquals(first.id, second.id)
    }

    // =========================================================================
    // removeServer
    // =========================================================================

    @Test
    fun `removeServer removes server from list`() {
        val server = ServerManager.addServer("Temp", "https://temp.example.com")
        assertEquals(1, ServerManager.getServers().size)

        ServerManager.removeServer(server.id)
        assertTrue(ServerManager.getServers().isEmpty())
    }

    @Test
    fun `removeServer falls back to first remaining when active is removed`() {
        val s1 = ServerManager.addServer("S1", "https://s1.example.com")
        val s2 = ServerManager.addServer("S2", "https://s2.example.com")

        // s2 is active because it was added last
        assertEquals(s2.id, ServerManager.activeServerId.value)

        ServerManager.removeServer(s2.id)
        assertEquals(s1.id, ServerManager.activeServerId.value)
    }

    @Test
    fun `removeServer sets null active when last server removed`() {
        val server = ServerManager.addServer("Only", "https://only.example.com")
        ServerManager.removeServer(server.id)
        assertNull(ServerManager.activeServerId.value)
    }

    @Test
    fun `removeServer does not change active when non-active is removed`() {
        val s1 = ServerManager.addServer("S1", "https://s1.example.com")
        val s2 = ServerManager.addServer("S2", "https://s2.example.com")

        // s2 is active
        ServerManager.removeServer(s1.id)
        assertEquals(s2.id, ServerManager.activeServerId.value)
        assertEquals(1, ServerManager.getServers().size)
    }

    // =========================================================================
    // switchTo
    // =========================================================================

    @Test
    fun `switchTo changes active server and configures ApiClient`() {
        val s1 = ServerManager.addServer("S1", "https://s1.example.com")
        val s2 = ServerManager.addServer("S2", "https://s2.example.com")

        ServerManager.switchTo(s1.id)

        assertEquals(s1.id, ServerManager.activeServerId.value)
        verify { ApiClient.configure("https://s1.example.com/", null) }
    }

    @Test
    fun `switchTo does nothing for unknown id`() {
        ServerManager.addServer("S1", "https://s1.example.com")
        val originalActive = ServerManager.activeServerId.value

        ServerManager.switchTo("nonexistent-id")

        assertEquals(originalActive, ServerManager.activeServerId.value)
    }

    // =========================================================================
    // getActiveServer
    // =========================================================================

    @Test
    fun `getActiveServer returns correct server`() {
        val server = ServerManager.addServer("Active", "https://active.example.com")
        val active = ServerManager.getActiveServer()

        assertNotNull(active)
        assertEquals(server.id, active!!.id)
        assertEquals("Active", active.name)
    }

    @Test
    fun `getActiveServer returns null when no active id`() {
        // No servers added, no active
        assertNull(ServerManager.getActiveServer())
    }

    // =========================================================================
    // getServers
    // =========================================================================

    @Test
    fun `getServers returns empty list initially`() {
        assertTrue(ServerManager.getServers().isEmpty())
    }

    @Test
    fun `getServers returns all added servers`() {
        ServerManager.addServer("A", "https://a.com")
        ServerManager.addServer("B", "https://b.com")
        ServerManager.addServer("C", "https://c.com")

        assertEquals(3, ServerManager.getServers().size)
    }

    // =========================================================================
    // migrateIfNeeded
    // =========================================================================

    @Test
    fun `migrateIfNeeded creates server from legacy URL when list is empty`() {
        every { mockPrefs.getString("server_url", null) } returns "https://legacy.example.com/"

        val context = mockk<Context>()
        ServerManager.migrateIfNeeded(context)

        assertEquals(1, ServerManager.getServers().size)
        assertEquals("https://legacy.example.com/", ServerManager.getServers()[0].url)
        assertEquals("legacy.example.com", ServerManager.getServers()[0].name)
    }

    @Test
    fun `migrateIfNeeded skips when legacy URL is null`() {
        every { mockPrefs.getString("server_url", null) } returns null

        val context = mockk<Context>()
        ServerManager.migrateIfNeeded(context)

        assertTrue(ServerManager.getServers().isEmpty())
    }

    @Test
    fun `migrateIfNeeded skips when legacy URL is blank`() {
        every { mockPrefs.getString("server_url", null) } returns "   "

        val context = mockk<Context>()
        ServerManager.migrateIfNeeded(context)

        assertTrue(ServerManager.getServers().isEmpty())
    }

    @Test
    fun `migrateIfNeeded skips when servers already exist`() {
        ServerManager.addServer("Existing", "https://existing.example.com")
        every { mockPrefs.getString("server_url", null) } returns "https://legacy.example.com/"

        val context = mockk<Context>()
        ServerManager.migrateIfNeeded(context)

        assertEquals(1, ServerManager.getServers().size)
        assertEquals("https://existing.example.com/", ServerManager.getServers()[0].url)
    }

    @Test
    fun `migrateIfNeeded extracts host from URL for server name`() {
        every { mockPrefs.getString("server_url", null) } returns "https://my-registry.internal.io:8080/api/"

        val context = mockk<Context>()
        ServerManager.migrateIfNeeded(context)

        assertEquals("my-registry.internal.io", ServerManager.getServers()[0].name)
    }

    @Test
    fun `migrateIfNeeded uses full URL as name for malformed URIs`() {
        every { mockPrefs.getString("server_url", null) } returns "not a valid uri %%"

        val context = mockk<Context>()
        ServerManager.migrateIfNeeded(context)

        assertEquals(1, ServerManager.getServers().size)
        assertEquals("not a valid uri %%", ServerManager.getServers()[0].name)
    }

    // =========================================================================
    // migrateFromPlaintextServerPrefs
    // =========================================================================

    @Test
    fun `migrateFromPlaintextServerPrefs copies legacy data to encrypted prefs`() {
        val legacyEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { remove(any()) } returns this
            every { apply() } returns Unit
        }
        val legacyPrefs = mockk<SharedPreferences> {
            every { contains("saved_servers") } returns true
            every { contains("active_server_id") } returns true
            every { getString("saved_servers", null) } returns """[{"id":"x","name":"X","url":"https://x.com/","addedAt":0}]"""
            every { getString("active_server_id", null) } returns "x"
            every { edit() } returns legacyEditor
        }

        every { mockPrefs.contains("saved_servers") } returns false
        every { mockPrefs.contains("active_server_id") } returns false

        val context = mockk<Context> {
            every { getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE) } returns legacyPrefs
        }

        ServerManager.migrateFromPlaintextServerPrefs(context)

        verify { mockEditor.putString("saved_servers", """[{"id":"x","name":"X","url":"https://x.com/","addedAt":0}]""") }
        verify { mockEditor.putString("active_server_id", "x") }
        verify { legacyEditor.remove("saved_servers") }
        verify { legacyEditor.remove("active_server_id") }
        verify { mockEditor.apply() }
        verify { legacyEditor.apply() }
    }

    @Test
    fun `migrateFromPlaintextServerPrefs skips when no legacy data`() {
        val legacyPrefs = mockk<SharedPreferences> {
            every { contains("saved_servers") } returns false
            every { contains("active_server_id") } returns false
        }

        val context = mockk<Context> {
            every { getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE) } returns legacyPrefs
        }

        ServerManager.migrateFromPlaintextServerPrefs(context)

        // Should not call edit() on the main prefs
        verify(exactly = 0) { mockPrefs.edit() }
    }

    @Test
    fun `migrateFromPlaintextServerPrefs skips key when encrypted already has it`() {
        val legacyEditor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { remove(any()) } returns this
            every { apply() } returns Unit
        }
        val legacyPrefs = mockk<SharedPreferences> {
            every { contains("saved_servers") } returns true
            every { contains("active_server_id") } returns false
            every { getString("saved_servers", null) } returns "[]"
            every { getString("active_server_id", null) } returns null
            every { edit() } returns legacyEditor
        }

        every { mockPrefs.contains("saved_servers") } returns true
        every { mockPrefs.contains("active_server_id") } returns false

        val context = mockk<Context> {
            every { getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE) } returns legacyPrefs
        }

        ServerManager.migrateFromPlaintextServerPrefs(context)

        // Should NOT copy saved_servers since encrypted already has it
        verify(exactly = 0) { mockEditor.putString("saved_servers", any()) }
    }

    // =========================================================================
    // saveToPrefs
    // =========================================================================

    @Test
    fun `saveToPrefs serializes servers and active id to prefs`() {
        ServerManager.addServer("Test", "https://test.example.com")

        // addServer already calls saveToPrefs; verify the serialization
        verify(atLeast = 1) { mockEditor.putString(eq("saved_servers"), any()) }
        verify(atLeast = 1) { mockEditor.putString(eq("active_server_id"), any()) }
        verify(atLeast = 1) { mockEditor.apply() }
    }
}
