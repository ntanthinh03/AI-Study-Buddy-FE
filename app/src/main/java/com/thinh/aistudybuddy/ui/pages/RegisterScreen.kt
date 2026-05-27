package com.thinh.aistudybuddy.ui.pages

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import com.thinh.aistudybuddy.viewmodel.RegisterViewModel

private val PASSWORD_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$")
private val PHONE_REGEX = Regex("^[+]?[0-9]{9,15}$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    onBackToChat: () -> Unit
) {
    val context = LocalContext.current
    val registerViewModel: RegisterViewModel = viewModel()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedMajor by remember { mutableStateOf("Computer Science") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val majors = listOf("Computer Science", "Business", "Medicine", "Engineering")


    val infiniteTransition = rememberInfiniteTransition(label = "register_ambient")
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
                    IconButton(onClick = onBackToChat) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
                        center = Offset(size.width * 0.15f, size.height * 0.25f + floatOffset),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(TertiaryCosmicIndigo.copy(alpha = 0.15f * pulseScale), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.75f - floatOffset),
                        radius = size.width * 0.7f
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    BuddyLogo(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(top = 8.dp)
                    )

                    Text(
                        text = "Start Your AI Journey",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Join Buddy App to supercharge your academic goals",
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
                        registerViewModel.errorMessage?.let {
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
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = "Full Name",
                            placeholder = "Enter your full name",
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

                        Spacer(modifier = Modifier.height(16.dp))

                        AuthTextField(
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it
                                if (phoneError != null) phoneError = null
                            },
                            label = "Phone Number",
                            placeholder = "e.g. +84901234567",
                            modifier = Modifier.fillMaxWidth()
                        )
                        phoneError?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = it,
                                color = RoseWarning,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AuthTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (passwordError != null) passwordError = null
                            },
                            label = "Password",
                            placeholder = "Create a password",
                            isPassword = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        passwordError?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = it,
                                color = RoseWarning,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AuthTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (confirmPasswordError != null) confirmPasswordError = null
                            },
                            label = "Confirm Password",
                            placeholder = "Repeat password",
                            isPassword = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        confirmPasswordError?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = it,
                                color = RoseWarning,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "What are you studying?",
                            color = PrimaryNeonTeal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MajorItem(majors[0], selectedMajor == majors[0], Modifier.weight(1f)) {
                                    selectedMajor = majors[0]
                                }
                                MajorItem(majors[1], selectedMajor == majors[1], Modifier.weight(1f)) {
                                    selectedMajor = majors[1]
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MajorItem(majors[2], selectedMajor == majors[2], Modifier.weight(1f)) {
                                    selectedMajor = majors[2]
                                }
                                MajorItem(majors[3], selectedMajor == majors[3], Modifier.weight(1f)) {
                                    selectedMajor = majors[3]
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))


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
                                .clickable(enabled = !registerViewModel.isLoading) {
                                    val trimmedEmail = email.trim()
                                    val normalizedPhone = phoneNumber.trim().replace(" ", "")
                                    emailError = if (Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                        null
                                    } else {
                                        "Invalid email format."
                                    }

                                    phoneError = if (PHONE_REGEX.matches(normalizedPhone)) {
                                        null
                                    } else {
                                        "Invalid phone number."
                                    }

                                    passwordError = if (PASSWORD_REGEX.matches(password)) {
                                        null
                                    } else {
                                        "Password must be 8-16 characters and include uppercase, lowercase, a number, and a special character."
                                    }

                                    confirmPasswordError = if (confirmPassword == password) {
                                        null
                                    } else {
                                        "Confirm password does not match."
                                    }

                                    if (emailError != null || phoneError != null || passwordError != null || confirmPasswordError != null) {
                                        return@clickable
                                    }

                                    registerViewModel.fullName = fullName.trim()
                                    registerViewModel.email = trimmedEmail
                                    registerViewModel.phoneNumber = normalizedPhone
                                    registerViewModel.password = password
                                    registerViewModel.major = selectedMajor
                                    registerViewModel.onRegisterClick {
                                        Toast
                                            .makeText(context, "Registration Successful!", Toast.LENGTH_SHORT)
                                            .show()
                                        onRegisterSuccess()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (registerViewModel.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "S I G N  U P",
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
                            text = "Already have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Log In",
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

@Composable
private fun MajorItem(title: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.cyberBorder(shape = RoundedCornerShape(12.dp), borderWidth = 1.5.dp)
                } else {
                    Modifier.glassCard(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = SurfaceCardContainer.copy(alpha = 0.3f),
                        borderColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) PrimaryNeonTeal else Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

