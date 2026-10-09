package com.jeremy.browser.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class TabViewModel : ViewModel() {
    private val _tabs = mutableStateListOf<BrowserTab>()
    val tabs: List<BrowserTab> get() = _tabs

    var activeTabId by mutableStateOf<String?>(null)
        private set

    val activeTab: BrowserTab?
        get() = _tabs.find { it.id == activeTabId } ?: _tabs.firstOrNull()

    fun initDefaultTab(runtime: GeckoRuntime, onHistoryRecorded: (String, String) -> Unit) {
        if (_tabs.isEmpty()) {
            createNewTab(runtime, onHistoryRecorded, "https://duckduckgo.com")
        }
    }

    fun createNewTab(runtime: GeckoRuntime, onHistoryRecorded: (String, String) -> Unit, initialUrl: String = "https://duckduckgo.com") {
        val session = GeckoSession().apply {
            open(runtime)
            navigationDelegate = object : GeckoSession.NavigationDelegate {
                onLocationChange(s: GeckoSession, url: String?, perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
                    url?.let { newUrl ->
                        _tabs.find { it.session == s }?.let { tab ->
                            tab.url = newUrl
                            tab.title = newUrl
                        }
                        onHistoryRecorded(newUrl, newUrl)
                    }
                }
            }
            loadUri(initialUrl)
        }

        val newTab = BrowserTab(session = session, url = initialUrl, title = initialUrl)
        _tabs.add(newTab)
        activeTabId = newTab.id
    }

    fun selectTab(tabId: String) {
        if (_tabs.any { it.id == tabId }) {
            activeTabId = tabId
        }
    }

    fun closeTab(tabId: String) {
        val tabToClose = _tabs.find { it.id == tabId }
        tabToClose?.session?.close()
        _tabs.remove(tabToClose)

        if (activeTabId == tabId) {
            activeTabId = _tabs.lastOrNull()?.id
        }
    }

    override fun onCleared() {
        super.onCleared()
        for (tab in _tabs) {
            tab.session.close()
        }
        _tabs.clear()
    }
}
