package com.jeremy.browser

import android.app.Application
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
            val host = ProxySettingsManager.getHost(this) // e.g. "127.0.0.1"
            val port = ProxySettingsManager.getPort(this) // e.g. 9050

            // Configure GeckoRuntime for SOCKS proxy (Orbot)
            settingsBuilder.socksProxy(host, port)
        }

        geckoRuntime = GeckoRuntime.create(this, settingsBuilder.build())
    }
}
