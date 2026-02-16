package com.example.bidcarftagent.presentation.ui.screens.dashboard

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.bidcarftagent.R
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bidcarftagent.presentation.dashboard.DashboardViewModel
import java.io.FileOutputStream
import java.io.File

@Composable
fun DashboardScreen(onNavigateToHome: () -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    val isLoadingState by viewModel.isLoading.collectAsState()
    val isUploadSuccessState by viewModel.isUploadSuccess.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var btnAnimating by remember { mutableStateOf(false) }
    var animIndex by remember { mutableStateOf(0) }
    val animTexts = listOf("Analyzing", "Extracting", "Processing company profile")
    var uploadStart by remember { mutableStateOf(0L) }
    var targetDelayMs by remember { mutableStateOf(5000L) }

    // Animate button text while animating
    androidx.compose.runtime.LaunchedEffect(btnAnimating) {
        if (btnAnimating) {
            while (btnAnimating) {
                animIndex = (animIndex + 1) % animTexts.size
                kotlinx.coroutines.delay(1200)
            }
        }
    }

    // Handle navigation on success after randomized minimum duration (3-5s)
    androidx.compose.runtime.LaunchedEffect(isUploadSuccessState) {
        if (isUploadSuccessState && btnAnimating) {
            val elapsed = System.currentTimeMillis() - uploadStart
            if (elapsed < targetDelayMs) kotlinx.coroutines.delay(targetDelayMs - elapsed)
            btnAnimating = false
            android.widget.Toast.makeText(context, "Company profile uploaded successfully", android.widget.Toast.LENGTH_SHORT).show()
            onNavigateToHome()
        } else if (!isLoadingState && !isUploadSuccessState && btnAnimating) {
            // upload failed
            btnAnimating = false
            android.widget.Toast.makeText(context, "Upload failed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            Log.d("DashboardScreen", "File Selected: $uri")
            
            // Resolve file name
            var name = "Selected Document"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
            selectedFileName = name
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image (Consistent with Login)
        Image(
            painter = painterResource(id = R.drawable.login_bg_light),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Hero Section text
             Text(
                text = "Company Profile",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Upload your documentation to get started",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Content Card (Glassmorphism + Modern styling)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.75f) 
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(28.dp)),
                 elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat modern look
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        
                        Spacer(modifier = Modifier.height(16.dp))

                         Text(
                            text = "Optimize Your Experience",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "A detailed company profile helps our AI generate accurate solutions and realistic pricing.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Black.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Information List (Professional Bullet Points)
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ideally includes:",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                             modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        val points = listOf(
                            "Company overview & positioning",
                            "Services (Web, Mobile, AI, Cloud)",
                            "Target industries & structure",
                            "Pricing approach & case studies"
                        )
                        
                        points.forEach { point ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50), // Success Green for checkmarks
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = point,
                                    color = Color.Black.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Upload Area (Dashed Border Drop Zone Look)
                    if (selectedFileUri == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clickable { 
                                    launcher.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) // Launch file picker
                                }
                                .background(Color.Transparent, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                                drawRoundRect(color = Color(0xFF1897FF), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f, 32f), style = stroke)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF1897FF))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tap to Upload Document",
                                    color = Color(0xFF1897FF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // File Selected State
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF1897FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1897FF).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                                    .clickable {
                                        launcher.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF1897FF)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = selectedFileName ?: "Selected Document",
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
                            
                            Button(
                                onClick = {
                                    scope.launch {
                                        // Convert Uri to temp file and upload via ViewModel
                                        val uri = selectedFileUri
                                        if (uri != null) {
                                            val file = try {
                                                val input = context.contentResolver.openInputStream(uri)
                                                val tmp = File.createTempFile("upload", ".pdf", context.cacheDir)
                                                FileOutputStream(tmp).use { out ->
                                                    input?.copyTo(out)
                                                }
                                                tmp
                                            } catch (e: Exception) {
                                                Log.w("DashboardScreen", "Failed to copy file", e)
                                                null
                                            }
                                            if (file != null) {
                                                // start button animation and choose a random delay between 3-5s
                                                uploadStart = System.currentTimeMillis()
                                                targetDelayMs = 5000L
                                                btnAnimating = true
                                                animIndex = 0
                                                viewModel.uploadProfileFile(file.absolutePath)
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoadingState && !btnAnimating,
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
                                if (btnAnimating) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF1897FF),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = animTexts[animIndex] + "...",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1897FF)
                                            )
                                        )
                                    }
                                } else if (isLoadingState) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF1897FF),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Continue",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
             // Footer Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(Color.White.copy(alpha=0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Better documentation leads to more accurate proposals.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.7f))
                )
            }
        }
    }
}
