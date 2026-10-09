package com.jeremy.browser

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class BrowserApplication : Application() {
    val geckoRuntime: GeckoRuntime by lazy {
        if (ProxySettingsManager.isEnabled(this)) {
            val host = ProxySettingsManager.getHost(this)
            val port = ProxySettingsManager.getPort(this).toString()

            // Force JVM/Android system properties so Gecko's networking stack tunnels via SOCKS5
            System.setProperty("socksProxyHost", host)
            System.setProperty("socksProxyPort", port)
            // Ensure DNS lookups are also resolved remotely through the SOCKS proxy to prevent leaks
            System.setProperty("java.net.socks.useSystemProxies", "true")
        } else {
            // Clear proxy properties if disabled
            System.clearProperty("socksProxyHost")
            System.clearProperty("socksProxyPort")
        }

        val settings = GeckoRuntimeSettings.Builder()
            .aboutConfigEnabled(true)
            .build()

        GeckoRuntime.create(this, settings)
    }
}
