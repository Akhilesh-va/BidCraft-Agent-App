package com.example.bidcarftagent.presentation.agent_processing

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bidcarftagent.core.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface AgentProcessingUiEvent {
    data class ShowMessage(val message: String) : AgentProcessingUiEvent
}

@HiltViewModel
class AgentProcessingViewModel @Inject constructor(
    private val pdfGenerator: PdfGenerator,
    @ApplicationContext private val context: Context,
    private val localRepo: com.example.bidcarftagent.data.local.LocalStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentProcessingUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AgentProcessingUiEvent>()
    val events = _events.asSharedFlow()
    // holds a pending proposal filename if the backend response arrived before processing finished
    private var pendingProposalName: String? = null

    init {
        initializeSteps()
        startProcessingSimulation()
        // start watcher to enable actions when proposal arrives
        watchForProposalReady()
        // collect proposalSaved events for immediate reaction
        viewModelScope.launch {
            try {
                localRepo.proposalSaved.collect { fname ->
                    android.util.Log.d("AgentProcessingVM", "proposalSaved event received for $fname")
                    // if processing already completed and waiting marker matches, mark ready
                    val waiting = localRepo.getWaitingForProposalFile()
                    android.util.Log.d("AgentProcessingVM", "waiting=$waiting, isCompleted=${_uiState.value.isCompleted}")
                    if (!waiting.isNullOrBlank() && waiting == fname && _uiState.value.isCompleted) {
                        android.util.Log.d("AgentProcessingVM", "conditions met, enabling proposalReady for $fname")
                        val proposal = localRepo.getProposalForFile(fname)
                        if (!proposal.isNullOrBlank()) {
                            val gson = com.google.gson.Gson()
                            val parsed = try { gson.fromJson(proposal, Map::class.java) as? Map<*, *> } catch (_: Exception) { null }
                            val html = parsed?.get("reportHtml") as? String ?: parsed?.get("html") as? String
                            _uiState.update { it.copy(proposalReady = true, finalProposalHtml = html ?: it.finalProposalHtml) }
                            extractAndSetFeasibility(proposal)
                            localRepo.clearWaitingForProposalFile()
                        }
                    } else {
                        // store pending name so when processing completes we can check
                        android.util.Log.d("AgentProcessingVM", "storing pendingProposalName = $fname")
                        pendingProposalName = fname
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun initializeSteps() {
        val initialSteps = listOf(
            AgentStep("Analysing the Document", StepStatus.ACTIVE),
            AgentStep("Requirement Mapping Agent"),
            AgentStep("Designing Solution Architect"),
            AgentStep("Finance Pricing Agent"),
            AgentStep("Master Agent Reviewing Task")
        )
        _uiState.update { 
            it.copy(
                steps = initialSteps, 
                currentStepIndex = 0, 
                progress = 0f,
                finalResultText = getMockFinalResult()
            ) 
        }
    }

    private fun startProcessingSimulation() {
        viewModelScope.launch {
            val streamData = mapOf(
                0 to listOf(
                    "Scanning document structure...",
                    "Extracting key requirements...",
                    "identifying constraints...",
                    "Analysis complete."
                ),
                1 to listOf(
                    "Mapping requirements to modules...",
                    "Defining user stories...",
                    "Prioritizing features...",
                    "Requirements mapped."
                ),
                2 to listOf(
                    "Drafting architecture diagram...",
                    "Selecting technology stack...",
                    "Designing database schema...",
                    "Architecture defined."
                ),
                3 to listOf(
                    "Estimating development range...",
                    "Calculating resource costs...",
                    "Finalizing budget proposal...",
                    "Pricing complete."
                ),
                4 to listOf(
                    "Reviewing all outputs...",
                    "Validating against SRS...",
                    "Generating final report...",
                    "Task approved."
                )
            )

            val totalSteps = _uiState.value.steps.size.toFloat()

            for (stepIndex in 0 until _uiState.value.steps.size) {
                // Ensure current step is ACTIVE if not already
                updateStepStatus(stepIndex, StepStatus.ACTIVE)
                
                // Stream messages for this step
                val messages = streamData[stepIndex] ?: emptyList()
                for (message in messages) {
                    delay(1500) // Simulate processing time per message
                    addMessageToStep(stepIndex, message)
                }
                
                delay(1000) // Final pause before completing step
                
                // Mark current step as DONE
                updateStepStatus(stepIndex, StepStatus.DONE)
                
                // Update progress
                val newProgress = (stepIndex + 1) / totalSteps
                _uiState.update { it.copy(progress = newProgress, currentStepIndex = stepIndex + 1) }

                // Activate next step if exists
                if (stepIndex + 1 < _uiState.value.steps.size) {
                    updateStepStatus(stepIndex + 1, StepStatus.ACTIVE)
                } else {
                    // All steps done
                    _uiState.update { it.copy(isCompleted = true) }
                    // if a pending proposal arrived earlier, and waiting matches, enable buttons now
                    try {
                        val pending = pendingProposalName
                        val waiting = localRepo.getWaitingForProposalFile()
                        if (!pending.isNullOrBlank() && !waiting.isNullOrBlank() && pending == waiting) {
                            val proposal = localRepo.getProposalForFile(pending)
                            if (!proposal.isNullOrBlank()) {
                                val gson = com.google.gson.Gson()
                                val parsed = try { gson.fromJson(proposal, Map::class.java) as? Map<*, *> } catch (_: Exception) { null }
                                val html = parsed?.get("reportHtml") as? String ?: parsed?.get("html") as? String
                                _uiState.update { it.copy(proposalReady = true, finalProposalHtml = html ?: it.finalProposalHtml) }
                                extractAndSetFeasibility(proposal)
                                localRepo.clearWaitingForProposalFile()
                                pendingProposalName = null
                            }
                        }
                    } catch (_: Exception) { /* ignore */ }
                }
            }
        }
    }

    fun onSaveClicked() {
        if (_uiState.value.isSavingPdf) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPdf = true) }
            try {
                android.util.Log.d("AgentProcessingVM", "onSaveClicked: generating PDF...")
                val file = ensurePdfForLastProposal()
                android.util.Log.d("AgentProcessingVM", "onSaveClicked: PDF file=${file?.absolutePath}, exists=${file?.exists()}")
                if (file != null && file.exists()) {
                    val savedUri = saveToDownloads(file)
                    if (savedUri != null) {
                        _events.emit(AgentProcessingUiEvent.ShowMessage("PDF Saved to Downloads"))
                    } else {
                        _events.emit(AgentProcessingUiEvent.ShowMessage("Failed to save to Downloads"))
                    }
                } else {
                    _events.emit(AgentProcessingUiEvent.ShowMessage("Failed to generate PDF"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AgentProcessingVM", "onSaveClicked error", e)
                _events.emit(AgentProcessingUiEvent.ShowMessage("Error: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isSavingPdf = false) }
            }
        }
    }

    private fun saveToDownloads(file: File): android.net.Uri? {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)

        return uri?.also {
            resolver.openOutputStream(it)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    fun onShareClicked() {
        if (_uiState.value.isSavingPdf) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPdf = true) }
            try {
                android.util.Log.d("AgentProcessingVM", "onShareClicked: generating PDF...")
                val file = ensurePdfForLastProposal()
                android.util.Log.d("AgentProcessingVM", "onShareClicked: PDF file=${file?.absolutePath}, exists=${file?.exists()}")
                if (file != null && file.exists()) {
                    val savedUri = saveToDownloads(file)
                    if (savedUri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, savedUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Proposal").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    } else {
                        _events.emit(AgentProcessingUiEvent.ShowMessage("Failed to share PDF"))
                    }
                } else {
                    _events.emit(AgentProcessingUiEvent.ShowMessage("Failed to generate PDF"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AgentProcessingVM", "onShareClicked error", e)
                _events.emit(AgentProcessingUiEvent.ShowMessage("Error: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isSavingPdf = false) }
            }
        }
    }

    /**
     * Extract feasibility info from proposal JSON and update UI state.
     */
    private fun extractAndSetFeasibility(proposalJson: String) {
        try {
            val gson = com.google.gson.Gson()
            val parsed = gson.fromJson(proposalJson, Map::class.java) as? Map<*, *> ?: return
            val feasible = parsed["feasible"]
            val isFeasible = when (feasible) {
                is Boolean -> feasible
                is String -> feasible.equals("true", ignoreCase = true)
                else -> null
            }
            val reasons = mutableListOf<String>()
            (parsed["feasibilityReasons"] as? List<*>)?.forEach { r ->
                val str = r?.toString()
                if (!str.isNullOrBlank()) reasons.add(str)
            }
            android.util.Log.d("AgentProcessingVM", "Feasibility: feasible=$isFeasible, reasons=$reasons")
            _uiState.update { it.copy(isFeasible = isFeasible, feasibilityReasons = reasons) }
        } catch (e: Exception) {
            android.util.Log.w("AgentProcessingVM", "Failed to extract feasibility", e)
        }
    }

    fun onShowFeasibilityReasons() {
        _uiState.update { it.copy(showFeasibilityReasons = true) }
    }

    fun onHideFeasibilityReasons() {
        _uiState.update { it.copy(showFeasibilityReasons = false) }
    }

    fun shareFeasibilityByEmail() {
        viewModelScope.launch {
            try {
                val last = localRepo.getLastProposalFile() ?: ""
                // Try to get email from Firebase user (Google Sign-In)
                val userEmail = try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
                } catch (_: Exception) { null }

                if (userEmail.isNullOrBlank()) {
                    _events.emit(AgentProcessingUiEvent.ShowMessage("No email available from Google Sign-In"))
                    return@launch
                }

                val reasons = _uiState.value.feasibilityReasons
                val subject = "Feasibility reasons for ${if (last.isBlank()) "proposal" else last}"
                val body = StringBuilder()
                body.append("Feasibility: ${_uiState.value.isFeasible}\n\n")
                if (reasons.isNotEmpty()) {
                    reasons.forEachIndexed { i, r ->
                        body.append("${i + 1}. $r\n")
                    }
                } else {
                    body.append("No additional reasons provided.")
                }

                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:${userEmail}")
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body.toString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Send email").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                _events.emit(AgentProcessingUiEvent.ShowMessage("Email app opened"))
            } catch (e: Exception) {
                android.util.Log.e("AgentProcessingVM", "shareFeasibilityByEmail error", e)
                _events.emit(AgentProcessingUiEvent.ShowMessage("Failed to open email: ${e.message}"))
            }
        }
    }

    /**
     * Extract HTML from proposal JSON using the same logic as onShowBidClicked.
     */
    private fun extractHtmlFromProposal(proposalJson: String): String? {
        return try {
            val gson = com.google.gson.Gson()
            val parsed = gson.fromJson(proposalJson, Map::class.java) as? Map<*, *> ?: return null
            // Check top-level keys first
            parsed["reportHtml"] as? String
                ?: parsed["html"] as? String
                // Fallback: stringify rfp.generatedProposal as readable HTML
                ?: (parsed["rfp"] as? Map<*, *>)?.get("generatedProposal")?.let { gp ->
                    buildHtmlFromProposal(gp, parsed)
                }
        } catch (_: Exception) { null }
    }

    /**
     * Convert a generatedProposal object into a styled HTML document
     * so Save/Share produces the same content shown in Show Bid.
     */
    private fun buildHtmlFromProposal(gp: Any?, parsed: Map<*, *>?): String {
        val gson = com.google.gson.Gson()
        val gpMap = (gp as? Map<*, *>) ?: return "<html><body><pre>${gson.toJson(gp)}</pre></body></html>"
        val rfp = parsed?.get("rfp") as? Map<*, *>
        val clientName = rfp?.get("clientName") as? String ?: "Client"

        val sb = StringBuilder()
        sb.append("""
            <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
                body{font-family:sans-serif;padding:16px;color:#222;line-height:1.5}
                h1{color:#003366;font-size:22px}h2{color:#003366;font-size:18px;margin-top:20px}
                h3{font-size:15px;margin-top:14px}
                table{width:100%;border-collapse:collapse;margin:10px 0}
                th,td{border:1px solid #ccc;padding:8px;text-align:left;font-size:13px}
                th{background:#003366;color:#fff}
                ul{padding-left:20px}li{margin:4px 0}
                .label{font-weight:bold}
            </style></head><body>
            <h1>Request for Proposal (RFP) Response</h1>
            <p><span class="label">Client:</span> $clientName</p>
        """.trimIndent())

        // Executive Summary
        (gpMap["executive_summary"] as? Map<*, *>)?.let { es ->
            sb.append("<h2>1. Executive Summary</h2>")
            (es["overview"] as? String)?.let { sb.append("<p><span class='label'>Overview:</span> $it</p>") }
            (es["value_proposition"] as? String)?.let { sb.append("<p><span class='label'>Value Proposition:</span> $it</p>") }
        }

        // Understanding of Requirements
        (gpMap["understanding_of_requirements"] as? Map<*, *>)?.let { ur ->
            sb.append("<h2>2. Understanding of Client Requirements</h2>")
            (ur["project_overview"] as? String)?.let { sb.append("<p>$it</p>") }
            (ur["key_objectives"] as? List<*>)?.let { list ->
                sb.append("<h3>Key Objectives</h3><ul>")
                list.forEach { sb.append("<li>$it</li>") }
                sb.append("</ul>")
            }
            (ur["in_scope"] as? List<*>)?.let { list ->
                sb.append("<h3>In Scope</h3><ul>")
                list.forEach { sb.append("<li>$it</li>") }
                sb.append("</ul>")
            }
        }

        // Requirement Mapping Table
        (gpMap["requirement_mapping"] as? List<*>)?.let { reqs ->
            sb.append("<h2>3. Requirement Mapping</h2>")
            sb.append("<table><tr><th>ID</th><th>Description</th><th>Service</th><th>Technology</th><th>Status</th></tr>")
            reqs.filterIsInstance<Map<*, *>>().forEach { r ->
                sb.append("<tr><td>${r["requirement_id"] ?: ""}</td><td>${r["description"] ?: ""}</td><td>${r["mapped_service"] ?: ""}</td><td>${r["mapped_technology"] ?: ""}</td><td>${r["status"] ?: ""}</td></tr>")
            }
            sb.append("</table>")
        }

        // Solution Architecture
        (gpMap["solution_architecture"] as? Map<*, *>)?.let { sa ->
            sb.append("<h2>4. Solution Architecture</h2>")
            (sa["architecture_overview"] as? String)?.let { sb.append("<p>$it</p>") }
            (sa["components"] as? List<*>)?.let { list ->
                sb.append("<h3>Components</h3><ul>")
                list.forEach { sb.append("<li>$it</li>") }
                sb.append("</ul>")
            }
            (sa["security_considerations"] as? List<*>)?.let { list ->
                sb.append("<h3>Security</h3><ul>")
                list.forEach { sb.append("<li>$it</li>") }
                sb.append("</ul>")
            }
        }

        // Delivery Plan
        (gpMap["delivery_plan"] as? Map<*, *>)?.let { dp ->
            sb.append("<h2>5. Delivery Plan</h2>")
            (dp["phases"] as? List<*>)?.let { phases ->
                sb.append("<table><tr><th>Phase</th><th>Duration</th><th>Deliverables</th></tr>")
                phases.filterIsInstance<Map<*, *>>().forEach { p ->
                    val deliverables = (p["deliverables"] as? List<*>)?.joinToString(", ") ?: ""
                    sb.append("<tr><td>${p["phase_name"] ?: ""}</td><td>${p["duration_weeks"] ?: ""} weeks</td><td>$deliverables</td></tr>")
                }
                sb.append("</table>")
            }
        }

        // Pricing
        (gpMap["pricing"] as? Map<*, *>)?.let { pr ->
            sb.append("<h2>6. Pricing</h2>")
            (pr["total_cost"] as? Any)?.let { sb.append("<p><span class='label'>Total Cost:</span> $it</p>") }
            (pr["line_items"] as? List<*>)?.let { items ->
                sb.append("<table><tr><th>Item</th><th>Amount</th></tr>")
                items.filterIsInstance<Map<*, *>>().forEach { item ->
                    sb.append("<tr><td>${item["description"] ?: item["item"] ?: ""}</td><td>${item["amount"] ?: item["cost"] ?: ""}</td></tr>")
                }
                sb.append("</table>")
            }
        }

        // Team Composition
        (gpMap["team_composition"] as? Map<*, *>)?.let { tc ->
            sb.append("<h2>7. Team Composition</h2>")
            (tc["roles"] as? List<*>)?.let { roles ->
                sb.append("<table><tr><th>Role</th><th>Count</th><th>Rate</th></tr>")
                roles.filterIsInstance<Map<*, *>>().forEach { r ->
                    sb.append("<tr><td>${r["role"] ?: ""}</td><td>${r["count"] ?: ""}</td><td>${r["monthly_rate"] ?: r["rate"] ?: ""}</td></tr>")
                }
                sb.append("</table>")
            }
        }

        sb.append("<hr><p style='text-align:center;color:#888;font-size:12px'>Generated by BidCraft Agentic AI</p>")
        sb.append("</body></html>")
        return sb.toString()
    }

    private suspend fun ensurePdfForLastProposal(): File? {
        val last = localRepo.getLastProposalFile()
        android.util.Log.d("AgentProcessingVM", "ensurePdf: lastProposalFile=$last")
        if (last.isNullOrBlank()) return null

        // Check if we already have a cached PDF
        val existingPdfPath = localRepo.getReportPdfPathForFile(last)
        android.util.Log.d("AgentProcessingVM", "ensurePdf: existingPdfPath=$existingPdfPath")
        if (!existingPdfPath.isNullOrBlank()) {
            val existingFile = File(existingPdfPath)
            if (existingFile.exists()) {
                android.util.Log.d("AgentProcessingVM", "ensurePdf: returning cached PDF")
                return existingFile
            }
        }

        // Use HTML already loaded in UI state (same as Show Bid), or re-extract from JSON
        var html = _uiState.value.finalProposalHtml
        android.util.Log.d("AgentProcessingVM", "ensurePdf: uiState html len=${html?.length ?: 0}")
        if (html.isNullOrBlank()) {
            val proposalJson = localRepo.getProposalForFile(last)
            android.util.Log.d("AgentProcessingVM", "ensurePdf: proposalJson len=${proposalJson?.length ?: 0}")
            if (proposalJson.isNullOrBlank()) return null
            html = extractHtmlFromProposal(proposalJson)
            android.util.Log.d("AgentProcessingVM", "ensurePdf: extracted html len=${html?.length ?: 0}")
        }
        if (html.isNullOrBlank()) {
            android.util.Log.w("AgentProcessingVM", "ensurePdf: no HTML available, cannot generate PDF")
            return null
        }

        return try {
            val outFile = File(context.cacheDir, "${last}_proposal.pdf")
            android.util.Log.d("AgentProcessingVM", "ensurePdf: generating PDF to ${outFile.absolutePath}, html len=${html.length}")
            val path = com.example.bidcarftagent.utils.HtmlPdfExporter.createPdfFromHtml(context, html, outFile)
            android.util.Log.d("AgentProcessingVM", "ensurePdf: HtmlPdfExporter returned path=$path")
            if (!path.isNullOrBlank()) {
                localRepo.savePdfPathForFile(last, path)
                File(path)
            } else null
        } catch (e: Exception) {
            android.util.Log.e("AgentProcessingVM", "ensurePdf: PDF generation failed", e)
            null
        }
    }

    fun onHideProposalClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(showProposal = false) }
        }
    }

    fun onShowBidClicked() {
        viewModelScope.launch {
            try {
                val last = localRepo.getLastProposalFile()
                if (last.isNullOrBlank()) {
                    _events.emit(AgentProcessingUiEvent.ShowMessage("No proposal available"))
                    return@launch
                }
                val proposalJson = localRepo.getProposalForFile(last)
                if (proposalJson.isNullOrBlank()) {
                    _events.emit(AgentProcessingUiEvent.ShowMessage("No proposal available"))
                    return@launch
                }
                val html = extractHtmlFromProposal(proposalJson)
                _uiState.update { it.copy(finalProposalHtml = html, showProposal = !html.isNullOrBlank()) }
                extractAndSetFeasibility(proposalJson)
                if (html.isNullOrBlank()) {
                    _events.emit(AgentProcessingUiEvent.ShowMessage("Proposal received but no HTML content"))
                }
            } catch (e: Exception) {
                _events.emit(AgentProcessingUiEvent.ShowMessage("Failed to load proposal: ${e.message}"))
            }
        }
    }

    private fun sharePdf(file: File) {
        try {
            // Prefer sharing via MediaStore (saved Downloads) to avoid FileProvider blanks on some devices.
            val mediaUri = saveToDownloads(file)
            val shareUri = mediaUri ?: kotlin.runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }.getOrNull()

            if (shareUri == null) {
                _events.tryEmit(AgentProcessingUiEvent.ShowMessage("Unable to share file"))
                return
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Proposal").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch { _events.emit(AgentProcessingUiEvent.ShowMessage("Error sharing PDF: ${e.message}")) }
        }
    }

    private fun updateStepStatus(stepIndex: Int, status: StepStatus) {
        _uiState.update { currentState ->
            val updatedSteps = currentState.steps.toMutableList()
            if (stepIndex in updatedSteps.indices) {
                updatedSteps[stepIndex] = updatedSteps[stepIndex].copy(status = status)
            }
            currentState.copy(steps = updatedSteps)
        }
    }

    private fun addMessageToStep(stepIndex: Int, message: String) {
        _uiState.update { currentState ->
            val updatedSteps = currentState.steps.toMutableList()
            if (stepIndex in updatedSteps.indices) {
                val currentMessages = updatedSteps[stepIndex].messages.toMutableList()
                currentMessages.add(message)
                updatedSteps[stepIndex] = updatedSteps[stepIndex].copy(messages = currentMessages)
            }
            currentState.copy(steps = updatedSteps)
        }
    }

    private fun watchForProposalReady() {
        viewModelScope.launch {
            // Poll for last proposal; when found, set proposalReady = true and load HTML if any
            while (true) {
                try {
                    val waiting = localRepo.getWaitingForProposalFile()
                    // Relaxed condition: if we are waiting for a file, and that file has a proposal, we are good.
                    // We don't strictly need waiting == last, just that waiting has data.
                    if (!waiting.isNullOrBlank() && _uiState.value.isCompleted) {
                        val proposal = localRepo.getProposalForFile(waiting)
                        if (!proposal.isNullOrBlank()) {
                            android.util.Log.d("AgentProcessingVM", "Found proposal for waiting file: $waiting")
                            val gson = com.google.gson.Gson()
                            val parsed = try { gson.fromJson(proposal, Map::class.java) as? Map<*, *> } catch (_: Exception) { null }
                            val html = parsed?.get("reportHtml") as? String ?: parsed?.get("html") as? String
                            _uiState.update { it.copy(proposalReady = true, finalProposalHtml = html ?: it.finalProposalHtml) }
                            extractAndSetFeasibility(proposal)
                            localRepo.clearWaitingForProposalFile()
                            break
                        }
                    }
                } catch (_: Exception) { /* ignore */ }
                delay(1000)
            }
        }
    }

    private fun getMockFinalResult(): String {
        return """
            BidCraft Proposal
            
            1. Executive Summary
            This proposal outlines the development of a comprehensive E-commerce mobile application...
            
            2. Scope of Work
            - User Authentication
            - Product Catalog
            - Shopping Cart
            - Payment Gateway
            
            3. Architecture
            Based on Clean Architecture with MVVM...
            
            4. Timeline & Cost
            Estimated timeline: 12 weeks
            Estimated cost: $25,000
            
            Generated by BidCraft Agentic AI
        """.trimIndent()
    }
}
