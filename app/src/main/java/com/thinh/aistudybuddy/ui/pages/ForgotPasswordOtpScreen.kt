package com.thinh.aistudybuddy.ui.pages

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thinh.aistudybuddy.ui.components.BuddyLogo
import com.thinh.aistudybuddy.ui.theme.*
import com.thinh.aistudybuddy.viewmodel.ForgotPasswordViewModel
import kotlinx.coroutines.delay

private const val RESEND_OTP_COOLDOWN_SECONDS = 30
private val OTP_SHAKE_INTENSITY = OtpShakeIntensity.MEDIUM

private enum class OtpShakeIntensity {
    SOFT,
    MEDIUM
}

private fun otpShakePattern(intensity: OtpShakeIntensity): List<Float> {
    return when (intensity) {
        OtpShakeIntensity.SOFT -> listOf(0f, -9f, 9f, -6f, 6f, -3f, 3f, 0f)
        OtpShakeIntensity.MEDIUM -> listOf(0f, -14f, 14f, -10f, 10f, -6f, 6f, 0f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordOtpScreen(
    email: String,
    onBack: () -> Unit,
    onOtpVerified: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ForgotPasswordViewModel = viewModel()

    var otp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var resendCooldownSeconds by remember { mutableStateOf(0) }
    val otpFocusRequester = remember { FocusRequester() }

    fun submitVerifyOtp() {
        otpError = if (otp.length == 6) null else "OTP must be exactly 6 digits."
        if (otpError == null) {
            viewModel.verifyOtp(email = email, otp = otp) { message ->
                Toast.makeText(context, message.ifBlank { "OTP verified successfully." }, Toast.LENGTH_SHORT).show()
                onOtpVerified()
            }
        }
    }

    LaunchedEffect(Unit) {
        otpFocusRequester.requestFocus()
    }

    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            delay(1000)
            resendCooldownSeconds -= 1
        }
    }

    // Ambient floating canvas glows
    val infiniteTransition = rememberInfiniteTransition(label = "otp_ambient")
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
            // Background ambient canvas circles
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
                        text = "Enter OTP",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "We have sent a 6-digit verification code to $email.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                item {
                    // Glass card container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard()
                            .padding(24.dp)
                    ) {
                        viewModel.errorMessage?.let {
                            Text(
                                text = it,
                                color = RoseWarning,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Text(
                            text = "OTP Code",
                            color = PrimaryNeonTeal.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OtpCodeField(
                            value = otp,
                            onValueChange = {
                                otp = it
                                if (otpError != null) otpError = null
                                viewModel.clearError()
                            },
                            focusRequester = otpFocusRequester,
                            shakeTrigger = 0
                        )

                        otpError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = it,
                                color = RoseWarning,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Gradient Submit Button
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
                                .clickable(enabled = otp.length == 6 && !viewModel.isLoading) {
                                    submitVerifyOtp()
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
                                    text = "V E R I F Y  O T P",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Resend Button styled beautifully
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(
                                    color = if (resendCooldownSeconds > 0) Color.Transparent else SurfaceContainerHigh.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (resendCooldownSeconds > 0) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = !viewModel.isLoading && resendCooldownSeconds == 0) {
                                    viewModel.sendOtp(email) { response ->
                                        resendCooldownSeconds = RESEND_OTP_COOLDOWN_SECONDS
                                        Toast
                                            .makeText(context, response.message, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (resendCooldownSeconds > 0) {
                                    "Resend OTP (${resendCooldownSeconds}s)"
                                } else {
                                    "Resend OTP"
                                },
                                color = if (resendCooldownSeconds > 0) Color.White.copy(alpha = 0.4f) else SecondaryTangerine,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
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

@Composable
private fun OtpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    shakeTrigger: Int,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val currentIndex = value.length.coerceAtMost(5)
    val shakeOffset = remember { Animatable(0f) }
    val caretTransition = rememberInfiniteTransition(label = "otp-caret")
    val caretAlpha by caretTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "otp-caret-alpha"
    )

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger <= 0) return@LaunchedEffect
        val pattern = otpShakePattern(OTP_SHAKE_INTENSITY)
        pattern.forEach { target ->
            shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 38))
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = { updated ->
                onValueChange(updated.filter(Char::isDigit).take(6))
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 1.sp, color = Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeOffset.value },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { index ->
                val digit = value.getOrNull(index)?.toString().orEmpty()
                val isActive = isFocused && value.length < 6 && index == currentIndex
                val borderColor = if (isActive) PrimaryNeonTeal else Color.White.copy(alpha = 0.1f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .glassCard(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = SurfaceCardContainer.copy(alpha = if (isActive) 0.8f else 0.4f),
                            borderColor = borderColor
                        )
                        .clickable { focusRequester.requestFocus() },
                    contentAlignment = Alignment.Center
                ) {
                    if (digit.isNotEmpty()) {
                        Text(
                            text = digit,
                            color = if (isActive) PrimaryNeonTeal else Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isActive) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(22.dp)
                                .background(PrimaryNeonTeal.copy(alpha = caretAlpha), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

