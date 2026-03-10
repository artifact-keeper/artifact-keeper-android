package com.artifactkeeper.android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Centralized manager for encrypted storage of sensitive data (auth tokens,
 * server credentials, user info). Non-sensitive UI preferences should continue
 * to use regular SharedPreferences.
 *
 * On first access after migration, any plaintext values stored under the old
 * "artifact_keeper_prefs" file are copied into encrypted storage and then
 * removed from the plaintext file.
 */
object EncryptedPrefsManager {

    private const val TAG = "EncryptedPrefsManager"
    private const val ENCRYPTED_PREFS_NAME = "artifact_keeper_secure_prefs"
    private const val LEGACY_PREFS_NAME = "artifact_keeper_prefs"

    // Keys for sensitive data
    const val KEY_SERVER_URL = "server_url"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_USERNAME = "user_username"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_IS_ADMIN = "user_is_admin"

    private val SENSITIVE_STRING_KEYS = listOf(
        KEY_SERVER_URL,
        KEY_AUTH_TOKEN,
        KEY_USER_ID,
        KEY_USER_USERNAME,
        KEY_USER_EMAIL,
    )
    private val SENSITIVE_BOOLEAN_KEYS = listOf(
        KEY_USER_IS_ADMIN,
    )

    @Volatile
    private var encryptedPrefs: SharedPreferences? = null

    /**
     * Returns the encrypted SharedPreferences instance, creating it on first
     * call. Thread-safe via double-checked locking.
     */
    fun getPrefs(context: Context): SharedPreferences {
        encryptedPrefs?.let { return it }
        synchronized(this) {
            encryptedPrefs?.let { return it }
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            migrateFromPlaintext(context, prefs)
            encryptedPrefs = prefs
            return prefs
        }
    }

    // ----- convenience read helpers -----

    fun getString(context: Context, key: String): String? =
        getPrefs(context).getString(key, null)

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean =
        getPrefs(context).getBoolean(key, default)

    // ----- convenience write helpers -----

    fun putString(context: Context, key: String, value: String?) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun remove(context: Context, vararg keys: String) {
        val editor = getPrefs(context).edit()
        for (key in keys) {
            editor.remove(key)
        }
        editor.apply()
    }

    /** Clears all auth-related keys (token + user info) but keeps server_url. */
    fun clearAuthData(context: Context) {
        remove(
            context,
            KEY_AUTH_TOKEN,
            KEY_USER_ID,
            KEY_USER_USERNAME,
            KEY_USER_EMAIL,
            KEY_USER_IS_ADMIN,
        )
    }

    /** Clears everything (auth + server). Used on full disconnect. */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    // ----- batch write for login -----

    fun saveLoginData(
        context: Context,
        token: String,
        userId: String,
        username: String,
        email: String,
        isAdmin: Boolean,
    ) {
        getPrefs(context).edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_USERNAME, username)
            .putString(KEY_USER_EMAIL, email)
            .putBoolean(KEY_USER_IS_ADMIN, isAdmin)
            .apply()
    }

    // ----- migration from plaintext prefs -----

    private fun migrateFromPlaintext(context: Context, encPrefs: SharedPreferences) {
        val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

        // Check whether there is anything to migrate. If the legacy file has
        // none of the sensitive keys we care about, skip entirely.
        val hasLegacyData = SENSITIVE_STRING_KEYS.any { legacy.contains(it) } ||
            SENSITIVE_BOOLEAN_KEYS.any { legacy.contains(it) }
        if (!hasLegacyData) return

        Log.i(TAG, "Migrating sensitive data from plaintext to encrypted storage")

        val editor = encPrefs.edit()
        val legacyEditor = legacy.edit()

        for (key in SENSITIVE_STRING_KEYS) {
            val value = legacy.getString(key, null)
            if (value != null) {
                // Only copy if the encrypted store doesn't already have a value,
                // so we don't overwrite data that was written directly to the
                // encrypted store after it was created.
                if (!encPrefs.contains(key)) {
                    editor.putString(key, value)
                }
                legacyEditor.remove(key)
            }
        }
        for (key in SENSITIVE_BOOLEAN_KEYS) {
            if (legacy.contains(key)) {
                if (!encPrefs.contains(key)) {
                    editor.putBoolean(key, legacy.getBoolean(key, false))
                }
                legacyEditor.remove(key)
            }
        }

        editor.apply()
        legacyEditor.apply()
    }
}
