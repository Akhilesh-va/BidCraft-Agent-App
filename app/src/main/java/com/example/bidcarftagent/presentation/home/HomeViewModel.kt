package com.example.bidcarftagent.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bidcarftagent.presentation.home.model.OnboardingPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bidcarftagent.data.local.LocalStorageRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val backendRepository: com.example.bidcarftagent.data.repository.BackendRepository,
    private val localRepo: LocalStorageRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val samplePages = listOf(
        OnboardingPage(
            title = "What we do",
            description = "We convert client SRS into winning proposals using AI-powered templates.",
            icon = Icons.Default.Info
        ),
        OnboardingPage(
            title = "How it works",
            description = "Upload SRS → AI analyzes requirements → Generate structured proposal drafts.",
            icon = Icons.Default.PlayCircle
        ),
        OnboardingPage(
            title = "Get started",
            description = "Upload the client SRS to begin. Our assistant guides you through the rest.",
            icon = Icons.Default.Description
        )
    )

    private val _uiState = MutableStateFlow(HomeUiState(pages = samplePages))
    val uiState = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<HomeNavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()
    
    private val _allRecentFiles = MutableStateFlow<List<com.example.bidcarftagent.presentation.home.RecentFile>>(emptyList())
    val allRecentFiles = _allRecentFiles.asStateFlow()

    init {
        // load recent files into state (show top 3)
        viewModelScope.launch {
            val rec = localRepo.getRecentFiles().take(3)
            val user = firebaseAuth.currentUser
            val name = user?.displayName ?: user?.email ?: "Welcome"
            val photo = user?.photoUrl?.toString()
            _uiState.update { it.copy(recentFiles = rec, userName = name, userPhotoUrl = photo) }
        }
    }

    fun onPageChanged(index: Int) {
        if (index in _uiState.value.pages.indices) {
            _uiState.value = _uiState.value.copy(currentPage = index)
        }
    }

    fun onUploadClicked() {
        val fileName = _uiState.value.selectedFileName ?: "Project_Requirements.pdf"
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(2000) // Simulate network call (kept for UX)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isUploadSuccess = true,
                    isUploadSheetVisible = false,
                    selectedFileName = null // Reset selection
                )
            }
            _navigationEvents.emit(HomeNavigationEvent.NavigateToSummary(fileName, null))
        }
    }

    fun onUploadSuccessConsumed() {
        _uiState.update { it.copy(isUploadSuccess = false) }
    }

    fun onNewBidClick() {
        _uiState.value = _uiState.value.copy(isUploadSheetVisible = true)
    }

    fun onUploadSheetDismiss() {
        _uiState.value = _uiState.value.copy(isUploadSheetVisible = false)
    }

    fun onFileSelected(fileName: String) {
        _uiState.value = _uiState.value.copy(selectedFileName = fileName)
    }

    fun uploadSrsFile(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resp = backendRepository.uploadSrsPdf(filePath)
                _uiState.update {
                    it.copy(isLoading = false, isUploadSuccess = true, isUploadSheetVisible = false, selectedFileName = null)
                }
                // persist recent file and update state
                val fname = java.io.File(filePath).name
                localRepo.addRecentFile(fname)
                // persist SRS JSON for later retrieval by summary screen
                if (!resp.isNullOrBlank()) {
                    localRepo.saveSrsForFile(fname, resp)
                }
                val rec = localRepo.getRecentFiles().take(3)
                _uiState.update { it.copy(recentFiles = rec) }
                _navigationEvents.emit(HomeNavigationEvent.NavigateToSummary(java.io.File(filePath).name, resp))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadAllRecentFiles() {
        viewModelScope.launch {
            val rec = localRepo.getRecentFiles()
            _allRecentFiles.value = rec
        }
    }

    fun clearAllRecentFilesView() {
        _allRecentFiles.value = emptyList()
    }

    fun uploadProfileFile(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resp = backendRepository.uploadProfilePdf(filePath)
                var ok = false
                if (!resp.isNullOrBlank()) {
                    try {
                        val jo = org.json.JSONObject(resp)
                        ok = jo.optBoolean("ok", false)
                    } catch (e: Exception) { /* ignore */ }
                }
                _uiState.update {
                    it.copy(isLoading = false, isUploadSuccess = ok, isUploadSheetVisible = false, selectedFileName = null)
                }
                if (ok) {
                    val fname = java.io.File(filePath).name
                    localRepo.addRecentFile(fname)
                    val rec = localRepo.getRecentFiles().take(3)
                    _uiState.update { it.copy(recentFiles = rec) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSettingsClick() {
        viewModelScope.launch {
            _navigationEvents.emit(HomeNavigationEvent.NavigateToSettings)
        }
    }
}

sealed interface HomeNavigationEvent {
    data class NavigateToSummary(val fileName: String, val srsJson: String?) : HomeNavigationEvent
    object NavigateToSettings : HomeNavigationEvent
}

