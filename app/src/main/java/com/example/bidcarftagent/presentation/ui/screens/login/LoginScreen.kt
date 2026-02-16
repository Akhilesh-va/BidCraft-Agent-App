package com.example.bidcarftagent.presentation.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.bidcarftagent.R
import com.example.bidcarftagent.presentation.components.LoadingOverlay
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    vm: LoginViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val packageName = context.packageName
    val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
    
    try {
        val clientIdResId = context.resources.getIdentifier("default_web_client_id", "string", packageName)
        if (clientIdResId != 0) {
            gsoBuilder.requestIdToken(context.getString(clientIdResId))
        } else {
            android.util.Log.w("LoginScreen", "default_web_client_id not found. Google Sign-In may fail.")
        }
    } catch (e: Exception) {
        android.util.Log.e("LoginScreen", "Error getting default_web_client_id", e)
    }

    val gso = gsoBuilder.build()
    val googleClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                vm.onGoogleSignInIdToken(idToken)
            } else {
                coroutineScope.launch { vm.showError("Google sign-in failed: ID Token is null") }
            }
        } catch (e: ApiException) {
            coroutineScope.launch { vm.showError("Google sign-in failed: ${e.localizedMessage}") }
        }
    }

    // Collect one-off events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is LoginUiEvent.ShowError -> { /* Handle error if needed */ }
                is LoginUiEvent.NavigateToHome -> onNavigateToHome()
                else -> {}
            }
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

        // 2. Gradient Overlay (Glassmorphism effect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // 3. Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // -- App Logo & Title --
            // Lottie Animation (Enhanced Size)
            val scannerResId = context.resources.getIdentifier("scanner", "raw", packageName)
            val scannerComposition by rememberLottieComposition(
                if (scannerResId != 0) LottieCompositionSpec.RawRes(scannerResId)
                else LottieCompositionSpec.RawRes(R.raw.upload_cloud)
            )
            
            Box(
                modifier = Modifier
                    .size(240.dp) // Increased size for impact
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = scannerComposition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "BidCraft Agent",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Intelligent Project Bidding",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.Black.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // -- Social Login Buttons --
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SocialLoginButton(
                    text = "Continue with Google",
                    iconResId = R.drawable.ic_google_logo,
                    backgroundColor = Color.White,
                    contentColor = Color.Black,
                    hasBorder = true,
                    onClick = {
                        try {
                            googleClient.signOut().addOnCompleteListener {
                                launcher.launch(googleClient.signInIntent)
                            }
                        } catch (e: Exception) {
                            launcher.launch(googleClient.signInIntent)
                        }
                    }
                )

                SocialLoginButton(
                    text = "Continue with Facebook",
                    iconResId = R.drawable.ic_facebook_logo,
                    backgroundColor = Color(0xFF1877F2),
                    contentColor = Color.White,
                    onClick = vm::onFacebookLoginClick
                )

                SocialLoginButton(
                    text = "Continue with Twitter",
                    iconResId = R.drawable.ic_twitter_x_logo,
                    backgroundColor = Color.Black,
                    contentColor = Color.White,
                    onClick = vm::onTwitterLoginClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "By continuing, you agree to our Terms & Privacy Policy",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.5f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    if (state.isLoading) {
        LoadingOverlay(visible = true)
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    iconResId: Int = 0,
    backgroundColor: Color,
    contentColor: Color,
    hasBorder: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        border = if (hasBorder) BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)) else null,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        if (iconResId != 0) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                tint = if (text.contains("Google")) Color.Unspecified else contentColor
            )
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
