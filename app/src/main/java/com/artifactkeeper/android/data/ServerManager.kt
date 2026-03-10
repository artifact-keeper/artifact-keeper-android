package com.artifactkeeper.android.data

import android.content.Context
import android.content.SharedPreferences
import com.artifactkeeper.android.data.api.ApiClient
import com.artifactkeeper.android.data.models.SavedServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object ServerManager {
    private const val PREFS_KEY = "saved_servers"
    private const val ACTIVE_KEY = "active_server_id"

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var prefs: SharedPreferences

    private val _servers = MutableStateFlow<List<SavedServer>>(emptyList())
    val servers: StateFlow<List<SavedServer>> = _servers.asStateFlow()

    private val _activeServerId = MutableStateFlow<String?>(null)
    val activeServerId: StateFlow<String?> = _activeServerId.asStateFlow()

    private val _serverStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, Boolean>> = _serverStatuses.asStateFlow()

    suspend fun refreshStatuses() = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Boolean>()
        for (server in _servers.value) {
            results[server.id] = try {
                val url = URL("${server.url}health")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            } catch (_: Exception) {
                false
            }
        }
        _serverStatuses.value = results
    }

    fun init(context: Context) {
        prefs = EncryptedPrefsManager.getPrefs(context)
        migrateFromPlaintextServerPrefs(context)
        loadFromPrefs()
    }

    fun getServers(): List<SavedServer> = _servers.value

    fun getActiveServer(): SavedServer? {
        val activeId = _activeServerId.value ?: return null
        return _servers.value.find { it.id == activeId }
    }

    fun addServer(name: String, url: String): SavedServer {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        // Check if a server with same URL already exists
        val existing = _servers.value.find { it.url == cleanUrl }
        if (existing != null) {
            switchTo(existing.id)
            return existing
        }

        val server = SavedServer(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            url = cleanUrl,
            addedAt = System.currentTimeMillis(),
        )
        val updated = _servers.value + server
        _servers.value = updated
        _activeServerId.value = server.id
        saveToPrefs()
        return server
    }

    fun removeServer(id: String) {
        val updated = _servers.value.filter { it.id != id }
        _servers.value = updated
        if (_activeServerId.value == id) {
            _activeServerId.value = updated.firstOrNull()?.id
        }
        saveToPrefs()
    }

    fun switchTo(id: String) {
        val server = _servers.value.find { it.id == id } ?: return
        _activeServerId.value = id
        saveToPrefs()
        ApiClient.configure(server.url, null)
    }

    fun migrateIfNeeded(context: Context) {
        // Check encrypted prefs for a legacy single-server URL that needs
        // converting into the multi-server list.
        val legacyUrl = prefs.getString(EncryptedPrefsManager.KEY_SERVER_URL, null)
        if (!legacyUrl.isNullOrBlank() && _servers.value.isEmpty()) {
            val host = try {
                java.net.URI(legacyUrl).host ?: legacyUrl
            } catch (_: Exception) {
                legacyUrl
            }
            addServer(name = host, url = legacyUrl)
        }
    }

    /**
     * One-time migration: if the old plaintext "artifact_keeper_prefs" file
     * contains saved_servers or active_server_id, copy them to encrypted
     * storage and remove the plaintext entries.
     */
    private fun migrateFromPlaintextServerPrefs(context: Context) {
        val legacy = context.getSharedPreferences("artifact_keeper_prefs", Context.MODE_PRIVATE)
        val hasLegacy = legacy.contains(PREFS_KEY) || legacy.contains(ACTIVE_KEY)
        if (!hasLegacy) return

        val editor = prefs.edit()
        val legacyEditor = legacy.edit()

        val serversJson = legacy.getString(PREFS_KEY, null)
        if (serversJson != null && !prefs.contains(PREFS_KEY)) {
            editor.putString(PREFS_KEY, serversJson)
        }
        val activeId = legacy.getString(ACTIVE_KEY, null)
        if (activeId != null && !prefs.contains(ACTIVE_KEY)) {
            editor.putString(ACTIVE_KEY, activeId)
        }

        legacyEditor.remove(PREFS_KEY).remove(ACTIVE_KEY)
        editor.apply()
        legacyEditor.apply()
    }

    private fun loadFromPrefs() {
        val serversJson = prefs.getString(PREFS_KEY, null)
        if (serversJson != null) {
            try {
                _servers.value = json.decodeFromString<List<SavedServer>>(serversJson)
            } catch (_: Exception) {
                _servers.value = emptyList()
            }
        }
        _activeServerId.value = prefs.getString(ACTIVE_KEY, null)
    }

    private fun saveToPrefs() {
        prefs.edit()
            .putString(PREFS_KEY, json.encodeToString(_servers.value))
            .putString(ACTIVE_KEY, _activeServerId.value)
            .apply()
    }
}
