package com.example.bidcarftagent.presentation.ui.screens.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OtpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OtpUiEvent>()
    val events = _events.asSharedFlow()

    fun setPhone(phoneWithCode: String) {
        _uiState.value = _uiState.value.copy(phoneNumberWithCode = phoneWithCode)
    }

    fun onDigitChanged(index: Int, value: String) {
        val digits = _uiState.value.digits.toMutableList()
        if (index in digits.indices) digits[index] = value.take(1)
        _uiState.value = _uiState.value.copy(digits = digits)
    }

    fun verifyOtp() {
        val code = _uiState.value.digits.joinToString("")
        if (code.length < 6) {
            viewModelScope.launch { _events.emit(OtpUiEvent.ShowError("Enter the 6-digit code")) }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true)
            // Simulate verification; replace with use case call
            delay(1000)
            _uiState.value = _uiState.value.copy(isVerifying = false)
            _events.emit(OtpUiEvent.Verified)
        }
    }
}

