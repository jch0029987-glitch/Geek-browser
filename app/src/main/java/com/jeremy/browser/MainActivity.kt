package com.jeremy.browser

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeremy.browser.data.AppDatabase
import com.jeremy.browser.data.HistoryDao
import com.jeremy.browser.data.HistoryEntity
import com.jeremy.browser.tabs.TabSwitcherBottomSheet
import com.jeremy.browser.tabs.TabViewModel
import com.jeremy.browser.update.UpdateDialog
import com.jeremy.browser.update.UpdateViewModel
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {
    private lateinit var geckoRuntime: GeckoRuntime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geckoRuntime = (application as BrowserApplication).geckoRuntime
        val historyDao = AppDatabase.getDatabase(applicationContext).historyDao()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(
                        runtime = geckoRuntime,
                        activity = this,
                        historyDao = historyDao
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    runtime: GeckoRuntime,
    activity: ComponentActivity,
    historyDao: HistoryDao,
    updateViewModel: UpdateViewModel = viewModel(),
    tabViewModel: TabViewModel = viewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    var showTabSwitcher by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val context = activity.applicationContext
    var proxyEnabled by remember { mutableStateOf(ProxySettingsManager.isEnabled(context)) }
    var proxyHost by remember { mutableStateOf(ProxySettingsManager.getHost(context)) }
    var proxyPort by remember { mutableStateOf(ProxySettingsManager.getPort(context).toString()) }

    // Initialize default tab on startup
    LaunchedEffect(Unit) {
        tabViewModel.initDefaultTab(runtime) { url, title ->
            coroutineScope.launch {
                historyDao.insertHistory(HistoryEntity(url = url, title = title))
            }
        }
    }

    val activeTab = tabViewModel.activeTab
    var urlInput by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "https://duckduckgo.com") }

    // Observe room database history list
    val historyList by historyDao.getAllHistory().collectAsState(initial = emptyList())

    // Keep address bar synced with active tab updates
    LaunchedEffect(activeTab?.url) {
        activeTab?.url?.let { urlInput = it }
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
                                activeTab?.session?.loadUri(target)
                                keyboardController?.hide()
                            }
                        )
                    )
                },
                actions = {
                    // Tab Switcher Button with count badge
                    IconButton(onClick = { showTabSwitcher = true }) {
                        BadgedBox(
                            badge = {
                                if (tabViewModel.tabs.isNotEmpty()) {
                                    Badge { Text("${tabViewModel.tabs.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Tabs"
                            )
                        }
                    }
                    // Check for Updates Button
                    IconButton(onClick = { updateViewModel.checkForUpdate() }) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check for Updates"
                        )
                    }
                    // History Button
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Browsing History"
                        )
                    }
                    // Proxy Settings Button
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
                    IconButton(onClick = { activeTab?.session?.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = { activeTab?.session?.goForward() }) {
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
            activeTab?.let { tab ->
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        GeckoView(ctx).apply {
                            setSession(tab.session)
                            isFocusable = true
                            isFocusableInTouchMode = true
                        }
                    },
                    update = { view ->
                        if (view.session != tab.session) {
                            view.setSession(tab.session)
                        }
                    }
                )
            }
        }

        // Tab Switcher Bottom Sheet
        if (showTabSwitcher) {
            TabSwitcherBottomSheet(
                viewModel = tabViewModel,
                onNewTab = {
                    tabViewModel.createNewTab(runtime, { url, title ->
                        coroutineScope.launch {
                            historyDao.insertHistory(HistoryEntity(url = url, title = title))
                        }
                    })
                    showTabSwitcher = false
                },
                onDismiss = { showTabSwitcher = false }
            )
        }

        // Mount In-App Update Dialog
        UpdateDialog(
            viewModel = updateViewModel,
            onDismiss = {
                updateViewModel.dismiss()
            }
        )

        // Proxy Settings Dialog
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

        // History Drawer/Dialog
        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = { Text("Browsing History") },
                text = {
                    Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                        if (historyList.isEmpty()) {
                            Text("No history recorded yet.", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(historyList) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        onClick = {
                                            activeTab?.session?.loadUri(item.url)
                                            showHistoryDialog = false
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(text = item.url, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            historyDao.clearHistory()
                        }
                    }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
