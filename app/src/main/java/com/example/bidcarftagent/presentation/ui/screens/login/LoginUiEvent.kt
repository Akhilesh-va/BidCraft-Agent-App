package com.example.bidcarftagent.presentation.ui.screens.login

sealed class LoginUiEvent {
    object NavigateToHome : LoginUiEvent()
    data class NavigateToOtp(val phoneNumberWithCode: String) : LoginUiEvent()
    data class ShowError(val message: String) : LoginUiEvent()
}

