package com.thinh.aistudybuddy.ui.pages

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val RESET_PASSWORD_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordResetScreen(
    email: String,
    onBack: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ForgotPasswordViewModel = viewModel()

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Floating cyber orbs animation
    val infiniteTransition = rememberInfiniteTransition(label = "reset_ambient")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_float"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            // Neon glowing ambient background
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BuddyLogo(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Set a new secure password for $email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Premium Glass Card container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard()
                        .padding(24.dp)
                ) {
                    AuthTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            if (newPasswordError != null) newPasswordError = null
                            viewModel.clearError()
                        },
                        label = "New Password",
                        placeholder = "Enter new password",
                        isPassword = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    newPasswordError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = it,
                            color = RoseWarning,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmPasswordError != null) confirmPasswordError = null
                            viewModel.clearError()
                        },
                        label = "Re-confirm Password",
                        placeholder = "Re-enter new password",
                        isPassword = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    confirmPasswordError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = it,
                            color = RoseWarning,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    viewModel.errorMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = RoseWarning,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Neo-gradient interactive submit button
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
                            .clickable(enabled = !viewModel.isLoading) {
                                newPasswordError = if (RESET_PASSWORD_REGEX.matches(newPassword)) {
                                    null
                                } else {
                                    "Password must be 8-16 characters and include uppercase, lowercase, a number, and a special character."
                                }
                                confirmPasswordError = if (confirmPassword == newPassword) {
                                    null
                                } else {
                                    "Re-confirm password does not match."
                                }

                                if (newPasswordError == null && confirmPasswordError == null) {
                                    viewModel.resetPassword(
                                        email = email,
                                        newPassword = newPassword
                                    ) { message ->
                                        Toast.makeText(
                                            context,
                                            message.ifBlank { "Password reset successful. Please log in." },
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onBackToLogin()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "RESET PASSWORD",
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
                    verticalAlignment = Alignment.CenterVertically
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


