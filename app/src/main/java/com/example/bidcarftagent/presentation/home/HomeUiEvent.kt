package com.example.bidcarftagent.presentation.home

sealed class HomeUiEvent {
    object UploadClicked : HomeUiEvent()
    object NewBidClicked : HomeUiEvent()
    object UploadSheetDismissed : HomeUiEvent()
    data class FileSelected(val fileName: String) : HomeUiEvent()
}

