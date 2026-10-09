package com.jeremy.browser.tabs

import org.mozilla.geckoview.GeckoSession
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val session: GeckoSession,
    var title: String = "New Tab",
    var url: String = "https://duckduckgo.com"
)
