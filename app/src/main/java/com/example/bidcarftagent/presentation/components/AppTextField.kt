package com.example.bidcarftagent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bidcarftagent.presentation.ui.theme.Surface
import com.example.bidcarftagent.presentation.ui.theme.TextPrimary
import com.example.bidcarftagent.presentation.ui.theme.TextSecondary

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .background(Surface),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TextPrimary,
            unfocusedBorderColor = TextSecondary,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedPlaceholderColor = TextSecondary,
            unfocusedPlaceholderColor = TextSecondary,
            cursorColor = TextPrimary
        ),
        placeholder = { Text(placeholder) }
    )
}

