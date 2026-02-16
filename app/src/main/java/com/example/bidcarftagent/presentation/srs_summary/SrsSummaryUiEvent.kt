package com.example.bidcarftagent.presentation.srs_summary

sealed interface SrsSummaryUiEvent {
    object NavigateToAgentProcessing : SrsSummaryUiEvent
    data class ShowMessage(val message: String) : SrsSummaryUiEvent
}
