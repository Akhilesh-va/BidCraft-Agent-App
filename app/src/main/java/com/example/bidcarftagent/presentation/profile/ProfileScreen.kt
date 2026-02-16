package com.example.bidcarftagent.presentation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.bidcarftagent.R

private val BluePrimary = Color(0xFF1897FF)
private val GlassWhite = Color.White.copy(alpha = 0.75f)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background Image
        Image(
            painter = painterResource(id = R.drawable.login_bg_light),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Gradient Overlay to ensure readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // 3. Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .border(3.dp, Color.White, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    if (state.photoUrl != null) {
                        AsyncImage(
                            model = state.photoUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            tint = Color.Gray.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name & Email
                Text(
                    text = state.displayName.ifBlank { "User" },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = state.email.ifBlank { "No email provided" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- Sections ---

                // Company Info
                ProfileSection(title = "Company Details", icon = Icons.Default.Business) {
                    InfoRow(label = "Company", value = state.companyName.ifBlank { "N/A" })
                    if (state.companySize.isNotBlank()) {
                        InfoRow(label = "Size", value = state.companySize)
                    }
                    if (state.industries.isNotEmpty()) {
                        InfoRow(label = "Industries", value = state.industries.joinToString(", "))
                    }
                    if (!state.contactEmail.isNullOrBlank()) {
                        InfoRow(label = "Contact", value = state.contactEmail)
                    }
                }

                // Services & Tech
                if (state.services.isNotEmpty() || state.techFrontend.isNotEmpty() || state.techBackend.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    ProfileSection(title = "Expertise & Tech", icon = Icons.Default.Code) {
                        if (state.services.isNotEmpty()) {
                            Text(
                                text = "Services",
                                style = MaterialTheme.typography.labelLarge.copy(color = BluePrimary, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
                            )
                            state.services.forEach {
                                Text(text = "• $it", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        // Tech Stack Summary
                        val techStack = listOf(
                            "Frontend" to state.techFrontend,
                            "Backend" to state.techBackend,
                            "Database" to state.techDatabase,
                            "Cloud" to state.techCloud
                        ).filter { it.second.isNotEmpty() }

                        if (techStack.isNotEmpty()) {
                            techStack.forEach { (category, items) ->
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge.copy(color = BluePrimary, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = items.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Delivery
                Spacer(modifier = Modifier.height(20.dp))
                ProfileSection(title = "Delivery & Pricing", icon = Icons.Default.WorkHistory) {
                     if (state.deliveryModels.isNotEmpty()) {
                         InfoRow(label = "Models", value = state.deliveryModels.joinToString(", "))
                     }
                     state.typicalDurationMonths?.let { 
                         InfoRow(label = "Avg Duration", value = "$it months")
                     }
                     if (state.pricingCurrency.isNotBlank()) {
                         Spacer(modifier = Modifier.height(8.dp))
                         Text(
                             text = "Pricing Reference (${state.pricingCurrency})",
                             style = MaterialTheme.typography.labelLarge.copy(color = BluePrimary, fontWeight = FontWeight.Bold)
                         )
                         state.pricingSample.entries.take(4).forEach { (k, v) ->
                             Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                 Text(text = k.replace('_', ' ').capitalizeWords(), style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                 Text(text = v.toString(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                             }
                         }
                     }
                }

                // Security & Case Studies
                if (state.securityPractices.isNotEmpty() || state.caseStudies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    ProfileSection(title = "Trust & Experience", icon = Icons.Default.Security) {
                        if (state.securityPractices.isNotEmpty()) {
                            Text(
                                text = "Security Practices",
                                style = MaterialTheme.typography.labelLarge.copy(color = BluePrimary, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            state.securityPractices.take(3).forEach {
                                Text(text = "✓ $it", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        if (state.caseStudies.isNotEmpty()) {
                            Text(
                                text = "Relevant Case Studies",
                                style = MaterialTheme.typography.labelLarge.copy(color = BluePrimary, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            state.caseStudies.take(3).forEach { (industry, solution) ->
                                Text(text = "• $industry: $solution", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary))
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = GlassWhite
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            ),
            modifier = Modifier.weight(2f)
        )
    }
}

private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
