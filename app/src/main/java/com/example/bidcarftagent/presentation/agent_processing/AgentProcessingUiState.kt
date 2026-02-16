package com.example.bidcarftagent.presentation.agent_processing

enum class StepStatus {
    PENDING,
    ACTIVE,
    DONE
}

data class AgentStep(
    val title: String,
    val status: StepStatus = StepStatus.PENDING,
    val messages: List<String> = emptyList()
)

data class AgentProcessingUiState(
    val steps: List<AgentStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val progress: Float = 0f,
    val isCompleted: Boolean = false,
    val finalResultText: String = "",
    val finalProposalHtml: String? = null,
    val showProposal: Boolean = false,
    val proposalReady: Boolean = false,
    val isSavingPdf: Boolean = false,
    val isFeasible: Boolean? = null,
    val feasibilityReasons: List<String> = emptyList(),
    val showFeasibilityReasons: Boolean = false
)
