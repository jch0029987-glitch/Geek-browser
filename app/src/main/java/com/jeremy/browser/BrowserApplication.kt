package com.jeremy.browser

import android.app.Application
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class BrowserApplication : Application() {
    lateinit var geckoRuntime: GeckoRuntime
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize runtime cleanly with standard settings
        val settings = GeckoRuntimeSettings.Builder()
            .aboutConfigEnabled(true)
            .build()

        geckoRuntime = GeckoRuntime.create(this, settings)

        // Apply SOCKS5 proxy dynamically via GeckoPreferenceController if enabled
        if (ProxySettingsManager.isEnabled(this)) {
            val host = ProxySettingsManager.getHost(this) // "127.0.0.1"
            val port = ProxySettingsManager.getPort(this) // 9050
            applySocksProxy(host, port)
        }
    }

    private fun applySocksProxy(host: String, port: Int) {
        try {
            val prefs = geckoRuntime.preferenceController
            prefs.set("network.proxy.type", 1)
            prefs.set("network.proxy.socks", host)
            prefs.set("network.proxy.socks_port", port)
            prefs.set("network.proxy.socks_version", 5)
            prefs.set("network.proxy.socks_remote_dns", true)
            
            Log.d("BrowserApp", "SOCKS5 proxy successfully configured for $host:$port")
        } catch (e: Exception) {
            Log.e("BrowserApp", "Failed to apply SOCKS proxy preferences", e)
        }
    }
}
