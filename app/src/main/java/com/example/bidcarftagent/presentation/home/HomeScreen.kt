package com.example.bidcarftagent.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.bidcarftagent.R
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSummary: (String, String?) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allRecent by viewModel.allRecentFiles.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Navigation Events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is HomeNavigationEvent.NavigateToSummary -> onNavigateToSummary(event.fileName, event.srsJson)
                else -> { /* ignore other navigation events here */ }
            }
        }
    }

    // Upload Success Effect (Toast only)
    LaunchedEffect(uiState.isUploadSuccess) {
        if (uiState.isUploadSuccess) {
            android.widget.Toast.makeText(context, "Document uploaded successfully", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.onUploadSuccessConsumed()
        }
    }
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    // File Picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            var fileName = "Selected Document"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
            selectedFileUri = it
            viewModel.onFileSelected(fileName)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Background Image
        Image(
            painter = painterResource(id = R.drawable.login_bg_light),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        // 3. Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            HomeHeader(userName = uiState.userName, userPhotoUrl = uiState.userPhotoUrl, onProfileClick = onNavigateToProfile)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Search Bar
            HomeSearchBar()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Quick Actions
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val actions = listOf(
                    Triple("New Bid", Icons.Default.Add, Color(0xFF1897FF)),
                    Triple("Analytics", Icons.Default.Analytics, Color(0xFF9C27B0)),
                    Triple("History", Icons.Default.History, Color(0xFFE91E63)),
                    Triple("Settings", Icons.Default.Settings, Color(0xFF607D8B))
                )
                actions.forEach { (label, icon, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                .clickable {
                                    when (label) {
                                        "New Bid" -> viewModel.onNewBidClick()
                                        "History" -> viewModel.loadAllRecentFiles()
                                        "Settings" -> onNavigateToSettings()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = color,
                                modifier = Modifier.size(28.dp)
                            )
                            if (label == "Analytics") {
                                val transition = rememberInfiniteTransition()
                                val scale by transition.animateFloat(
                                    initialValue = 0.96f,
                                    targetValue = 1.04f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 800)
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = (-4).dp, y = (-4).dp)
                                        .scale(scale)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE91E63))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Soon",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recent Activity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Bids",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1897FF),
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { viewModel.loadAllRecentFiles() }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            RecentActivityList(uiState.recentFiles)
            
            Spacer(modifier = Modifier.height(24.dp))
            // Promotional Lottie card (PDF animation) — follows app UI/UX
            PromotionalLottieCard()
        }
        
        // 4. Upload Bottom Sheet
        if (uiState.isUploadSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onUploadSheetDismiss() },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                UploadSrsBottomSheet(
                    selectedFileName = uiState.selectedFileName,
                    isLoading = uiState.isLoading,
                    onPickFileClick = {
                        launcher.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    },
                    onDismiss = { 
                        selectedFileUri = null
                        viewModel.onUploadSheetDismiss() 
                    },
                    onUploadClick = {
                        // copy Uri to temp file and upload via ViewModel
                        val uri = selectedFileUri
                        if (uri != null) {
                            scope.launch {
                                try {
                                    val input = context.contentResolver.openInputStream(uri)
                                    val tmp = java.io.File.createTempFile("upload", ".pdf", context.cacheDir)
                                    java.io.FileOutputStream(tmp).use { out ->
                                        input?.copyTo(out)
                                    }
                                    viewModel.uploadSrsFile(tmp.absolutePath)
                                } catch (e: Exception) {
                                    android.util.Log.w("HomeScreen", "Failed to upload SRS", e)
                                }
                            }
                        }
                    }
                )
            }
        }

        // Show "All recent files" dialog/sheet when available
        if (allRecent.isNotEmpty()) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.clearAllRecentFilesView() },
                title = {
                    Text(text = "All Files")
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        LazyColumn {
                            items(allRecent) { item ->
                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)) {
                                    Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = relativeTime(item.ts), style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                }
                                Divider()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearAllRecentFilesView() }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}


@Composable
fun UploadSrsBottomSheet(
    selectedFileName: String?,
    isLoading: Boolean,
    onPickFileClick: () -> Unit,
    onDismiss: () -> Unit,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp), // Add padding for bottom bars
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Handle bar with close button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onDismiss() }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Lottie Animation
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.upload_cloud))
        
        Box(
            modifier = Modifier
                .size(120.dp) // Increased size for animation
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Upload Client SRS",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Upload the client’s SRS/RFP document (PDF or DOCX)",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Black.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Selected File Display
        if (selectedFileName != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1897FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .background(Color(0xFF1897FF).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .animateContentSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF1897FF)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedFileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50) // Success Green
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (selectedFileName == null) {
            // "Choose File" - Primary Action when no file selected
            Button(
                onClick = onPickFileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1897FF)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Choose File",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            // "Change File" - Secondary Action
            OutlinedButton(
                onClick = onPickFileClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isLoading) Color.Gray else Color(0xFF1897FF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1897FF)
                )
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Change File",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // "Upload Document" - Primary Action
            Button(
                onClick = onUploadClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1897FF)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Document",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HomeHeader(userName: String, userPhotoUrl: String? = null, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black.copy(alpha = 0.6f)
                )
            )
            Text(
                text = if (userName.isBlank()) "User" else userName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            )
        }

        // Profile Icon
        Box {
            IconButton(
                onClick = { onProfileClick() },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            ) {
                if (!userPhotoUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search",
            tint = Color.Black.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Search projects, bids...",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.Black.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun QuickActionsGrid(
    onNewBidClick: () -> Unit
    , onHistoryClick: () -> Unit
) {
    val actions = listOf(
        Triple("New Bid", Icons.Default.Add, Color(0xFF1897FF)),
        Triple("Analytics", Icons.Default.Analytics, Color(0xFF9C27B0)),
        Triple("History", Icons.Default.History, Color(0xFFE91E63)),
        Triple("Settings", Icons.Default.Settings, Color(0xFF607D8B))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        actions.forEach { (label, icon, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .clickable {
                            when (label) {
                                "New Bid" -> onNewBidClick()
                                "History" -> onHistoryClick()
                                "Settings" -> { /* no-op for legacy grid */ }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

@Composable
fun RecentActivityList(recentFiles: List<com.example.bidcarftagent.presentation.home.RecentFile>) {
    // show up to 3 recent files; if none, show placeholders
    val items = if (recentFiles.isEmpty()) {
        listOf(
            com.example.bidcarftagent.presentation.home.RecentFile("Project_Requirements.pdf", System.currentTimeMillis()),
            com.example.bidcarftagent.presentation.home.RecentFile("Client_SRS.pdf", System.currentTimeMillis() - 3600_000),
            com.example.bidcarftagent.presentation.home.RecentFile("Sample_Profile.pdf", System.currentTimeMillis() - 86_400_000)
        )
    } else {
        recentFiles.take(3)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            RecentFileCard(item)
        }
    }
}

@Composable
fun RecentFileCard(item: com.example.bidcarftagent.presentation.home.RecentFile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = relativeTime(item.ts),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun PromotionalLottieCard() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val resId = ctx.resources.getIdentifier("pdf", "raw", ctx.packageName).takeIf { it != 0 } ?: R.raw.upload_cloud
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Do your Bid in Minutes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0B355E)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Be Fast, Grab fast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

data class ActivityItem(
    val title: String,
    val status: String,
    val time: String,
    val statusColor: Color
)

@Composable
fun ActivityCard(item: ActivityItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                )
            }
            
            // Status Pill
            Box(
                modifier = Modifier
                    .background(item.statusColor.copy(alpha = 0.1f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.status,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = item.statusColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}


fun relativeTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    if (diff < 0) return "just now"
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    if (seconds < 60) return "${seconds}s ago"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    if (minutes < 60) return "${minutes}m ago"
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    if (hours < 24) return "${hours}h ago"
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    if (days < 7) return "${days}d ago"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(ts))
}


