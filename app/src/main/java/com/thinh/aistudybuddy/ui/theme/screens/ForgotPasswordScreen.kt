package com.thinh.aistudybuddy.ui.theme.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.ui.components.AuthTextField
import com.thinh.aistudybuddy.ui.components.BuddyLogo
import com.thinh.aistudybuddy.viewmodel.ForgotPasswordViewModel

private enum class ForgotStep { VERIFY_IDENTITY, RESET_PASSWORD }

private val RESET_PASSWORD_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$")

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()

    var step by remember { mutableStateOf(ForgotStep.VERIFY_IDENTITY) }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(140.dp)
                ) {
                    BuddyLogo(
                        modifier = Modifier
                            .size(260.dp)
                            .align(Alignment.BottomCenter)
                            .aspectRatio(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (step == ForgotStep.VERIFY_IDENTITY) "Forgot Password" else "Reset Password",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (step == ForgotStep.VERIFY_IDENTITY) {
                item {
                    forgotPasswordViewModel.errorMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFFE53935),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = "Enter your email and phone number to verify your identity.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
                        },
                        label = "University Email",
                        placeholder = "your.name@university.edu"
                    )
                    emailError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = it, color = Color(0xFFE53935), fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            if (phoneError != null) phoneError = null
                        },
                        label = "Phone Number",
                        placeholder = "e.g. 0912345678"
                    )
                    phoneError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = it, color = Color(0xFFE53935), fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val trimmedEmail = email.trim()
                            val normalizedPhone = phoneNumber.trim().replace(" ", "")
                            val digits = normalizedPhone.filter { it.isDigit() }

                            emailError = if (Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                null
                            } else {
                                "Invalid email format."
                            }

                            phoneError = if (digits.length in 9..15) {
                                null
                            } else {
                                "Invalid phone number."
                            }

                            if (emailError == null && phoneError == null) {
                                forgotPasswordViewModel.clearError()
                                step = ForgotStep.RESET_PASSWORD
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Text("Verify", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                item {
                    forgotPasswordViewModel.errorMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFFE53935),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = "Create a new password for your account.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            if (newPasswordError != null) newPasswordError = null
                        },
                        label = "New Password",
                        placeholder = "Enter new password",
                        isPassword = true
                    )
                    newPasswordError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = it, color = Color(0xFFE53935), fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmPasswordError != null) confirmPasswordError = null
                        },
                        label = "Re-confirm Password",
                        placeholder = "Re-enter new password",
                        isPassword = true
                    )
                    confirmPasswordError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = it, color = Color(0xFFE53935), fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
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
                                forgotPasswordViewModel.resetPassword(
                                    email = email.trim(),
                                    phoneNumber = phoneNumber.trim().replace(" ", ""),
                                    newPassword = newPassword
                                ) { message ->
                                    Toast.makeText(context, message.ifBlank { "Password reset successful. Please log in." }, Toast.LENGTH_SHORT).show()
                                    onBackToLogin()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        enabled = !forgotPasswordViewModel.isLoading
                    ) {
                        if (forgotPasswordViewModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Reset Password", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.Center) {
                        Text(text = "Remembered your password? ", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            text = "Back to Login",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onBackToLogin() }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}



