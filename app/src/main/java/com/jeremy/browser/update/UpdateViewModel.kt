package com.jeremy.browser.update

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val updateManager = AppUpdateManager(application)

    var uiState by mutableStateOf<UpdateUiState>(UpdateUiState.Idle)
        private set

    fun checkForUpdate() {
        uiState = UpdateUiState.Checking
        viewModelScope.launch {
            val release = updateManager.checkForUpdate()
            uiState = if (release != null) {
                UpdateUiState.UpdateAvailable(release)
            } else {
                UpdateUiState.UpToDate
            }
        }
    }

    fun downloadAndInstall(release: GitHubRelease) {
        uiState = UpdateUiState.Downloading(0)
        viewModelScope.launch {
            val success = updateManager.downloadAndInstall(release.downloadUrl) { progress ->
                uiState = UpdateUiState.Downloading(progress)
            }
            if (!success) {
                uiState = UpdateUiState.Error("Failed to download or initialize update package.")
            }
        }
    }

    fun dismiss() {
        uiState = UpdateUiState.Idle
    }
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class UpdateAvailable(val release: GitHubRelease) : UpdateUiState
    data class Downloading(val progress: Int) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}
