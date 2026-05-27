package com.thinh.aistudybuddy.ui.pages

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.ui.components.AuthTextField
import com.thinh.aistudybuddy.ui.components.BuddyLogo
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.ForgotPasswordViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    onContinueToOtp: (email: String) -> Unit
) {
    val context = LocalContext.current
    val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }


    val infiniteTransition = rememberInfiniteTransition(label = "forgot_ambient")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_float"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = DeepSpaceBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryNeonTeal.copy(alpha = 0.12f * pulseScale), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.2f + floatOffset),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.15f * pulseScale), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.8f - floatOffset),
                        radius = size.width * 0.7f
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    BuddyLogo(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Forgot Password",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Don't worry, we'll send verification details to your email",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                item {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard()
                            .padding(24.dp)
                    ) {
                        forgotPasswordViewModel.errorMessage?.let {
                            Text(
                                text = it,
                                color = RoseWarning,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        AuthTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (emailError != null) emailError = null
                                forgotPasswordViewModel.clearError()
                            },
                            label = "University Email",
                            placeholder = "your.name@university.edu",
                            modifier = Modifier.fillMaxWidth()
                        )
                        emailError?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = it,
                                color = RoseWarning,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))


                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(PrimaryNeonTeal, TertiaryCosmicIndigo)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = !forgotPasswordViewModel.isLoading) {
                                    val trimmedEmail = email.trim()
                                    emailError = if (Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                        null
                                    } else {
                                        "Invalid email format."
                                    }

                                    if (emailError == null) {
                                        forgotPasswordViewModel.sendOtp(trimmedEmail) { response ->
                                            Toast
                                                .makeText(context, response.message, Toast.LENGTH_SHORT)
                                                .show()
                                            onContinueToOtp(trimmedEmail)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (forgotPasswordViewModel.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "S E N D  O T P",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "Remembered your password? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Back to Login",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryTangerine,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onBackToLogin() }
                        )
                    }
                }
            }
        }
    }
}


