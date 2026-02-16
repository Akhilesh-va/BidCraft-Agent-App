package com.example.bidcarftagent.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bidcarftagent.presentation.ui.theme.Primary
import com.example.bidcarftagent.presentation.ui.theme.Surface
import com.example.bidcarftagent.presentation.ui.theme.TextPrimary

@Composable
fun OTPInput(
    digits: List<String>,
    modifier: Modifier = Modifier
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        digits.forEach { d ->
            Box(
                modifier = modifier
                    .size(48.dp)
                    .border(1.dp, Primary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = d,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

