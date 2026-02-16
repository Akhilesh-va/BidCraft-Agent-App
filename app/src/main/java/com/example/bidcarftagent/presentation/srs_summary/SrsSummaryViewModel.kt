package com.example.bidcarftagent.presentation.srs_summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

@HiltViewModel
class SrsSummaryViewModel @Inject constructor(
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val backendRepository: com.example.bidcarftagent.data.repository.BackendRepository,
    private val localRepo: com.example.bidcarftagent.data.local.LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SrsSummaryUiState())
    val uiState = _uiState.asStateFlow()
    // prevent duplicate concurrent generate calls
    private val isGenerating = AtomicBoolean(false)

    init {
        // Support cases where the navigation sets the savedStateHandle after this ViewModel is created.
        // Use StateFlows from SavedStateHandle so we react to updates.
        viewModelScope.launch {
            val fileFlow = savedStateHandle.getStateFlow("fileName", null as String?)
            val srsFlow = savedStateHandle.getStateFlow("srsJson", null as String?)

            // If srsJson already present, load immediately; otherwise try cached SRS, otherwise show dummy.
            val initialSrs = srsFlow.value
            val initialFile = fileFlow.value
            if (!initialSrs.isNullOrBlank()) {
                loadFromJson(initialFile, initialSrs)
            } else {
                // try cache
                if (!initialFile.isNullOrBlank()) {
                    val cached = localRepo.getSrsForFile(initialFile)
                    if (!cached.isNullOrBlank()) {
                        loadFromJson(initialFile, cached)
                    } else {
                        loadDummySummary(initialFile)
                    }
                } else {
                    loadDummySummary(initialFile)
                }
            }

            // Collect future updates and replace UI when srsJson arrives
            launch {
                srsFlow.collect { s ->
                    if (!s.isNullOrBlank()) {
                        val f = fileFlow.value
                        loadFromJson(f, s)
                    }
                }
            }
        }
    }

    private fun loadDummySummary(fileName: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Simulate processing
            // delay(1000) 
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    fileName = fileName ?: "project_requirements_v1.pdf",
                    overview = "The project aims to develop a comprehensive E-commerce mobile application for iOS and Android platforms. The goal is to provide a seamless shopping experience with features like product browsing, secure payments, and order tracking.",
                    requirements = listOf(
                        "User Authentication (Email, Social Login)",
                        "Product Catalog with Search & Filter",
                        "Shopping Cart & Checkout Process",
                        "Payment Gateway Integration (Stripe/PayPal)",
                        "Order Management & History",
                        "Push Notifications for Order Updates"
                    ),
                    modules = listOf(
                        "Authentication Module",
                        "Product Management Module",
                        "Cart & Checkout Module",
                        "Payment Module",
                        "User Profile Module"
                    ),
                    constraints = listOf(
                        "Must support Android 10+ and iOS 14+",
                        "Use Jetpack Compose for Android UI",
                        "Backend API provided via REST",
                        "Secure data handling (GDPR compliance)"
                    )
                ) 
            }
        }
    }

    private fun loadFromJson(fileName: String?, srsJson: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val gson = com.google.gson.Gson()
                val parsed = gson.fromJson(srsJson, Map::class.java)
                // support wrapper { ok: true, srs: { ... } } or direct srs object
                val srsObj = when {
                    parsed.containsKey("srs") -> parsed["srs"]
                    parsed.containsKey("ok") && parsed.containsKey("srs") -> parsed["srs"]
                    else -> parsed
                } as? Map<*, *> ?: emptyMap<Any, Any>()
                val overview = srsObj["projectOverview"] as? String ?: ""
                val keyReq = (srsObj["keyRequirements"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val modules = (srsObj["scopeAndModules"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val constraints = (srsObj["constraints"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val rawText = (parsed["rawText"] as? String) ?: ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        fileName = fileName ?: "srs.pdf",
                        overview = overview,
                        requirements = keyReq,
                        modules = modules,
                        constraints = constraints,
                        rawText = rawText
                    )
                }
            } catch (e: Exception) {
                // fallback
                loadDummySummary(fileName)
            }
        }
    }

    fun onEditClicked() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun onCancelEdit() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun onSaveEdit(
        overview: String,
        requirements: List<String>,
        modules: List<String>,
        constraints: List<String>
    ) {
        _uiState.update {
            it.copy(
                isEditing = false,
                overview = overview,
                requirements = requirements,
                modules = modules,
                constraints = constraints
            )
        }

        // Persist the edited SRS into local cache so next screen/request can read modified JSON
        viewModelScope.launch {
            try {
                val current = _uiState.value
                val fileName = if (current.fileName.isBlank()) "srs.pdf" else current.fileName
                val srsMap = mapOf(
                    "projectOverview" to overview,
                    "keyRequirements" to requirements,
                    "scopeAndModules" to modules,
                    "constraints" to constraints
                )
                val payload = mapOf(
                    "ok" to true,
                    "srs" to srsMap,
                    "rawText" to current.rawText
                )
                val json = com.google.gson.Gson().toJson(payload)
                localRepo.saveSrsForFile(fileName, json)
                Log.d("SrsSummaryViewModel", "Saved edited SRS for $fileName, len=${json.length}")
            } catch (e: Exception) {
                Log.w("SrsSummaryViewModel", "Failed to save edited SRS", e)
            }
        }
    }

    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<SrsSummaryUiEvent>()
    val events = _events.asSharedFlow()

    fun onApproveClick() {
        // guard against re-entrancy / rapid multiple taps
        if (!isGenerating.compareAndSet(false, true)) return

        // Persist current UI state as SRS JSON (handles edited state) immediately
        val current = _uiState.value
        val file = if (current.fileName.isBlank()) "srs.pdf" else current.fileName
        val srsMap = mapOf(
            "projectOverview" to current.overview,
            "keyRequirements" to current.requirements,
            "scopeAndModules" to current.modules,
            "constraints" to current.constraints
        )
        val payload = mapOf(
            "ok" to true,
            "srs" to srsMap,
            "rawText" to current.rawText
        )
        val json = com.google.gson.Gson().toJson(payload)
        viewModelScope.launch {
            try {
                localRepo.saveSrsForFile(file, json)
                Log.d("SrsSummaryViewModel", "Saved SRS before generate for $file, len=${json.length}")
            } catch (e: Exception) {
                Log.w("SrsSummaryViewModel", "Failed to save SRS before generate", e)
            }
        }

        // mark we're waiting for this file's proposal
        localRepo.setWaitingForProposalFile(file)
        Log.d("SrsSummaryViewModel", "Set waiting_for_proposal_file = $file")

        // Start generate in background (do not await to navigate)
        viewModelScope.launch {
            try {
                val resp = backendRepository.generateProposalForFile(file)
                Log.d("SrsSummaryViewModel", "background generateProposal response len=${resp?.length ?: 0}")
            } catch (e: Exception) {
                Log.w("SrsSummaryViewModel", "Background generate failed", e)
            } finally {
                isGenerating.set(false)
            }
        }

        // Immediately show loading for 5s then navigate to AgentProcessing
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            kotlinx.coroutines.delay(5000L)
            _uiState.update { it.copy(isLoading = false) }
            _events.emit(SrsSummaryUiEvent.NavigateToAgentProcessing)
        }
    }

    fun uploadSrsFile(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resp = backendRepository.uploadSrsPdf(filePath)
                Log.d("SrsSummaryViewModel", "uploadSrsFile response length=${resp?.length ?: 0}")
                // For now, we won't parse the response — just stop loading and emit success
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
