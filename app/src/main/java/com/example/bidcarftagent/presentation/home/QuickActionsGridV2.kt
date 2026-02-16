package com.example.bidcarftagent.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun QuickActionsGridV2(
    onNewBidClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
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
                                "Settings" -> onSettingsClick()
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

                    // Animated "Coming Soon" bubble for Analytics icon
                    if (label == "Analytics") {
                        val transition = rememberInfiniteTransition()
                        val scale by transition.animateFloat(
                            initialValue = 0.36f,
                            targetValue = 0.54f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 800)
                            )
                        )

                        Box(
                            modifier = Modifier
                                .align(TopStart)
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
}

