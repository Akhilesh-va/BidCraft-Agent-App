package com.example.bidcarftagent.presentation.ui.screens.otp

sealed class OtpUiEvent {
    object Verified : OtpUiEvent()
    data class ShowError(val message: String) : OtpUiEvent()
}

