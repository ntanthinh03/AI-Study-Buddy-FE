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
import com.thinh.aistudybuddy.data.local.SessionStore
import com.thinh.aistudybuddy.data.local.TokenDataStore
import com.thinh.aistudybuddy.services.network.RetrofitClient
import com.thinh.aistudybuddy.ui.components.AuthTextField
import com.thinh.aistudybuddy.ui.components.BuddyLogo
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loginViewModel: LoginViewModel = viewModel()
    var rememberLogin by remember { mutableStateOf(false) }


    val infiniteTransition = rememberInfiniteTransition(label = "login_ambient")
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
                    text = "Welcome Back",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Log in to synchronize your study roadmap",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard()
                        .padding(24.dp)
                ) {
                    AuthTextField(
                        value = loginViewModel.email,
                        onValueChange = { loginViewModel.email = it },
                        label = "University Email",
                        placeholder = "your.name@university.edu",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    AuthTextField(
                        value = loginViewModel.password,
                        onValueChange = { loginViewModel.password = it },
                        label = "Password",
                        placeholder = "Enter your password",
                        isPassword = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberLogin,
                            onCheckedChange = { rememberLogin = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryNeonTeal,
                                uncheckedColor = Color.White.copy(alpha = 0.3f),
                                checkmarkColor = DeepSpaceBackground
                            )
                        )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    loginViewModel.errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = RoseWarning,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))


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
                            .clickable(enabled = !loginViewModel.isLoading) {
                                loginViewModel.onLoginClick { displayName ->
                                    val token = RetrofitClient.authToken
                                    scope.launch {
                                        if (!token.isNullOrBlank()) {
                                            SessionStore.saveSession(context, token, rememberLogin, displayName)
                                            TokenDataStore.saveToken(context, token)
                                        } else {
                                            SessionStore.clearSession(context)
                                            TokenDataStore.clearToken(context)
                                        }

                                        Toast
                                            .makeText(context, "Login Successful!", Toast.LENGTH_SHORT)
                                            .show()
                                        onLoginSuccess(displayName)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (loginViewModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "L O G  I N",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryNeonTeal,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onForgotPasswordClick() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryTangerine,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onRegisterClick() }
                    )
                }
            }
        }
    }
}
