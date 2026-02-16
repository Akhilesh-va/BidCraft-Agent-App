package com.example.bidcarftagent.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bidcarftagent.data.repository.BackendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val backendRepository: BackendRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _isUploadSuccess = MutableStateFlow(false)
    val isUploadSuccess = _isUploadSuccess.asStateFlow()

    fun uploadProfileFile(filePath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = backendRepository.uploadProfilePdf(filePath)
                // success only when backend returns ok:true
                var ok = false
                if (!resp.isNullOrBlank()) {
                    try {
                        val jo = org.json.JSONObject(resp)
                        ok = jo.optBoolean("ok", false)
                    } catch (e: Exception) { /* ignore */ }
                }
                _isUploadSuccess.value = ok
            } catch (e: Exception) {
                // log handled in repository
                _isUploadSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}

