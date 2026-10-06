package com.storepos.launcher

import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class LauncherPrefs(private val context: Context) {
    private val prefs = context.getSharedPreferences("storepos_terminal_launcher", Context.MODE_PRIVATE)

    fun hasAdminPin(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun setAdminPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, makePbkdf2Hash(pin)).apply()
    }

    fun verifyAdminPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_PIN_HASH, null) ?: return false

        if (saved.startsWith("pbkdf2$")) {
            return verifyPbkdf2(pin, saved)
        }

        val matchesLegacy = MessageDigest.isEqual(
            saved.toByteArray(),
            legacyHash(pin).toByteArray()
        )

        if (matchesLegacy) setAdminPin(pin)
        return matchesLegacy
    }

    var kioskEnabled: Boolean
        get() = prefs.getBoolean(KEY_KIOSK, false)
        set(value) = prefs.edit().putBoolean(KEY_KIOSK, value).apply()

    var autoOpenStorePos: Boolean
        get() = prefs.getBoolean(KEY_AUTO_OPEN, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_OPEN, value).apply()

    var hideEscapeApps: Boolean
        get() = prefs.getBoolean(KEY_HIDE_ESCAPE_APPS, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_ESCAPE_APPS, value).apply()

    private fun makePbkdf2Hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return listOf(
            "pbkdf2",
            PBKDF2_ITERATIONS.toString(),
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(hash, Base64.NO_WRAP)
        ).joinToString("$")
    }

    private fun verifyPbkdf2(pin: String, saved: String): Boolean {
        return runCatching {
            val parts = saved.split("$")
            if (parts.size != 4) return false
            val iterations = parts[1].toInt()
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expected = Base64.decode(parts[3], Base64.NO_WRAP)
            val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, expected.size * 8)
            val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            MessageDigest.isEqual(expected, actual)
        }.getOrDefault(false)
    }

    private fun legacyHash(pin: String): String {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        return MessageDigest.getInstance("SHA-256")
            .digest(("storepos-terminal:" + id + ":" + pin).toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private companion object {
        const val KEY_PIN_HASH = "admin_pin_hash"
        const val KEY_KIOSK = "kiosk_enabled"
        const val KEY_AUTO_OPEN = "auto_open_storepos"
        const val KEY_HIDE_ESCAPE_APPS = "hide_escape_apps"
        const val PBKDF2_ITERATIONS = 120_000
        const val SALT_BYTES = 16
        const val KEY_BITS = 256
    }
}
