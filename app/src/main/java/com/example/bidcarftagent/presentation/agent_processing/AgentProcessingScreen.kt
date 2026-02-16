package com.example.bidcarftagent.presentation.agent_processing

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentProcessingScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AgentProcessingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    // ... (existing effects)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AgentProcessingUiEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BidCraft Agentic AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1897FF)
                            )
                        )
                        Text(
                            text = "We are doing work for you",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            if (uiState.isCompleted && uiState.proposalReady && uiState.isFeasible != null) {
                FeasibilityBottomBar(
                    isFeasible = uiState.isFeasible!!,
                    hasReasons = uiState.feasibilityReasons.isNotEmpty(),
                    onReasonsClick = { viewModel.onShowFeasibilityReasons() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            // Remove existing ProcessingHeader call as it's now in TopBar
            // ProcessingHeader() 
            
            // Spacer(modifier = Modifier.height(24.dp)) // Adjust spacing
            
            OverallProgressBar(progress = uiState.progress)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            VerticalTimeline(
                steps = uiState.steps,
                modifier = Modifier.weight(1f),
                listState = scrollState
            )

            if (uiState.isCompleted) {
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.proposalReady) {
                    // Show Bid button above Save/Share only when proposalReady is true
                    Button(
                        onClick = { viewModel.onShowBidClicked() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1897FF))
                    ) {
                        Text(text = "Show Bid", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    // Show waiting indicator until proposal arrives
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF1897FF), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Waiting for proposal...", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
                    }
                }
            }

            // Show proposal HTML dialog when requested
            if (uiState.showProposal && uiState.finalProposalHtml != null) {
                val html = uiState.finalProposalHtml!!
                androidx.compose.ui.window.Dialog(onDismissRequest = {
                    viewModel.onHideProposalClicked()
                }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Show feasibility reasons dialog
            if (uiState.showFeasibilityReasons && uiState.feasibilityReasons.isNotEmpty()) {
                androidx.compose.ui.window.Dialog(onDismissRequest = {
                    viewModel.onHideFeasibilityReasons()
                }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Feasibility Reasons",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            uiState.feasibilityReasons.forEachIndexed { index, reason ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        ),
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.Black
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.shareFeasibilityByEmail() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1897FF))
                                ) {
                                    Text(
                                        "Share via Email",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.onHideFeasibilityReasons() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1897FF))
                                ) {
                                    Text(
                                        "Close",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ... existing components ...

@Composable
fun FeasibilityBottomBar(
    isFeasible: Boolean,
    hasReasons: Boolean,
    onReasonsClick: () -> Unit
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFeasible) {
                // Feasible = true: show green success indicator full width
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Feasibility: True",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    )
                }
            } else {
                // Feasible = false: show red indicator + Reasons button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Feasibility: False",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    )
                }
                if (hasReasons) {
                    Button(
                        onClick = onReasonsClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text(
                            text = "Reasons",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "BidCraft Agentic AI",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF1897FF), // Primary Blue
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We are doing work for you",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun OverallProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = Color(0xFF1897FF), // Primary Blue
        trackColor = Color(0xFFE3F2FD) // Light Blue
    )
}

@Composable
fun VerticalTimeline(
    steps: List<AgentStep>,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(steps) { index, step ->
            AgentStepItem(
                step = step,
                isLast = index == steps.lastIndex
            )
        }
    }
}

@Composable
fun AgentStepItem(
    step: AgentStep,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline Indicator Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            StepIndicatorCircle(status = step.status)
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(
                            if (step.status == StepStatus.DONE) Color(0xFF1897FF) else Color.LightGray.copy(alpha = 0.5f)
                        )
                        .padding(vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp) // Spacing between steps
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (step.status == StepStatus.PENDING) Color.Gray else Color.Black
                )
            )
            
            if (step.messages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TypingTextArea(
                    messages = step.messages,
                    isActive = step.status == StepStatus.ACTIVE
                )
            }
        }
    }
}

@Composable
fun StepIndicatorCircle(status: StepStatus) {
    val color = when (status) {
        StepStatus.DONE, StepStatus.ACTIVE -> Color(0xFF1897FF) // Primary Blue
        StepStatus.PENDING -> Color.LightGray
    }
    
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color.White)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (status == StepStatus.DONE) {
            Box(
                 modifier = Modifier
                    .size(14.dp)
                    .background(color, CircleShape)
            )
        } else if (status == StepStatus.ACTIVE) {
             Box(
                 modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun TypingTextArea(
    messages: List<String>,
    isActive: Boolean
) {
    Column(
        modifier = Modifier.animateContentSize()
    ) {
        // Show only the last 2 messages to keep it clean
        val visibleMessages = if (messages.size > 2) messages.takeLast(2) else messages
        
        visibleMessages.forEachIndexed { index, message ->
            // Identify if this is the very last message in the entire list
            val isMostRecent = message == messages.last()
            
            if (isMostRecent && isActive) {
                // Typing animation only for the newest message of an active step
                TypewriterText(text = message)
            } else {
                // Static text for previous or older messages
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TypewriterText(text: String) {
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(text) {
        displayedText = ""
        text.forEachIndexed { index, char ->
            displayedText = text.substring(0, index + 1)
            delay(30) // Typing speed
        }
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = displayedText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 13.sp
            )
        )
        // Blinking Cursor
        BlinkingCursor()
    }
}

@Composable
fun BlinkingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.7f at 500
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .width(2.dp)
            .height(16.dp)
            .background(Color.Black.copy(alpha = alpha))
    )
}
