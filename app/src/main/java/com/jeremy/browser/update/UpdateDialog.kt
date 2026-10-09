package com.jeremy.browser.update

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel,
    onDismiss: () -> Unit
) {
    val state = viewModel.uiState

    if (state !is UpdateUiState.Idle) {
        AlertDialog(
            onDismissRequest = {
                if (state !is UpdateUiState.Downloading) {
                    viewModel.dismiss()
                    onDismiss()
                }
            },
            title = {
                Text(
                    text = when (state) {
                        is UpdateUiState.Checking -> "Checking for Updates"
                        is UpdateUiState.UpToDate -> "You're Up to Date"
                        is UpdateUiState.UpdateAvailable -> "Update Available (${state.release.tagName})"
                        is UpdateUiState.Downloading -> "Downloading Update..."
                        is UpdateUiState.Error -> "Update Failed"
                        else -> "Software Update"
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (state) {
                        is UpdateUiState.Checking -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is UpdateUiState.UpToDate -> {
                            Text("You are running the latest version of Geek Browser.")
                        }
                        is UpdateUiState.UpdateAvailable -> {
                            Text("What's new:")
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = state.release.releaseNotes,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is UpdateUiState.Downloading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${state.progress}% downloaded",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        is UpdateUiState.Error -> {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                when (state) {
                    is UpdateUiState.UpdateAvailable -> {
                        Button(onClick = { viewModel.downloadAndInstall(state.release) }) {
                            Text("Download & Install")
                        }
                    }
                    is UpdateUiState.UpToDate, is UpdateUiState.Error -> {
                        Button(onClick = {
                            viewModel.dismiss()
                            onDismiss()
                        }) {
                            Text("Close")
                        }
                    }
                    else -> {}
                }
            },
            dismissButton = {
                if (state is UpdateUiState.UpdateAvailable) {
                    TextButton(onClick = {
                        viewModel.dismiss()
                        onDismiss()
                    }) {
                        Text("Later")
                    }
                }
            }
        )
    }
}
