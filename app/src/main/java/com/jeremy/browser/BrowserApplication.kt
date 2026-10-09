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

        val settingsBuilder = GeckoRuntimeSettings.Builder()
            .aboutConfigEnabled(true)

        if (ProxySettingsManager.isEnabled(this)) {
            val host = ProxySettingsManager.getHost(this) // "127.0.0.1"
            val port = ProxySettingsManager.getPort(this) // 9050

            // Safely pass network proxy settings via arguments array
            try {
                settingsBuilder.arguments(
                    arrayOf(
                        "-setpref", "network.proxy.type=1",
                        "-setpref", "network.proxy.socks=$host",
                        "-setpref", "network.proxy.socks_port=$port",
                        "-setpref", "network.proxy.socks_version=5",
                        "-setpref", "network.proxy.socks_remote_dns=true"
                    )
                )
            } catch (e: Exception) {
                Log.e("BrowserApp", "Failed to set proxy arguments", e)
            }
        }

        // Initialize runtime cleanly
        geckoRuntime = GeckoRuntime.create(this, settingsBuilder.build())
    }
}
