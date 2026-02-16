package com.example.bidcarftagent.presentation.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bidcarftagent.presentation.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginUiEvent>()
    val events = _events.asSharedFlow()
    fun onGoogleSignInIdToken(idToken: String) {
        android.util.Log.d("LoginViewModel", "Google ID Token: $idToken")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = authManager.signInWithGoogle(idToken)
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (res.isSuccess) {
                _events.emit(LoginUiEvent.NavigateToHome)
            } else {
                _events.emit(LoginUiEvent.ShowError(res.exceptionOrNull()?.localizedMessage ?: "Sign-in failed"))
            }
        }
    }
    fun showError(message: String) {
        viewModelScope.launch {
            _events.emit(LoginUiEvent.ShowError(message))
        }
    }
    fun onFacebookLoginClick() = showError("Facebook login not implemented")
    fun onTwitterLoginClick() = showError("Twitter login not implemented")
}
