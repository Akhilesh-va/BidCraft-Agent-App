package com.example.bidcarftagent.presentation.ui.screens.home

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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bidcarftagent.R
import java.util.*
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun HomeScreen() {
    val viewModel: com.example.bidcarftagent.presentation.home.HomeViewModel = hiltViewModel()
    val context = LocalContext.current
    val pickPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it) ?: run {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            viewModel.uploadSrsFile(file.absolutePath)
            Toast.makeText(context, "Uploading ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }
    val pickProfileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it) ?: run {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            viewModel.uploadProfileFile(file.absolutePath)
            Toast.makeText(context, "Uploading profile ${file.name}", Toast.LENGTH_SHORT).show()
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
            HomeHeader()
            
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
            QuickActionsGrid(
                onUploadSrs = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                onUploadProfile = { pickProfileLauncher.launch(arrayOf("application/pdf")) }
            )
            
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
                    modifier = Modifier.clickable { }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            RecentActivityList()
        }
    }
}

@Composable
fun HomeHeader() {
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
                text = "Akhilesh M.",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            )
        }
        
        // Notification Icon with Badge
        Box {
            IconButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black
                )
            }
            // Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(10.dp)
                    .background(Color(0xFFFF3D00), CircleShape)
            )
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
fun QuickActionsGrid(onUploadSrs: () -> Unit = {}, onUploadProfile: () -> Unit = {}) {
    val actions = listOf(
        Triple("New Bid", Icons.Default.Add, Color(0xFF1897FF)),
        Triple("Analytics", Icons.Default.Analytics, Color(0xFF9C27B0)),
        Triple("History", Icons.Default.History, Color(0xFFE91E63)),
        Triple("Profile", Icons.Default.Person, Color(0xFF4CAF50))
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
                        .clickable { },
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
        // Additional quick action buttons for upload flows
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF2196F3).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .clickable { onUploadSrs() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Upload SRS", tint = Color(0xFF2196F3))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Upload SRS", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .clickable { onUploadProfile() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.AccountBox, contentDescription = "Upload Profile", tint = Color(0xFF4CAF50))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Upload Profile", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
        }
    }
}

fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val file = File.createTempFile("upload", ".pdf", context.cacheDir)
        FileOutputStream(file).use { out ->
            input.copyTo(out)
        }
        file
    } catch (e: Exception) {
        null
    }
}

@Composable
fun RecentActivityList() {
    val items = listOf(
        ActivityItem("Mobile App Development", "Pending Review", "2 hrs ago", Color(0xFFFFA000)),
        ActivityItem("E-commerce Website", "Won", "5 hrs ago", Color(0xFF4CAF50)),
        ActivityItem("AI Integration Project", "Lost", "1 day ago", Color(0xFFF44336))
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            ActivityCard(item)
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
