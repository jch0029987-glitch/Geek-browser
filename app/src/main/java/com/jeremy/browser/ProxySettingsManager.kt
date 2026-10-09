package com.jeremy.browser

import android.content.Context
import android.content.SharedPreferences

object ProxySettingsManager {
    private const val PREFS_NAME = "proxy_prefs"
    private const val KEY_ENABLED = "proxy_enabled"
    private const val KEY_HOST = "proxy_host"
    private const val KEY_PORT = "proxy_port"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_ENABLED, false)
    fun getHost(context: Context): String = getPrefs(context).getString(KEY_HOST, "127.0.0.1") ?: "127.0.0.1"
    fun getPort(context: Context): Int = getPrefs(context).getInt(KEY_PORT, 9050)

    fun saveSettings(context: Context, enabled: Boolean, host: String, port: Int) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_ENABLED, enabled)
            putString(KEY_HOST, host)
            putInt(KEY_PORT, port)
            apply()
        }
    }
}
