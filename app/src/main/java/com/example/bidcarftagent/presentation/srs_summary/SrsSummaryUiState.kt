package com.example.bidcarftagent.presentation.srs_summary

data class SrsSummaryUiState(
    val fileName: String = "",
    val isLoading: Boolean = false,
    val overview: String = "",
    val requirements: List<String> = emptyList(),
    val modules: List<String> = emptyList(),
    val constraints: List<String> = emptyList(),
    val rawText: String = "",
    val isEditing: Boolean = false
)
