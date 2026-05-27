package com.thinh.aistudybuddy.ui.pages

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.ui.components.AuthTextField
import com.thinh.aistudybuddy.ui.components.BuddyLogo
import com.thinh.aistudybuddy.viewmodel.ChangePasswordViewModel
import com.thinh.aistudybuddy.ui.theme.*

private val CHANGE_PASSWORD_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ChangePasswordViewModel = viewModel()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var oldPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    if (viewModel.sessionExpired) {
        viewModel.consumeSessionExpired()
        onSessionExpired()
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBackground)) {

        val infiniteTransition = rememberInfiniteTransition(label = "change_password_ambient")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_pulse"
        )
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = -30f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_float"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryNeonTeal.copy(alpha = 0.08f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.2f + floatOffset),
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.1f * pulseScale), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f - floatOffset),
                    radius = size.width * 0.6f
                )
            )
        }

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
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BuddyLogo(
                            modifier = Modifier
                                .size(240.dp)
                                .aspectRatio(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Change Credentials",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Typography.headlineMedium.fontFamily
                    )
                    Text(
                        text = "Update your neural access passwords",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    viewModel.errorMessage?.let {
                        Surface(
                            color = RoseWarning.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoseWarning.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = it,
                                color = RoseWarning,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(20.dp), backgroundColor = SurfaceCardContainer.copy(alpha = 0.4f))
                            .cyberBorder(shape = RoundedCornerShape(20.dp), borderWidth = 1.dp, startColor = PrimaryNeonTeal.copy(alpha = 0.2f), endColor = TertiaryCosmicIndigo.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            AuthTextField(
                                value = oldPassword,
                                onValueChange = {
                                    oldPassword = it
                                    if (oldPasswordError != null) oldPasswordError = null
                                    viewModel.clearMessages()
                                },
                                label = "Old Password",
                                placeholder = "Enter current old password",
                                isPassword = true
                            )
                            oldPasswordError?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = it, color = RoseWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            AuthTextField(
                                value = newPassword,
                                onValueChange = {
                                    newPassword = it
                                    if (newPasswordError != null) newPasswordError = null
                                    viewModel.clearMessages()
                                },
                                label = "New Password",
                                placeholder = "Enter new cyber credentials",
                                isPassword = true
                            )
                            newPasswordError?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = it, color = RoseWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            AuthTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    if (confirmPasswordError != null) confirmPasswordError = null
                                    viewModel.clearMessages()
                                },
                                label = "Re-confirm New Password",
                                placeholder = "Re-enter new credentials",
                                isPassword = true
                            )
                            confirmPasswordError?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = it, color = RoseWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    oldPasswordError = if (oldPassword.isBlank()) {
                                        "Please enter your old password."
                                    } else null

                                    newPasswordError = when {
                                        !CHANGE_PASSWORD_REGEX.matches(newPassword) -> {
                                            "Password must be 8-16 characters and include uppercase, lowercase, a number, and a special character."
                                        }
                                        newPassword == oldPassword -> {
                                            "New password must be different from old password."
                                        }
                                        else -> null
                                    }

                                    confirmPasswordError = if (confirmPassword == newPassword) {
                                        null
                                    } else {
                                        "Re-confirm password does not match."
                                    }

                                    if (oldPasswordError == null && newPasswordError == null && confirmPasswordError == null) {
                                        viewModel.changePassword(oldPassword, newPassword) {
                                            Toast.makeText(context, viewModel.successMessage ?: "Password changed successfully.", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonTeal, contentColor = Color.Black),
                                enabled = !viewModel.isLoading
                            ) {
                                if (viewModel.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Verify & Save Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Return back to ", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Scholar Profile",
                            color = PrimaryNeonTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onBack() }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
