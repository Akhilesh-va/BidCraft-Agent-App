package com.example.bidcarftagent.presentation.srs_summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SrsSummaryScreen(
    viewModel: SrsSummaryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAgentProcessing: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event: SrsSummaryUiEvent ->
            when (event) {
                is SrsSummaryUiEvent.NavigateToAgentProcessing -> onNavigateToAgentProcessing()
                is SrsSummaryUiEvent.ShowMessage -> {
                    // Show a toast or snackbar with the message
                    // Note: Toast requires context, would need to be implemented with SnackbarHost
                }
            }
        }
    }

    // Local state for editing, initialized when entering edit mode
    var editableOverview by remember(uiState.isEditing) { mutableStateOf(uiState.overview) }
    var editableRequirements by remember(uiState.isEditing) { mutableStateOf(uiState.requirements.joinToString("\n")) }
    var editableModules by remember(uiState.isEditing) { mutableStateOf(uiState.modules.joinToString("\n")) }
    var editableConstraints by remember(uiState.isEditing) { mutableStateOf(uiState.constraints.joinToString("\n")) }

    Scaffold(
        topBar = { 
            SummaryTopBar(
                isEditing = uiState.isEditing,
                onBackClick = onNavigateBack
            ) 
        },
        bottomBar = {
            if (uiState.isEditing) {
                BottomActionBarEdit(
                    onCancelClick = viewModel::onCancelEdit,
                    onSaveClick = {
                        viewModel.onSaveEdit(
                            overview = editableOverview,
                            requirements = editableRequirements.split("\n").filter { it.isNotBlank() },
                            modules = editableModules.split("\n").filter { it.isNotBlank() },
                            constraints = editableConstraints.split("\n").filter { it.isNotBlank() }
                        )
                    }
                )
            } else {
                BottomActionBarPreview(
                    onEditClick = viewModel::onEditClicked,
                    onApproveClick = viewModel::onApproveClick,
                    isLoading = uiState.isLoading
                )
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isEditing) {
                EditableSummaryContent(
                    fileName = uiState.fileName,
                    overview = editableOverview,
                    onOverviewChange = { editableOverview = it },
                    requirements = editableRequirements,
                    onRequirementsChange = { editableRequirements = it },
                    modules = editableModules,
                    onModulesChange = { editableModules = it },
                    constraints = editableConstraints,
                    onConstraintsChange = { editableConstraints = it }
                )
            } else {
                ReadOnlySummaryContent(uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryTopBar(
    isEditing: Boolean = false,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = if (isEditing) "Edit Summary" else "Review Summary",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFFF5F5F5)
        )
    )
}

@Composable
fun ReadOnlySummaryContent(uiState: SrsSummaryUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            FileInfoCard(fileName = uiState.fileName)
        }

        item {
            SummarySectionCard(
                title = "Project Overview",
                content = uiState.overview
            )
        }

        item {
            SummaryListCard(
                title = "Key Requirements",
                items = uiState.requirements
            )
        }

        item {
            SummaryListCard(
                title = "Scope & Modules",
                items = uiState.modules
            )
        }
        
        if (uiState.constraints.isNotEmpty()) {
            item {
                SummaryListCard(
                    title = "Constraints",
                    items = uiState.constraints
                )
            }
        }
    }
}

@Composable
fun EditableSummaryContent(
    fileName: String,
    overview: String,
    onOverviewChange: (String) -> Unit,
    requirements: String,
    onRequirementsChange: (String) -> Unit,
    modules: String,
    onModulesChange: (String) -> Unit,
    constraints: String,
    onConstraintsChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            FileInfoCard(fileName = fileName)
        }

        item {
            EditableSectionCard(
                title = "Project Overview",
                value = overview,
                onValueChange = onOverviewChange,
                minLines = 4
            )
        }

        item {
            EditableSectionCard(
                title = "Key Requirements (One per line)",
                value = requirements,
                onValueChange = onRequirementsChange,
                minLines = 6
            )
        }

        item {
            EditableSectionCard(
                title = "Scope & Modules (One per line)",
                value = modules,
                onValueChange = onModulesChange,
                minLines = 6
            )
        }

        item {
            EditableSectionCard(
                title = "Constraints (One per line)",
                value = constraints,
                onValueChange = onConstraintsChange,
                minLines = 4
            )
        }
    }
}

@Composable
fun FileInfoCard(fileName: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF1897FF)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (fileName.isEmpty()) "Loading..." else fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Upload Successful",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF4CAF50)
                    )
                )
            }
            
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun SummarySectionCard(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black.copy(alpha = 0.7f),
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
            )
        }
    }
}

@Composable
fun EditableSectionCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 3
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = minLines,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1897FF),
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun SummaryListCard(title: String, items: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            items.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(Color(0xFF1897FF), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Black.copy(alpha = 0.8f)
                        ),
                         modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomActionBarPreview(
    onEditClick: () -> Unit,
    onApproveClick: () -> Unit,
    isLoading: Boolean = false
) {
    BottomActionBarLayout(
        secondaryButton = {
            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1897FF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1897FF)
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Manually",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        primaryButton = {
            Button(
                onClick = onApproveClick,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoading) Color.LightGray else Color(0xFF1897FF)
                ),
                 elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF1897FF), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Approve",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    )
}

@Composable
fun BottomActionBarEdit(
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    BottomActionBarLayout(
        secondaryButton = {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Gray
                )
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        primaryButton = {
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50) // Green for save
                ),
                 elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}

@Composable
fun BottomActionBarLayout(
    secondaryButton: @Composable RowScope.() -> Unit,
    primaryButton: @Composable RowScope.() -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            secondaryButton()
            primaryButton()
        }
    }
}
