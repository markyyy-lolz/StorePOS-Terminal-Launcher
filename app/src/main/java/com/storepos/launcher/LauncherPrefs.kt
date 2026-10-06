package com.storepos.launcher

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

class LauncherPrefs(private val context: Context) {
    private val prefs = context.getSharedPreferences("storepos_terminal_launcher", Context.MODE_PRIVATE)

    fun hasAdminPin(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    fun setAdminPin(pin: String) = prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    fun verifyAdminPin(pin: String): Boolean = prefs.getString(KEY_PIN_HASH, null) == hash(pin)

    var kioskEnabled: Boolean
        get() = prefs.getBoolean(KEY_KIOSK, false)
        set(value) = prefs.edit().putBoolean(KEY_KIOSK, value).apply()

    var autoOpenStorePos: Boolean
        get() = prefs.getBoolean(KEY_AUTO_OPEN, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_OPEN, value).apply()

    private fun hash(pin: String): String {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        return MessageDigest.getInstance("SHA-256")
            .digest(("storepos-terminal:" + id + ":" + pin).toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private companion object {
        const val KEY_PIN_HASH = "admin_pin_hash"
        const val KEY_KIOSK = "kiosk_enabled"
        const val KEY_AUTO_OPEN = "auto_open_storepos"
    }
}
