package com.example.bidcarftagent.presentation.ui.screens.otp

data class OtpUiState(
    val phoneNumberWithCode: String = "",
    val digits: List<String> = List(6) { "" },
    val isVerifying: Boolean = false,
    val errorMessage: String? = null
)

