package com.jeremy.browser

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {
    private lateinit var geckoSession: GeckoSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val runtime = (application as BrowserApplication).geckoRuntime
        
        geckoSession = GeckoSession().apply {
            open(runtime)
            loadUri("https://duckduckgo.com")
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(session = geckoSession, activity = this)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::geckoSession.isInitialized) {
            geckoSession.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(session: GeckoSession, activity: ComponentActivity) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var urlInput by remember { mutableStateOf("https://duckduckgo.com") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = activity.applicationContext
    var proxyEnabled by remember { mutableStateOf(ProxySettingsManager.isEnabled(context)) }
    var proxyHost by remember { mutableStateOf(ProxySettingsManager.getHost(context)) }
    var proxyPort by remember { mutableStateOf(ProxySettingsManager.getPort(context).toString()) }

    // Keep the address bar synced when pages change internally (e.g. following links)
    DisposableEffect(session) {
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                s: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                url?.let { urlInput = it }
            }
        }

        onDispose {
            session.navigationDelegate = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val target = if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) {
                                    urlInput
                                } else if (Patterns.WEB_URL.matcher(urlInput).matches()) {
                                    "https://$urlInput"
                                } else {
                                    "https://duckduckgo.com/?q=${Uri.encode(urlInput)}"
                                }
                                session.loadUri(target)
                                keyboardController?.hide()
                            }
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Proxy Settings",
                            tint = if (proxyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(modifier = Modifier.height(56.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { session.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = { session.goForward() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    GeckoView(context).apply {
                        setSession(session)
                        isFocusable = true
                        isFocusableInTouchMode = true
                    }
                }
            )
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Proxy Settings (Orbot)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable SOCKS Proxy")
                            Switch(
                                checked = proxyEnabled,
                                onCheckedChange = { proxyEnabled = it }
                            )
                        }
                        OutlinedTextField(
                            value = proxyHost,
                            onValueChange = { proxyHost = it },
                            label = { Text("Proxy Host") },
                            singleLine = true,
                            enabled = proxyEnabled
                        )
                        OutlinedTextField(
                            value = proxyPort,
                            onValueChange = { proxyPort = it },
                            label = { Text("Proxy Port (e.g. 9050)") },
                            singleLine = true,
                            enabled = proxyEnabled
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val portInt = proxyPort.toIntOrNull() ?: 9050
                        ProxySettingsManager.saveSettings(context, proxyEnabled, proxyHost, portInt)
                        showSettingsDialog = false

                        val intent = activity.intent
                        activity.finish()
                        activity.startActivity(intent)
                    }) {
                        Text("Save & Restart")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
