package com.example.bidcarftagent.presentation.home

import com.example.bidcarftagent.presentation.home.model.OnboardingPage

data class RecentFile(
    val name: String,
    val ts: Long // epoch millis
)

data class HomeUiState(
    val pages: List<OnboardingPage> = emptyList(),
    val currentPage: Int = 0,
    val isLoading: Boolean = false,
    val isUploadSheetVisible: Boolean = false,
    val selectedFileName: String? = null,
    val isUploadSuccess: Boolean = false,
    val recentFiles: List<RecentFile> = emptyList(),
    val userName: String = "",
    val userPhotoUrl: String? = null
)

