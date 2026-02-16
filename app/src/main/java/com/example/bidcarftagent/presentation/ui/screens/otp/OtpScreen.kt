package com.example.bidcarftagent.presentation.ui.screens.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.bidcarftagent.presentation.components.LoadingOverlay
import com.example.bidcarftagent.presentation.components.OTPInput
import com.example.bidcarftagent.presentation.components.PrimaryButton

@Composable
fun OtpScreen(
    phoneWithCode: String,
    vm: OtpViewModel,
    onVerified: () -> Unit = {}
) {
    // set phone once
    LaunchedEffect(phoneWithCode) { vm.setPhone(phoneWithCode) }

    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is OtpUiEvent.Verified -> onVerified()
                is OtpUiEvent.ShowError -> { /* show snackbar */ }
            }
        }
    }

    Card(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "OTP Verification", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Please verify your otp", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))

            OTPInput(digits = state.digits)

            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(text = "Verify", onClick = vm::verifyOtp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Resend code", style = MaterialTheme.typography.bodyMedium)
        }
    }

    LoadingOverlay(visible = state.isVerifying)
}

